import fs from "node:fs";
const path = process.argv[2] || "target_teavm_wasm_gc/javascript/assets.epk";
const listOnly = !path.endsWith(".epk") && path !== process.argv[2] ? false : false;
const buf = fs.readFileSync(path);
let off = 8;
const vl = buf[off++]; off += vl;
const fnl = buf[off++]; off += fnl;
const cmt = buf.readUInt16BE(off); off += 2 + cmt;
off += 8;
const numFiles = buf.readUInt32BE(off); off += 4;
const comp = String.fromCharCode(buf[off]); off += 1;
const zlib = await import("node:zlib");
const rawStream = buf.subarray(off, buf.length - 8);
const raw = comp === "0" ? rawStream : (comp === "G" ? zlib.gunzipSync(rawStream) : null);
if (!raw) { console.error("comp", JSON.stringify(comp), "unsupported"); process.exit(1); }

let o = 0;
const u32 = () => { const v = raw.readUInt32BE(o); o += 4; return v; };
const lenByteSeq = () => { const n = raw[o++]; const s = raw.subarray(o, o + n).toString(); o += n; return s; };
const FILE = (0x46 << 24) | (0x49 << 16) | (0x4c << 8) | 0x45;
const names = [];
let badCrc = 0;
for (let i = 0; i < numFiles; i++) {
	const type = u32();
	const nm = lenByteSeq();
	const len = u32();
	if (i === 0) {
		o += len;
		if (raw[o++] !== 0x3e) { console.error(`bad head: obj=${nm} len=${len}`); break; }
		continue;
	}
	if (type === FILE) {
		const crc = u32();
		const data = raw.subarray(o, o + (len - 5)); o += len - 5;
		o += 1; // ':'
		names.push(nm);
		const zl = zlib;
		if (zl.crc32) {
			const actual = zl.crc32(data) >>> 0;
			if ((crc >>> 0) !== actual) { console.error(`CRC mismatch ${nm}: epk=${(crc >>> 0).toString(16)} actual=${actual.toString(16)}`); badCrc++; }
		}
	} else {
		o += len;
	}
	if (raw[o++] !== 0x3e) { console.error(`bad tail near #${i} name=${nm} parsed=${names.length}`); break; }
}
console.log(`parsed ${names.length} of ${numFiles} (crcBad=${badCrc})`);
console.log(names.join("\n"));
