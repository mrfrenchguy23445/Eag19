const assert = require('assert');
const net = require('net');
const path = require('path');
const { spawn } = require('child_process');
const WebSocket = require('ws');

const REPO = path.resolve(__dirname, '..', '..');
const BRIDGE = path.join(__dirname, '..', 'eagler-bridge.js');
const USERNAME = 'BridgeTester';
const TOKEN = 'test-token';

function writeVarInt(out, v) {
  v >>>= 0;
  while ((v & ~0x7f) !== 0) {
    out.push((v & 0x7f) | 0x80);
    v >>>= 7;
  }
  out.push(v);
}

function readVarInt(buf, start) {
  let value = 0, shift = 0, pos = start;
  for (;;) {
    if (pos >= buf.length) throw new Error('truncated varint');
    const b = buf[pos++];
    value |= (b & 0x7f) << shift;
    if ((b & 0x80) === 0) return { value: value >>> 0, pos };
    shift += 7;
    if (shift > 35) throw new Error('varint too long');
  }
}

function readString(buf, start) {
  const len = readVarInt(buf, start);
  return {
    value: buf.slice(len.pos, len.pos + len.value).toString('utf8'),
    pos: len.pos + len.value
  };
}

function frame(payload) {
  const out = [];
  writeVarInt(out, payload.length);
  return Buffer.concat([Buffer.from(out), payload]);
}

function u16(n) {
  const b = Buffer.alloc(2);
  b.writeUInt16BE(n, 0);
  return b;
}

function loginSuccess() {
  const out = [];
  const uuid = '00000000-0000-3000-8000-000000000000';
  writeVarInt(out, 0x02);
  writeVarInt(out, uuid.length);
  out.push(...Buffer.from(uuid));
  writeVarInt(out, USERNAME.length);
  out.push(...Buffer.from(USERNAME));
  writeVarInt(out, 0); // properties
  return Buffer.from(out);
}

function clientVersionPacket() {
  const brand = Buffer.from('BetterEagler');
  const version = Buffer.from('1.19.4');
  const name = Buffer.from(USERNAME);
  const out = [0x01, 0x02];
  out.push(...u16(3));           // handshake protocol count
  for (const v of [5, 4, 3]) out.push(...u16(v));
  out.push(...u16(1), ...u16(762)); // one game protocol: 1.19.4
  out.push(brand.length, ...brand);
  out.push(version.length, ...version);
  out.push(0);
  out.push(name.length, ...name);
  return Buffer.from(out);
}

function requestLoginPacket() {
  // name/service/password/cookie are all empty; no extended capabilities.
  return Buffer.from([0x04, 0, 0, 0, 0, 0, 0, 0]);
}

function freePort() {
  return new Promise((resolve, reject) => {
    const s = net.createServer();
    s.once('error', reject);
    s.listen(0, '127.0.0.1', () => {
      const port = s.address().port;
      s.close(() => resolve(port));
    });
  });
}

function withTimeout(promise, ms, label) {
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => reject(new Error('timeout waiting for ' + label)), ms);
    promise.then(
      (v) => { clearTimeout(timer); resolve(v); },
      (e) => { clearTimeout(timer); reject(e); }
    );
  });
}

async function main() {
  const backendPort = await freePort();
  const listenPort = await freePort();

  let resolveHandshake, resolveLoginStart, resolveClientPlay;
  const handshakeSeen = new Promise((r) => { resolveHandshake = r; });
  const loginStartSeen = new Promise((r) => { resolveLoginStart = r; });
  const clientPlaySeen = new Promise((r) => { resolveClientPlay = r; });

  const backend = net.createServer((socket) => {
    let buf = Buffer.alloc(0);
    let handshakeDone = false;
    let loginDone = false;
    socket.setNoDelay(true);
    socket.on('error', () => {});
    socket.on('data', (chunk) => {
      buf = Buffer.concat([buf, chunk]);
      for (;;) {
        if (buf.length < 1) return;
        let len;
        try { len = readVarInt(buf, 0); } catch (e) { return; }
        if (buf.length < len.pos + len.value) return;
        const packet = Buffer.from(buf.slice(len.pos, len.pos + len.value));
        buf = buf.slice(len.pos + len.value);
        const id = readVarInt(packet, 0).value;

        if (id === 0 && !handshakeDone) {
          handshakeDone = true;
          const proto = readVarInt(packet, 1);
          const host = readString(packet, proto.pos);
          const port = packet.readUInt16BE(host.pos);
          const nextState = readVarInt(packet, host.pos + 2).value;
          resolveHandshake({ proto: proto.value, host: host.value, port, nextState });
        } else if (id === 0 && handshakeDone && !loginDone) {
          loginDone = true;
          const name = readString(packet, 1);
          resolveLoginStart({
            name: name.value,
            hasOptionalUuid: packet[name.pos] !== 0,
            length: packet.length
          });
          socket.write(frame(loginSuccess()));
        } else if (id === 0x03) {
          resolveClientPlay(packet);
          socket.write(frame(Buffer.from([0x1f, 0x01])));
        }
      }
    });
  });
  await new Promise((resolve) => backend.listen(backendPort, '127.0.0.1', resolve));

  const bridge = spawn(process.execPath, [
    BRIDGE,
    '--bind', '127.0.0.1',
    '--listen', String(listenPort),
    '--target', '127.0.0.1',
    '--port', '1',
    '--allow-any-target',
    '--token', TOKEN
  ], { cwd: REPO, stdio: ['ignore', 'pipe', 'pipe'] });

  let bridgeOutput = '';
  bridge.stdout.on('data', (d) => { bridgeOutput += d.toString(); });
  bridge.stderr.on('data', (d) => { bridgeOutput += d.toString(); });

  const ready = new Promise((resolve, reject) => {
    const check = () => {
      if (bridgeOutput.includes(`ws://127.0.0.1:${listenPort}`)) return resolve();
      if (bridge.exitCode !== null) return reject(new Error('bridge exited:\n' + bridgeOutput));
      setTimeout(check, 20);
    };
    check();
  });
  await withTimeout(ready, 5000, 'bridge startup');

  const targetQuery = `?host=127.0.0.1&port=${backendPort}&token=${TOKEN}`;
  const connect = (query = '') => new Promise((resolve, reject) => {
    const ws = new WebSocket(`ws://127.0.0.1:${listenPort}/${query}`);
    ws.once('open', () => resolve(ws));
    ws.once('error', reject);
  });

  const nextBinary = (ws) => new Promise((resolve, reject) => {
    const timer = setTimeout(() => reject(new Error('binary message timeout')), 5000);
    const handler = (data, isBinary) => {
      if (!isBinary) return;
      clearTimeout(timer);
      ws.off('message', handler);
      resolve(Buffer.isBuffer(data) ? data : Buffer.from(data));
    };
    ws.on('message', handler);
  });

  try {
    // A wrong token is rejected before any backend connection is opened.
    const bad = new WebSocket(`ws://127.0.0.1:${listenPort}/?host=127.0.0.1&port=${backendPort}&token=wrong`);
    bad.once('error', () => {});
    const badClose = new Promise((resolve) => bad.once('close', (code) => resolve(code)));
    assert.strictEqual(await withTimeout(badClose, 5000, 'token rejection'), 1008);

    // Server-list query is answered by the bridge, not the backend.
    const queryWs = await connect(targetQuery);
    const queryPromise = new Promise((resolve) => {
      const messages = [];
      queryWs.on('message', (data, isBinary) => {
        messages.push({ data, isBinary });
        if (messages.length === 2) resolve(messages);
      });
    });
    queryWs.send('Accept: MOTD');
    const query = await withTimeout(queryPromise, 5000, 'MOTD query');
    assert.strictEqual(query[0].isBinary, false, 'MOTD must be a text frame');
    const motd = JSON.parse(query[0].data.toString());
    assert.strictEqual(motd.type, 'motd');
    assert.strictEqual(motd.name, `127.0.0.1:${backendPort}`, 'MOTD should show the per-client target');
    assert.strictEqual(motd.data.icon, true);
    assert.strictEqual(query[1].isBinary, true, 'icon must be a binary frame');
    assert.strictEqual(query[1].data.length, 64 * 64 * 4);
    queryWs.close();

    // Real join: Eagler v5 in, vanilla protocol 762 out.
    const ws = await connect(targetQuery);
    const versionP = nextBinary(ws);
    ws.send(clientVersionPacket());
    const serverVersion = await versionP;
    assert.strictEqual(serverVersion[0], 0x02);
    assert.strictEqual(serverVersion[serverVersion.length - 1], 0, 'nickname selection must be false');

    const allowP = nextBinary(ws);
    ws.send(requestLoginPacket());
    const handshake = await withTimeout(handshakeSeen, 5000, 'backend handshake');
    assert.strictEqual(handshake.proto, 762);
    assert.strictEqual(handshake.host, '127.0.0.1');
    assert.strictEqual(handshake.port, backendPort, 'backend port must come from the client URL');
    assert.strictEqual(handshake.nextState, 2);
    const loginStart = await withTimeout(loginStartSeen, 5000, 'backend LoginStart');
    assert.strictEqual(loginStart.name, USERNAME);
    assert.strictEqual(loginStart.hasOptionalUuid, false);
    assert.strictEqual(loginStart.length, 1 + 1 + USERNAME.length + 1);
    assert.strictEqual((await allowP)[0], 0x05);

    const finishP = nextBinary(ws);
    ws.send(Buffer.from([0x07])); // CLIENT_PROFILE_DATA
    ws.send(Buffer.from([0x08])); // CLIENT_FINISH_LOGIN
    assert.strictEqual((await finishP)[0], 0x09);

    // Play packets are unframed on the WebSocket side and framed on TCP.
    const responseP = nextBinary(ws);
    const sentPlay = Buffer.from([0x03, 0x02, 0x68, 0x69]);
    ws.send(sentPlay);
    assert.deepStrictEqual(Buffer.from(await withTimeout(clientPlaySeen, 5000, 'client play packet')), sentPlay);
    assert.deepStrictEqual(await responseP, Buffer.from([0x1f, 0x01]));
    ws.close();

    console.log('PASS per-client target, token gate, MOTD, 762 handshake, LoginStart(false), and play relay');
  } catch (e) {
    if (bridgeOutput) console.error(bridgeOutput);
    throw e;
  } finally {
    bridge.kill('SIGTERM');
    backend.close();
  }
}

main().catch((e) => {
  console.error(e);
  process.exitCode = 1;
});
