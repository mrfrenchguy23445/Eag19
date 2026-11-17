#!/usr/bin/env node
// EaglerBridge: Eaglercraft v5 handshake (WS) <-> vanilla Minecraft TCP tunnel
//
//   client (Eagler 1.19.4) --ws--> bridge --tcp--> any offline-mode 1.19.4 server
//
// Protocol notes:
//   - WS side: each message is one Eagler handshake packet, or after login exactly
//     one uncompressed vanilla packet payload (packetId VarInt + body), no length prefix.
//   - TCP side: standard vanilla framing, VarInt length + payload, zlib if the
//     backend enabled compression.
const net = require('net');
const zlib = require('zlib');
const WebSocket = require('ws');
const crypto = require('crypto');

const PKT = {
  CLIENT_VERSION: 0x01,
  SERVER_VERSION: 0x02,
  VERSION_MISMATCH: 0x03,
  CLIENT_REQUEST_LOGIN: 0x04,
  SERVER_ALLOW_LOGIN: 0x05,
  SERVER_DENY_LOGIN: 0x06,
  CLIENT_PROFILE_DATA: 0x07,
  CLIENT_FINISH_LOGIN: 0x08,
  SERVER_FINISH_LOGIN: 0x09,
  SERVER_REDIRECT: 0x0A,
  SERVER_ERROR: 0xFF
};

const AUTH_NONE = 0x00;
const PROTO_V5 = 5;
const GAME_PROTO = 762; // 1.19.4
const MSG_ONLINE_MODE = 'Server requires Mojang login (offline-mode only)';
const MAX_FRAME = 8388608;

/** MOTD payload expected by the Eaglercraft server-list query. */
function motdDocument(name) {
  return JSON.stringify({
    type: 'motd',
    vers: 'EaglerBridge/1.0',
    brand: 'EaglerBridge',
    name,
    cracked: true,
    time: Date.now(),
    data: {
      motd: ['\u00a7aEaglerBridge 1.19.4', '\u00a77vanilla TCP tunnel'],
      icon: true,
      online: 1,
      max: 20,
      players: [name],
      brand: 'EaglerBridge',
      vers: '1.0',
      time: Date.now()
    }
  });
}

function makeIcon() {
  const out = Buffer.alloc(64 * 64 * 4);
  for (let y = 0; y < 64; ++y) {
    for (let x = 0; x < 64; ++x) {
      const i = (y * 64 + x) * 4;
      out[i] = 0x30 + Math.floor((x / 64) * 0x7f);
      out[i + 1] = 0x60 + Math.floor((y / 64) * 0x7f);
      out[i + 2] = 0xa0;
      out[i + 3] = 0xff;
    }
  }
  return out;
}

const QUERY_ICON = makeIcon();

function writeVarInt(out, v) {
  while ((v & 0xffffff80) !== 0) {
    out.push((v & 0x7f) | 0x80);
    v >>>= 7;
  }
  out.push(v & 0x7f);
}

function readVarInt(data, start) {
  let value = 0, shift = 0, pos = start;
  while (pos < data.length) {
    const b = data[pos++];
    value |= (b & 0x7f) << shift;
    if ((b & 0x80) === 0) return { value, pos };
    shift += 7;
    if (shift > 35) throw new Error('varint too long');
  }
  throw new Error('truncated varint');
}

function offlineUUID(name) {
  const hash = crypto.createHash('md5').update('OfflinePlayer:' + name).digest();
  hash[6] = (hash[6] & 0x0f) | 0x30; // version 3
  hash[8] = (hash[8] & 0x3f) | 0x80; // IETF variant
  return { msb: hash.readBigInt64BE(0), lsb: hash.readBigInt64BE(8) };
}

function bitCount(v) {
  let c = 0;
  while (v) { c += v & 1; v >>>= 1; }
  return c;
}

function frame(payload) {
  const out = [];
  writeVarInt(out, payload.length);
  for (let i = 0; i < payload.length; i++) out.push(payload[i]);
  return Buffer.from(out);
}

class EaglerBridge {
  constructor(opts = {}) {
    this.targetHost = opts.targetHost || '127.0.0.1';
    this.targetPort = opts.targetPort || 25565;
    this.listenPort = opts.listenPort || 8081;
    this.listenHost = opts.listenHost || '127.0.0.1';
    this.displayName = `${this.targetHost}:${this.targetPort}`;
    this.wss = new WebSocket.Server({ port: this.listenPort, host: this.listenHost });
    this.wss.on('listening', () => {
      console.log(`[bridge] ws://${this.listenHost}:${this.listenPort} -> tcp://${this.targetHost}:${this.targetPort}`);
    });
    this.wss.on('connection', (ws) => new Session(ws, this));
    this.wss.on('error', (e) => console.error('[bridge] server error', e));
  }
}

class Session {
  constructor(ws, bridge) {
    this.ws = ws;
    this.bridge = bridge;
    this.state = 'eagler_version';
    this.username = null;
    this.backend = null;
    this.backendBuf = Buffer.alloc(0);
    this.compression = -1;      // backend threshold
    this.backendLoginDone = false;
    this.pending = [];          // PLAY packets from backend, held until client is ready
    this.buffering = false;
    this.statusHandshake = null;
    this.statusQueue = [];
    this.statusConnected = false;

    ws.on('message', (msg, isBinary) => this.onClientMessage(msg, isBinary));
    ws.on('close', () => this.close('client closed'));
    ws.on('error', (e) => { console.log('[bridge] client ws error:', e.message); this.close('client error'); });
    ws.on('pong', () => {});
  }

  log(...a) { console.log(`[${this.username || 'anon'}]`, ...a); }

  close(reason) {
    if (this.closed) return;
    this.closed = true;
    if (this.backend) { try { this.backend.destroy(); } catch (e) {} this.backend = null; }
    try { this.ws.close(); } catch (e) {}
    this.log('session closed:', reason);
  }

  sendClient(buf) {
    if (this.ws.readyState === WebSocket.OPEN) this.ws.send(buf);
  }

  sendClientText(text) {
    if (this.ws.readyState === WebSocket.OPEN) this.ws.send(text);
  }

  onClientMessage(msg, isBinary) {
    if (this.closed) return;
    if (!isBinary) {
      const text = typeof msg === 'string' ? msg : Buffer.from(msg).toString('utf8');
      if (/^\s*accept\s*:/i.test(text)) {
        this.log('query:', text.trim());
        this.sendClientText(motdDocument(this.bridge.displayName));
        this.sendClient(QUERY_ICON);
        return;
      }
      this.log('ignoring text frame:', text.slice(0, 80));
      return;
    }
    const buf = Buffer.isBuffer(msg) ? msg : Buffer.from(msg);
    if (buf.length === 0) return;
    try {
      switch (this.state) {
        case 'eagler_version':
          if (buf[0] === 0x00) { this.startStatusPing(buf); return; }  // vanilla status handshake
          this.handleVersion(buf);
          break;
        case 'status':
          // vanilla status: status request, ping request -> relay raw
          if (this.statusConnected) this.backend.write(frame(buf));
          else this.statusQueue.push(buf);
          break;
        case 'eagler_login': this.handleLoginRequest(buf); break;
        case 'eagler_profile': this.handleProfile(buf); break;
        case 'play': this.handleClientPlay(buf); break;
        default: this.log('dropping message in state', this.state);
      }
    } catch (e) {
      console.error('[bridge] packet error:', e);
      this.close('protocol error');
    }
  }

  buildVersionMismatch() {
    const out = [PKT.VERSION_MISMATCH];
    const b16 = Buffer.alloc(2);
    b16.writeUInt16BE(1); out.push(b16[0], b16[1]);          // hs protocol count
    b16.writeUInt16BE(PROTO_V5); out.push(b16[0], b16[1]);    // hs v5
    b16.writeUInt16BE(2); out.push(b16[0], b16[1]);           // mc count
    b16.writeUInt16BE(GAME_PROTO); out.push(b16[0], b16[1]);  // min mc
    b16.writeUInt16BE(GAME_PROTO); out.push(b16[0], b16[1]);  // max mc
    const msg = Buffer.from('Unsupported Client Version', 'ascii');
    out.push(msg.length);
    for (const c of msg) out.push(c);
    return out;
  }

  // Some clients ping with a vanilla status handshake. Keep relaying that to
  // the backend, but note that this client's server list uses the Eagler text
  // query handled in onClientMessage() instead.
  startStatusPing(hs) {
    const proto = readVarInt(hs, 1);
    const hostLen = readVarInt(hs, proto.pos);
    const portAt = hostLen.pos + hostLen.value;
    const port = hs.readUInt16BE(portAt);
    const nextState = readVarInt(hs, portAt + 2);
    if (nextState.value !== 1) {
      this.log('unsupported first-packet next state', nextState.value);
      this.close('unsupported handshake');
      return;
    }
    this.log(`status ping for ${hs.slice(hostLen.pos, hostLen.value + hostLen.pos).toString()}:${port}`);
    this.state = 'status';
    this.statusHandshake = hs;
    this.statusQueue = [];
    this.statusConnected = false;
    this.connectBackend();
  }

  handleVersion(buf) {
    if (buf[0] !== PKT.CLIENT_VERSION) throw new Error('expected CLIENT_VERSION, got 0x' + buf[0].toString(16));
    // [0x01][0x02][ushort hsCnt][hs vers xN][ushort mcCnt][mc vers xN]
    // [brandLen][brand][verLen][version][auth bool][nameLen][name]
    if (buf[1] !== 2) throw new Error('expected Eagler v2+ marker, got ' + buf[1]);
    let pos = 2;
    const hsCount = buf.readUInt16BE(pos); pos += 2;
    if (hsCount === 0 || hsCount > 16) throw new Error('bad hs count ' + hsCount);
    const hsVers = [];
    for (let i = 0; i < hsCount; i++) { hsVers.push(buf.readUInt16BE(pos)); pos += 2; }
    const mcCount = buf.readUInt16BE(pos); pos += 2;
    if (mcCount === 0 || mcCount > 16) throw new Error('bad mc count ' + mcCount);
    const mcVers = [];
    for (let i = 0; i < mcCount; i++) { mcVers.push(buf.readUInt16BE(pos)); pos += 2; }
    if (!mcVers.includes(GAME_PROTO)) {
      this.sendClient(Buffer.from(this.buildVersionMismatch(hsVers)));
      this.log('client does not offer MC', GAME_PROTO, 'got', mcVers.join(','));
      this.close('version mismatch');
      return;
    }
    const brandLen = buf[pos++];
    const brand = buf.slice(pos, pos + brandLen).toString(); pos += brandLen;
    const verLen = buf[pos++];
    const version = buf.slice(pos, pos + verLen).toString(); pos += verLen;
    const hasPassword = buf[pos++] !== 0;
    const nameLen = buf[pos++];
    const username = buf.slice(pos, pos + nameLen).toString('ascii'); pos += nameLen;
    this.clientBrand = brand;
    this.clientVersion = version;
    // The real name always lives here. REQUEST_LOGIN's name slot is only a
    // nickname override and is 0-length when nickname selection is off.
    this.versionName = username;
    this.log(`client ${brand} ${version} hs=${hsVers.join('/')} mc=${mcVers.join(',')} pw=${hasPassword} name="${username}"`);

    const out = [];
    out.push(PKT.SERVER_VERSION);
    const b16 = Buffer.alloc(2);
    b16.writeUInt16BE(Math.max(...hsVers.filter(v => v >= 3 && v <= 5)) || PROTO_V5);
    out.push(b16[0], b16[1]);
    b16.writeUInt16BE(GAME_PROTO);
    out.push(b16[0], b16[1]);
    for (const s of [Buffer.from('EaglerBridge'), Buffer.from('1.0')]) {
      out.push(s.length & 0xff);
      for (const c of s) out.push(c);
    }
    out.push(AUTH_NONE); // auth method none
    out.push(0, 0);      // salt length (unsigned short)
    // v5 nickname selection. False means "accept the name the client sends in
    // CLIENT_VERSION"; the client then leaves REQUEST_LOGIN's name slot empty,
    // which is why handleLoginRequest falls back to this.versionName.
    const nicknameSelection = false;
    out.push(nicknameSelection ? 1 : 0);
    this.nicknameSelection = nicknameSelection;
    this.sendClient(Buffer.from(out));
    this.state = 'eagler_login';
  }

  handleLoginRequest(buf) {
    if (buf[0] !== PKT.CLIENT_REQUEST_LOGIN) throw new Error('expected REQUEST_LOGIN');
    let pos = 1;
    const nameLen = buf[pos++];
    const requestedName = buf.slice(pos, pos + nameLen).toString('ascii'); pos += nameLen;
    const srvLen = buf[pos++]; pos += srvLen;
    const pwLen = buf[pos++]; pos += pwLen;
    // v4+: cookie support
    const enableCookie = buf[pos++] !== 0;
    const cookieLen = buf[pos++];
    pos += enableCookie ? cookieLen : 0;
    // v5: standard caps (VarInt of mask, then one VarInt version per set bit), ext caps
    const caps = readVarInt(buf, pos); pos = caps.pos;
    for (let i = 0; i < bitCount(caps.value); i++) { const r = readVarInt(buf, pos); pos = r.pos; }
    const extCaps = buf[pos++];
    pos += extCaps * 17;

    // HandshakerInstance.handlePacketRequestLogin(): with nickname selection
    // disabled (we send false in SERVER_VERSION) the real name comes from
    // CLIENT_VERSION and REQUEST_LOGIN carries a 0-length nickname slot; with
    // selection enabled the REQUEST_LOGIN name wins.
    const username = this.nicknameSelection ? requestedName : (this.versionName || requestedName);
    if (!username) {
      this.log('rejecting login: client sent no name');
      this.sendDeny('This server needs a username. Pick one in the profile editor.');
      this.close('no username');
      return;
    }

    this.username = username;
    this.uuid = offlineUUID(username);
    this.log(`login requested for "${username}"`);

    this.state = 'eagler_profile';
    this.connectBackend();
  }

  // ---- backend (vanilla TCP) ------------------------------------------------

  connectBackend() {
    const b = net.createConnection({ host: this.bridge.targetHost, port: this.bridge.targetPort });
    this.backend = b;
    b.on('connect', () => {
      if (this.state === 'status') {
        this.log('backend connected (status), relaying');
        b.write(frame(this.statusHandshake));
        for (const p of this.statusQueue) b.write(frame(p));
        this.statusQueue.length = 0;
        this.statusConnected = true;
        return;
      }
      this.log('backend connected, sending handshake + login start');
      const hs = [];
      writeVarInt(hs, 0);                       // serverbound handshake
      writeVarInt(hs, GAME_PROTO);
      const addr = Buffer.from(this.bridge.targetHost);
      writeVarInt(hs, addr.length);
      for (const c of addr) hs.push(c);
      hs.push((this.bridge.targetPort >> 8) & 0xff, this.bridge.targetPort & 0xff);
      writeVarInt(hs, 2);                       // next state: login
      b.write(frame(Buffer.from(hs)));

      const ls = [];
      writeVarInt(ls, 0);                       // serverbound login start
      const nm = Buffer.from(this.username);
      writeVarInt(ls, nm.length);
      for (const c of nm) ls.push(c);
      ls.push(0);                               // Optional<UUID> present = false (761..763)
      b.write(frame(Buffer.from(ls)));
    });
    b.on('data', (chunk) => this.onBackendData(chunk));
    b.on('error', (e) => {
      this.log('backend error:', e.message);
      if (this.state !== 'status' && !this.backendLoginDone) this.sendDeny('Backend unreachable: ' + e.message);
      this.close('backend error');
    });
    b.on('close', () => { if (!this.closed) this.close('backend closed'); });
  }

  onBackendData(chunk) {
    this.backendBuf = Buffer.concat([this.backendBuf, chunk]);
    for (;;) {
      if (this.backendBuf.length < 1) return;
      let vi;
      try { vi = readVarInt(this.backendBuf, 0); } catch (e) { return; }
      if (vi.value < 0 || vi.value > MAX_FRAME) { this.log('bad backend frame length', vi.value); this.close('bad frame'); return; }
      if (vi.pos + vi.value > this.backendBuf.length) return;
      const payload = this.backendBuf.slice(vi.pos, vi.pos + vi.value);
      this.backendBuf = this.backendBuf.slice(vi.pos + vi.value);
      let packet;
      if (this.compression >= 0) {
        const dl = readVarInt(payload, 0);
        const body = payload.slice(dl.pos);
        if (dl.value === 0) {
          packet = body;
        } else {
          try {
            packet = zlib.inflateSync(body);
          } catch (e) {
            this.log('inflate failed:', e.message); this.close('bad compression'); return;
          }
          if (packet.length !== dl.value) { this.log('bad inflated size'); this.close('bad compression'); return; }
        }
      } else {
        packet = payload;
      }
      this.onBackendPacket(packet);
    }
  }

  onBackendPacket(packet) {
    if (this.state === 'status') {
      // vanilla status: status response (0x00) and pong (0x01) relay verbatim
      this.sendClient(packet);
      return;
    }
    if (!this.backendLoginDone) {
      const id = packet[0];
      switch (id) {
        case 0x00: { // disconnect
          let msg = 'Backend rejected login';
          try {
            const len = readVarInt(packet, 1);
            msg = packet.slice(len.pos, len.pos + len.value).toString().slice(0, 300);
          } catch (e) {}
          this.log('backend disconnect:', msg);
          this.sendDeny(msg);
          this.close('backend deny');
          return;
        }
        case 0x01: // encryption request
          this.log('backend wants online-mode auth');
          this.sendDeny(MSG_ONLINE_MODE);
          this.close('online mode');
          return;
        case 0x02: // login success -> backend login complete
          this.log('backend login success, sending ALLOW_LOGIN');
          this.backendLoginDone = true;
          this.buffering = true;
          this.sendAllow();
          return;
        case 0x03: { // set compression
          const th = readVarInt(packet, 1).value;
          this.compression = th;
          this.log('backend compression threshold:', th);
          return;
        }
        default:
          this.log('unexpected backend login packet 0x' + id.toString(16));
          this.close('unexpected login packet');
          return;
      }
    }
    // PLAY
    if (this.buffering) {
      this.pending.push(packet);
      if (this.pending.length > 512) { this.log('pending overflow'); this.close('overflow'); }
      return;
    }
    this.sendClient(packet);
  }

  // ---- Eagler handshake (client side) --------------------------------------

  handleProfile(buf) {
    const id = buf[0];
    if (id === PKT.CLIENT_PROFILE_DATA) {
      this.log('profile data received');
      return;
    }
    if (id === PKT.CLIENT_FINISH_LOGIN) {
      this.sendClient(Buffer.from([PKT.SERVER_FINISH_LOGIN]));
      this.state = 'play';
      this.buffering = false;
      this.log('login finished, entering play');
      for (const p of this.pending) this.sendClient(p);
      this.pending = [];
      return;
    }
    this.log('unexpected handshake packet 0x' + id.toString(16));
    this.close('unexpected packet');
  }

  sendAllow() {
    const out = [];
    out.push(PKT.SERVER_ALLOW_LOGIN);
    const ubytes = Buffer.from(this.username);
    out.push(ubytes.length & 0xff);
    for (const c of ubytes) out.push(c);
    const msb = Buffer.alloc(8), lsb = Buffer.alloc(8);
    msb.writeBigInt64BE(this.uuid.msb);
    lsb.writeBigInt64BE(this.uuid.lsb);
    for (let i = 0; i < 8; i++) out.push(msb[i]);
    for (let i = 0; i < 8; i++) out.push(lsb[i]);
    out.push(0); // standard capabilities mask (VarInt 0) + 0 versions
    out.push(0); // extended capability count
    this.sendClient(Buffer.from(out));
    this.log('ALLOW_LOGIN sent for', this.username);
  }

  sendDeny(message) {
    const msg = Buffer.from(String(message).slice(0, 255), 'utf8');
    const out = [PKT.SERVER_DENY_LOGIN];
    out.push(msg.length >> 8, msg.length & 0xff);
    for (const c of msg) out.push(c);
    this.sendClient(Buffer.from(out));
    this.log('DENY_LOGIN:', message);
  }

  // ---- play (both directions) ----------------------------------------------

  handleClientPlay(packet) {
    if (!this.backend || this.backend.destroyed) return;
    let payload = packet;
    if (this.compression >= 0) {
      const body = packet.length < this.compression
        ? Buffer.concat([Buffer.from([0]), packet])
        : Buffer.concat([
            (() => { const o = []; writeVarInt(o, packet.length); return Buffer.from(o); })(),
            zlib.deflateSync(packet)
          ]);
      payload = body;
    }
    this.backend.write(frame(payload));
  }
}

// ---- CLI -------------------------------------------------------------------
const args = process.argv.slice(2);
const opts = {};
for (let i = 0; i < args.length; i++) {
  switch (args[i]) {
    case '--target': opts.targetHost = args[++i]; break;
    case '--port': opts.targetPort = parseInt(args[++i], 10); break;
    case '--listen': opts.listenPort = parseInt(args[++i], 10); break;
    case '--bind': opts.listenHost = args[++i]; break;
    case '--help':
      console.log('Usage: node eagler-bridge.js [--bind ip] [--listen port] [--target host] [--port port]');
      process.exit(0);
  }
}
new EaglerBridge(opts);
