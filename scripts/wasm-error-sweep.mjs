#!/usr/bin/env node
// Enumerate every function-body type error in a wasm-gc module in one pass.
//
// V8 stops at the first bad function, so we take the function it names, replace
// ONLY that function's body with `unreachable; end` (valid for any signature),
// re-encode the code section, and compile again. Index space (imports, exports,
// start, globals, elem) is untouched, so this is pure diagnosis: it never edits
// source, and it never touches the input file.
//
// usage: node scripts/wasm-error-sweep.mjs in.wasm out.wasm [maxIter] [maxSeconds]

import fs from "fs";

const inPath = process.argv[2];
const outPath = process.argv[3] || "sweep.out.wasm";
const MAX_ITER = parseInt(process.argv[4] || "500", 10);
const MAX_MS = parseFloat(process.argv[5] || "700") * 1000;

if (!inPath) {
	console.error("usage: wasm-error-sweep.mjs in.wasm out.wasm [maxIter] [maxSeconds]");
	process.exit(2);
}

function readLeb(buf, off) {
	let r = 0, s = 0, o = off;
	for (;;) {
		const b = buf[o++];
		r += (b & 0x7f) * Math.pow(2, s);
		if ((b & 0x80) === 0) break;
		s += 7;
	}
	return [r, o];
}

function writeLeb(n) {
	const out = [];
	n = n >>> 0;
	do {
		let byte = n & 0x7f;
		n = n >>> 7;
		if (n !== 0) byte |= 0x80;
		out.push(byte);
	} while (n !== 0);
	return Buffer.from(out);
}

function parseSections(buf) {
	let off = 8;
	const secs = [];
	while (off < buf.length) {
		const id = buf[off];
		const hdr = off;
		off++;
		const [size, payload] = readLeb(buf, off);
		secs.push({ id, hdr, payload, size, end: payload + size });
		off = payload + size;
	}
	return secs;
}

function parseBodies(buf, code) {
	let o = code.payload;
	const [count, o2] = readLeb(buf, o);
	o = o2;
	const list = [];
	for (let i = 0; i < count; i++) {
		const [sz, cs] = readLeb(buf, o);
		list.push({ i, contentStart: cs, size: sz, contentEnd: cs + sz });
		o = cs + sz;
	}
	return { count, list };
}

function rebuild(buf, code, list, patchIdx) {
	const payloadParts = [writeLeb(list.length)];
	for (const b of list) {
		if (b.i === patchIdx) {
			const body = Buffer.from([0x00, 0x00, 0x0b]); // 0 locals, unreachable, end
			payloadParts.push(writeLeb(body.length), body);
		} else {
			payloadParts.push(writeLeb(b.size), buf.subarray(b.contentStart, b.contentEnd));
		}
	}
	const payload = Buffer.concat(payloadParts);
	return Buffer.concat([
		buf.subarray(0, code.hdr),
		Buffer.from([10]),
		writeLeb(payload.length),
		payload,
		buf.subarray(code.end),
	]);
}

let buf = fs.readFileSync(inPath);
const t0 = Date.now();

const origCode = parseSections(buf).find((s) => s.id === 10);
if (!origCode) { console.error("no code section"); process.exit(2); }
const origStarts = parseBodies(buf, origCode).list.map((b) => b.contentStart);

const errors = [];
let fnConst = null; // learned: V8's function # minus our code index
let iter = 0;
let status = "unknown";

for (; iter < MAX_ITER; iter++) {
	let msg = null;
	try {
		await WebAssembly.compile(buf);
		status = "clean";
		break;
	} catch (e) {
		msg = String((e && e.message) || e);
	}
	if (Date.now() - t0 > MAX_MS) { status = "timeout"; break; }
	if (!/function #\d+/.test(msg)) { status = "non-function: " + msg; break; }

	const secs = parseSections(buf);
	const code = secs.find((s) => s.id === 10);
	const { list } = parseBodies(buf, code);

	const mFn = msg.match(/function #(\d+)/);
	const mOff = msg.match(/@\+(\d+)/);
	const N = mFn ? +mFn[1] : -1;

	let idx = -1, rel = -1, byOffset = false;
	if (mOff) {
		const o = +mOff[1];
		for (const b of list) {
			if (o >= b.contentStart && o < b.contentEnd) { idx = b.i; rel = o - b.contentStart; byOffset = true; break; }
		}
	}
	if (idx < 0 && fnConst !== null) {
		const cand = N - fnConst;
		if (cand >= 0 && cand < list.length) { idx = cand; rel = 0; }
	}
	if (idx < 0) { status = "cannot-locate: " + msg; break; }

	let drift = 0;
	if (byOffset) {
		if (fnConst === null) fnConst = N - idx;
		else drift = (N - idx) - fnConst;
	}

	const origOff = origStarts[idx] + rel;
	const sig = msg.replace(/#\d+/g, "#N").replace(/@\+\d+/g, "@+OFF");

	errors.push({ iter, N, codeIdx: idx, origOff, drift, sig, msg });
	const ms = Date.now() - t0;
	console.log(`[${iter}] fn#${N} code#${idx} orig@${origOff}${drift ? " DRIFT=" + drift : ""} (${ms}ms) :: ${msg}`);

	buf = rebuild(buf, code, list, idx);
}

fs.writeFileSync(outPath, buf);

const uniq = {};
for (const e of errors) uniq[e.sig] = (uniq[e.sig] || 0) + 1;
const elapsed = ((Date.now() - t0) / 1000).toFixed(1);

console.log("----");
console.log(`status=${status} masked=${errors.length} elapsed=${elapsed}s -> ${outPath}`);
console.log(`distinct signatures=${Object.keys(uniq).length}`);
for (const [k, v] of Object.entries(uniq).sort((a, b) => b[1] - a[1])) {
	console.log(`  ${v}x  ${k}`);
}
if (errors.length) fs.writeFileSync(outPath + ".errors.json", JSON.stringify(errors, null, 2));
process.exit(status === "clean" ? 0 : 1);
