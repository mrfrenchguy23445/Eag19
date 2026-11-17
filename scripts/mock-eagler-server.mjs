// Minimal Eaglercraft WebSocket server for exercising the client without a
// real Minecraft backend.
//
//   usage: node scripts/mock-eagler-server.mjs [port]        (default 8080)
//
// It implements two things a real EaglerXServer does on the same socket:
//
//   1. The server-list query. The client sends a text frame
//      "Accept: MOTD"; this replies with the MOTD JSON document and, when
//      "icon" is true, a 64x64 RGBA binary frame.
//
//   2. The v3/v5 login handshake (CLIENT_VERSION -> SERVER_VERSION ->
//      CLIENT_REQUEST_LOGIN -> SERVER_ALLOW_LOGIN -> profile data ->
//      CLIENT_FINISH_LOGIN -> SERVER_FINISH_LOGIN), after which the client
//      moves its Connection to PLAY.
//
// There is deliberately no vanilla play implementation. Once the handshake
// finishes the client sits on "Joining world..." (waiting for the play Login
// packet), which is enough to prove that the handshake and the PLAY transition
// worked. A real server is still required for actual gameplay.
//
// Point the client at "ws://localhost:8080" (include the scheme; the page is
// served over http, so plain ws is the right transport). The server list query
// accepts "localhost:8080" as well.

import http from "http";
import crypto from "crypto";

const PORT = Number(process.argv[2] || 8080);

const GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
const HANDSHAKE_PROTOCOL = 5;
const GAME_PROTOCOL = 762;
const BRAND = "EaglercraftXServer";
const PLUGIN_VERSION = "1.1.1";

const OP_CONT = 0x0;
const OP_TEXT = 0x1;
const OP_BINARY = 0x2;
const OP_CLOSE = 0x8;
const OP_PING = 0x9;
const OP_PONG = 0xa;

// ---------------------------------------------------------------------------
// WebSocket framing (server side, no extensions)
// ---------------------------------------------------------------------------

function encodeFrame(opcode, payload) {
	const len = payload.length;
	let header;
	if (len < 126) {
		header = Buffer.alloc(2);
		header[1] = len;
	} else if (len < 65536) {
		header = Buffer.alloc(4);
		header[1] = 126;
		header.writeUInt16BE(len, 2);
	} else {
		header = Buffer.alloc(10);
		header[1] = 127;
		header.writeBigUInt64BE(BigInt(len), 2);
	}
	header[0] = 0x80 | opcode;
	return Buffer.concat([header, payload]);
}

/** One parsed frame from the client (always masked). */
class FrameReader {
	constructor(onFrame) {
		this.buf = Buffer.alloc(0);
		this.onFrame = onFrame;
		this.fragments = [];
		this.fragmentOpcode = 0;
	}

	push(chunk) {
		this.buf = this.buf.length ? Buffer.concat([this.buf, chunk]) : chunk;
		for (;;) {
			if (this.buf.length < 2) return;
			const b0 = this.buf[0];
			const b1 = this.buf[1];
			const fin = (b0 & 0x80) !== 0;
			const opcode = b0 & 0x0f;
			const masked = (b1 & 0x80) !== 0;
			let len = b1 & 0x7f;
			let offset = 2;
			if (len === 126) {
				if (this.buf.length < 4) return;
				len = this.buf.readUInt16BE(2);
				offset = 4;
			} else if (len === 127) {
				if (this.buf.length < 10) return;
				len = Number(this.buf.readBigUInt64BE(2));
				offset = 10;
			}
			const maskLen = masked ? 4 : 0;
			if (this.buf.length < offset + maskLen + len) return;
			let payload = this.buf.subarray(offset + maskLen, offset + maskLen + len);
			if (masked) {
				const mask = this.buf.subarray(offset, offset + 4);
				const out = Buffer.allocUnsafe(len);
				for (let i = 0; i < len; i++) out[i] = payload[i] ^ mask[i & 3];
				payload = out;
			}
			this.buf = this.buf.subarray(offset + maskLen + len);

			if (opcode === OP_CONT) {
				this.fragments.push(Buffer.from(payload));
				if (fin) {
					const whole = Buffer.concat(this.fragments);
					const op = this.fragmentOpcode;
					this.fragments = [];
					this.fragmentOpcode = 0;
					this.onFrame(op, whole);
				}
				continue;
			}

			if (!fin) {
				this.fragmentOpcode = opcode;
				this.fragments = [Buffer.from(payload)];
				continue;
			}

			this.onFrame(opcode, Buffer.from(payload));
		}
	}
}

// ---------------------------------------------------------------------------
// Eaglercraft binary helpers (big-endian, like FriendlyByteBuf)
// ---------------------------------------------------------------------------

function u8(n) {
	return Buffer.from([n & 0xff]);
}

function u16(n) {
	const b = Buffer.alloc(2);
	b.writeUInt16BE(n & 0xffff, 0);
	return b;
}

function i64(n) {
	const b = Buffer.alloc(8);
	b.writeBigInt64BE(BigInt.asIntN(64, n), 0);
	return b;
}

function ascii(s) {
	return Buffer.from(s, "ascii");
}

function varint(n) {
	const out = [];
	n >>>= 0;
	while ((n & ~0x7f) !== 0) {
		out.push((n & 0x7f) | 0x80);
		n >>>= 7;
	}
	out.push(n);
	return Buffer.from(out);
}

/** Java's UUID.nameUUIDFromBytes("OfflinePlayer:" + name). */
function offlineUuid(name) {
	const md5 = crypto.createHash("md5").update("OfflinePlayer:" + name, "utf8").digest();
	md5[6] = (md5[6] & 0x0f) | 0x30;
	md5[8] = (md5[8] & 0x3f) | 0x80;
	return md5;
}

/** 64x64 RGBA gradient used as the query icon. */
function makeIcon() {
	const size = 64;
	const out = Buffer.alloc(size * size * 4);
	for (let y = 0; y < size; y++) {
		for (let x = 0; x < size; x++) {
			const i = (y * size + x) * 4;
			const edge = x < 2 || y < 2 || x >= size - 2 || y >= size - 2;
			out[i] = edge ? 0x1f : 0x30 + Math.floor((x / size) * 0x7f);
			out[i + 1] = edge ? 0x8f : 0x60 + Math.floor((y / size) * 0x7f);
			out[i + 2] = edge ? 0x3f : 0xa0;
			out[i + 3] = 0xff;
		}
	}
	return out;
}

const ICON = makeIcon();

function motdDocument(name) {
	return JSON.stringify({
		type: "motd",
		vers: BRAND + "/" + PLUGIN_VERSION,
		brand: BRAND,
		name: name,
		time: Date.now(),
		data: {
			motd: ["\u00a7aBetterEagler \u00a77local test server", "\u00a77handshake verified"],
			icon: true,
			online: 1,
			max: 20,
			players: [name],
			brand: BRAND,
			vers: PLUGIN_VERSION,
			time: Date.now()
		}
	});
}

// ---------------------------------------------------------------------------
// One connection
// ---------------------------------------------------------------------------

let connectionId = 0;

function handleUpgrade(req, socket) {
	const id = ++connectionId;
	const key = req.headers["sec-websocket-key"];
	if (!key) {
		socket.destroy();
		return;
	}

	const accept = crypto.createHash("sha1").update(key + GUID).digest("base64");
	socket.write(
		"HTTP/1.1 101 Switching Protocols\r\n" +
			"Upgrade: websocket\r\n" +
			"Connection: Upgrade\r\n" +
			"Sec-WebSocket-Accept: " +
			accept +
			"\r\n\r\n"
	);
	socket.setNoDelay(true);

	const state = {
		id,
		remote: req.socket.remoteAddress + ":" + req.socket.remotePort,
		path: req.url,
		playing: false,
		username: "Player" + id
	};

	const log = (...args) => console.log("[conn " + id + "]", ...args);
	log("open", state.path, "from", state.remote);

	const send = (opcode, payload) => {
		if (!socket.destroyed) socket.write(encodeFrame(opcode, payload));
	};
	const sendText = (s) => send(OP_TEXT, Buffer.from(s, "utf8"));
	const sendBinary = (b) => send(OP_BINARY, Buffer.from(b));

	const onHandshakePacket = (data) => {
		if (data.length === 0) return;
		log("raw frame " + data.toString("hex"));
		const packetId = data[0];
		switch (packetId) {
			case 0x01: {
				// CLIENT_VERSION:
				//   [0] 0x01
				//   [1] 2  (protocol groups)
				//   [2..3] u16 3 (count), then u16 3, u16 4, u16 5
				//   [10..11] u16 1 (game count), then u16 762
				//   [14] brand length ...
				const versions = data.readUInt16BE(2);
				const client = {
					brand: "?",
					vers: "?",
					name: "?"
				};
				try {
					let p = 14;
					const brandLen = data[p++];
					client.brand = data.subarray(p, p + brandLen).toString("ascii");
					p += brandLen;
					const versLen = data[p++];
					client.vers = data.subarray(p, p + versLen).toString("ascii");
					p += versLen;
					p++; // password-present flag
					const nameLen = data[p++];
					client.name = data.subarray(p, p + nameLen).toString("ascii");
				} catch (err) {
					log("could not fully parse CLIENT_VERSION:", err.message);
				}
				state.username = client.name && client.name !== "?" ? client.name : state.username;
				log(
					"CLIENT_VERSION versions=" +
						versions +
						" brand=" +
						client.brand +
						" vers=" +
						client.vers +
						" user=" +
						state.username
				);
				sendBinary(
					Buffer.concat([
						u8(0x02),
						u16(HANDSHAKE_PROTOCOL),
						u16(GAME_PROTOCOL),
						u8(BRAND.length),
						ascii(BRAND),
						u8(PLUGIN_VERSION.length),
						ascii(PLUGIN_VERSION),
						u8(0x00), // AUTH_METHOD_NONE
						u16(0), // salt length
						u8(1) // nicknameSelection
					])
				);
				break;
			}
			case 0x04: {
				log("CLIENT_REQUEST_LOGIN");
				const uuid = offlineUuid(state.username);
				sendBinary(
					Buffer.concat([
						u8(0x05),
						u8(state.username.length),
						ascii(state.username),
						uuid.subarray(0, 8),
						uuid.subarray(8, 16),
						varint(0), // no standard capabilities
						u8(0) // no extended capabilities
					])
				);
				break;
			}
			case 0x07:
				log("CLIENT_PROFILE_DATA");
				break;
			case 0x08: {
				log("CLIENT_FINISH_LOGIN -> entering PLAY");
				state.playing = true;
				sendBinary(Buffer.from([0x09]));
				console.log(
					"[conn " +
						id +
						"] handshake complete for " +
						state.username +
						"; client should now show 'Joining world...' (no play packets are sent)"
				);
				break;
			}
			default:
				log("unhandled handshake packet 0x" + packetId.toString(16));
				break;
		}
	};

	const reader = new FrameReader((opcode, payload) => {
		if (opcode === OP_CLOSE) {
			log("close frame");
			socket.end(encodeFrame(OP_CLOSE, Buffer.alloc(0)));
			return;
		}
		if (opcode === OP_PING) {
			send(OP_PONG, payload);
			return;
		}
		if (opcode === OP_PONG) return;
		if (opcode === OP_TEXT) {
			const text = payload.toString("utf8");
			if (/^\s*accept\s*:/i.test(text)) {
				log("query:", text);
				// Server list ping. Answer with the MOTD JSON, then the icon frame
				// the document advertised.
				sendText(motdDocument(state.username));
				sendBinary(ICON);
			} else {
				log("ignoring text frame:", text.slice(0, 80));
			}
			return;
		}
		if (opcode === OP_BINARY) {
			if (state.playing) {
				log("play frame (" + payload.length + " bytes), ignoring");
				return;
			}
			onHandshakePacket(payload);
		}
	});

	socket.on("data", (chunk) => reader.push(chunk));
	socket.on("error", (err) => log("socket error:", err.message));
	socket.on("close", () => log("closed"));
	socket.on("end", () => socket.end());
}

const server = http.createServer((req, res) => {
	res.writeHead(200, { "Content-Type": "text/plain; charset=utf-8" });
	res.end(
		"Eaglercraft mock server\n" +
			"Connect the game to ws://localhost:" +
			PORT +
			" (or ping localhost:" +
			PORT +
			" in the server list)\n"
	);
});

server.on("upgrade", handleUpgrade);
server.listen(PORT, () => {
	console.log("Eaglercraft mock server listening on ws://localhost:" + PORT);
	console.log("  server-list ping: localhost:" + PORT);
	console.log("  join:             ws://localhost:" + PORT);
});
