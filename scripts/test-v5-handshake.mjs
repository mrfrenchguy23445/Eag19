// Regression check for the raw V5 handshake sent by EaglercraftHandshaker.
//
// Starts scripts/mock-eagler-server.mjs on a private port, sends the exact
// CLIENT_VERSION layout produced by the Java client, and verifies which game
// protocol the mock server selects.
//
//   node scripts/test-v5-handshake.mjs

import { spawn } from "node:child_process";
import crypto from "node:crypto";
import net from "node:net";
import path from "node:path";
import { fileURLToPath } from "node:url";

const here = path.dirname(fileURLToPath(import.meta.url));
const serverScript = path.join(here, "mock-eagler-server.mjs");
const port = Number(process.env.PORT || 8188);

function u8(value) {
	return Buffer.from([value]);
}

function u16(value) {
	const b = Buffer.alloc(2);
	b.writeUInt16BE(value);
	return b;
}

function ascii(value) {
	return Buffer.from(value, "ascii");
}

function clientFrame(opcode, payload) {
	const mask = crypto.randomBytes(4);
	const masked = Buffer.allocUnsafe(payload.length);
	for (let i = 0; i < payload.length; ++i) masked[i] = payload[i] ^ mask[i & 3];

	let header;
	if (payload.length < 126) {
		header = Buffer.alloc(2);
		header[1] = 0x80 | payload.length;
	} else {
		header = Buffer.alloc(4);
		header[1] = 0x80 | 126;
		header.writeUInt16BE(payload.length, 2);
	}
	header[0] = 0x80 | opcode;
	return Buffer.concat([header, mask, masked]);
}

function clientVersion(protocols) {
	const brand = ascii("Eag19Test");
	const version = ascii("1.19.4");
	const name = ascii("Player");
	return Buffer.concat([
		u8(0x01),
		u8(2),
		u16(3),
		u16(3), u16(4), u16(5),
		u16(protocols.length),
		...protocols.map(u16),
		u8(brand.length), brand,
		u8(version.length), version,
		u8(0),
		u8(name.length), name
	]);
}

function readVarint(buffer, offset) {
	let value = 0;
	let shift = 0;
	for (;;) {
		if (offset >= buffer.length) throw new Error("truncated varint in login response");
		const byte = buffer[offset++];
		value |= (byte & 0x7f) << shift;
		if ((byte & 0x80) === 0) return { value, offset };
		shift += 7;
		if (shift > 35) throw new Error("varint too large in login response");
	}
}

function clientRequestLogin(username) {
	const name = ascii(username);
	const server = ascii("default");
	return Buffer.concat([
		u8(0x04),
		u8(name.length), name,
		u8(server.length), server,
		u8(0), // no password
		u8(0), u8(0), // V4 fields
		u8(0x1c), // REDIRECT | NOTIFICATION | PAUSE_MENU
		u8(1), u8(1), u8(1), u8(0)
	]);
}

function clientProfileData() {
	const type = ascii("brand_uuid_v1");
	return Buffer.concat([
		u8(0x07),
		u8(1),
		u8(type.length), type,
		u16(16),
		Buffer.alloc(16)
	]);
}

function parseAllowLogin(response) {
	if (response[0] !== 0x05) throw new Error("expected SERVER_ALLOW_LOGIN (0x05), got 0x" + response[0].toString(16));
	let p = 1;
	const nameLen = response[p++];
	if (nameLen === undefined || p + nameLen > response.length) throw new Error("truncated login name");
	const name = response.subarray(p, p + nameLen).toString("ascii");
	p += nameLen;
	if (p + 16 > response.length) throw new Error("truncated login UUID");
	p += 16;

	const caps = readVarint(response, p);
	p = caps.offset;
	let capCount = 0;
	let remaining = caps.value >>> 0;
	while (remaining !== 0) {
		capCount += remaining & 1;
		remaining >>>= 1;
	}
	if (p + capCount > response.length) throw new Error("truncated standard capabilities");
	p += capCount;

	const extCaps = response[p++];
	if (extCaps === undefined) throw new Error("missing extended capability count");
	if (p + extCaps * 17 > response.length) throw new Error("truncated extended capabilities");
	p += extCaps * 17;
	if (p !== response.length) throw new Error("unread bytes in SERVER_ALLOW_LOGIN: " + (response.length - p));
	return { name, standardCaps: caps.value, extendedCaps: extCaps };
}

function parseFrames(buffer) {
	const frames = [];
	while (buffer.length >= 2) {
		const b0 = buffer[0];
		const b1 = buffer[1];
		let len = b1 & 0x7f;
		let offset = 2;
		if (len === 126) {
			if (buffer.length < 4) break;
			len = buffer.readUInt16BE(2);
			offset = 4;
		} else if (len === 127) {
			if (buffer.length < 10) break;
			len = Number(buffer.readBigUInt64BE(2));
			offset = 10;
		}
		if (b1 & 0x80) throw new Error("server frame was masked");
		if (buffer.length < offset + len) break;
		frames.push({ opcode: b0 & 0x0f, payload: buffer.subarray(offset, offset + len) });
		buffer = buffer.subarray(offset + len);
	}
	return { frames, rest: buffer };
}

function startServer() {
	return new Promise((resolve, reject) => {
		const child = spawn(process.execPath, [serverScript, String(port)], {
			cwd: path.dirname(here),
			stdio: ["ignore", "pipe", "pipe"]
		});
		let output = "";
		const onData = (chunk) => {
			output += chunk.toString();
			if (output.includes("listening on ws://localhost:" + port)) {
				child.stdout.off("data", onData);
				resolve(child);
			}
		};
		child.stdout.on("data", onData);
		child.stderr.on("data", (chunk) => { output += chunk.toString(); });
		child.once("error", reject);
		child.once("exit", (code) => reject(new Error("mock server exited early (" + code + "): " + output)));
	});
}

function openWebSocket() {
	return new Promise((resolve, reject) => {
		const socket = net.connect(port, "127.0.0.1");
		socket.once("error", reject);
		socket.once("connect", () => {
			const key = crypto.randomBytes(16).toString("base64");
			socket.write(
				"GET / HTTP/1.1\r\n" +
				"Host: localhost:" + port + "\r\n" +
				"Upgrade: websocket\r\n" +
				"Connection: Upgrade\r\n" +
				"Sec-WebSocket-Key: " + key + "\r\n" +
				"Sec-WebSocket-Version: 13\r\n\r\n"
			);
			let data = Buffer.alloc(0);
			const onData = (chunk) => {
				data = Buffer.concat([data, chunk]);
				const end = data.indexOf("\r\n\r\n");
				if (end < 0) return;
				const headers = data.subarray(0, end).toString();
				if (!/101 Switching Protocols/.test(headers)) {
					socket.destroy();
					reject(new Error("WebSocket upgrade failed: " + headers.split("\r\n")[0]));
					return;
				}
				socket.off("data", onData);
				socket.off("error", reject);
				const { rest } = parseFrames(data.subarray(end + 4));
				resolve({ socket, buffer: rest });
			};
			socket.on("data", onData);
			socket.once("error", reject);
		});
	});
}

function nextBinary(conn, timeoutMs = 5000) {
	return new Promise((resolve, reject) => {
		const { socket } = conn;
		const timer = setTimeout(() => {
			cleanup();
			reject(new Error("timed out waiting for a binary server frame"));
		}, timeoutMs);
		const onData = (chunk) => {
			conn.buffer = Buffer.concat([conn.buffer, chunk]);
			let parsed;
			try {
				parsed = parseFrames(conn.buffer);
			} catch (err) {
				cleanup();
				reject(err);
				return;
			}
			conn.buffer = parsed.rest;
			const binary = parsed.frames.find((f) => f.opcode === 0x02);
			if (binary) {
				cleanup();
				resolve(binary.payload);
			}
		};
		const onError = (err) => {
			cleanup();
			reject(err);
		};
		function cleanup() {
			clearTimeout(timer);
			socket.off("data", onData);
			socket.off("error", onError);
		}
		socket.on("data", onData);
		socket.on("error", onError);
	});
}

async function check(protocols, expected, fullLogin = false) {
	const conn = await openWebSocket();
	try {
		conn.socket.write(clientFrame(0x02, clientVersion(protocols)));
		const response = await nextBinary(conn);
		if (response[0] !== 0x02) throw new Error("expected SERVER_VERSION (0x02), got 0x" + response[0].toString(16));
		const handshake = response.readUInt16BE(1);
		const selected = response.readUInt16BE(3);
		if (handshake !== 5) throw new Error("expected V5 handshake, got v" + handshake);
		if (selected !== expected) throw new Error("expected protocol " + expected + ", got " + selected);
		console.log("PASS advertised [" + protocols.join(", ") + "] -> selected " + selected);

		if (fullLogin) {
			conn.socket.write(clientFrame(0x02, clientRequestLogin("Player")));
			const allow = parseAllowLogin(await nextBinary(conn));
			if (allow.name !== "Player") throw new Error("login response named " + allow.name + " instead of Player");
			conn.socket.write(clientFrame(0x02, clientProfileData()));
			conn.socket.write(clientFrame(0x02, Buffer.from([0x08])));
			const finish = await nextBinary(conn);
			if (finish[0] !== 0x09) throw new Error("expected SERVER_FINISH_LOGIN (0x09), got 0x" + finish[0].toString(16));
			console.log("PASS V5 login response parsed (standardCaps=" + allow.standardCaps
				+ ", extendedCaps=" + allow.extendedCaps + ") and finish received");
		}
	} finally {
		conn.socket.destroy();
	}
}

const server = await startServer();
try {
	await check([764, 758], 764, true);
	await check([758], 758);
	await check([762], 762);
	console.log("V5 handshake regression checks passed");
} finally {
	server.kill();
}
