import fs from "node:fs";
const path = process.argv[2] || "target_teavm_wasm_gc/javascript/assets.epk";
const buf = fs.readFileSync(path);
let off = 8;
const vl = buf[off++]; off += vl;
const fnl = buf[off++]; off += fnl;
const cmt = buf.readUInt16BE(off); off += 2 + cmt;
off += 8;
const numFiles = buf.readUInt32BE(off); off += 4;
const comp = String.fromCharCode(buf[off]); off += 1;
const raw = comp === "0" ? buf.subarray(off, buf.length - 8) : null;
if (!raw) { console.error("only comp '0' supported; got", comp); process.exit(1); }

let o = 0;
const u32 = () => raw.readUInt32BE(o++);
const FILE = (0x46 << 24) | (0x49 << 16) | (0x4c << 8) | 0x45;
const names = [];
let bad = null;
for (let i = 0; i < numFiles; i++) {
	const type = u32() | 0;
	let sIdx = 1 + raw[o];
	const nm = raw.subarray(o + 1, sIdx).toString();
	const len = u32();
	if (i === 0) {
		offend += len + 1;
		continue;
	}
	if (type === FILE) {
		names.push(nm);
		o += 4 + (len - 5) + 1;
	} else {
		offend += len;
	}
	if (raw[o++] !== 0x3e && !bad) { bad = `#${i} ${nm}`; break; }
}
console.log(JSON.stringify({ numFiles, parsed: names.length, bad, head: raw.subarray(0, 60).toString("latin1").replace(/[^\x20-\x7e]/g, ".") }));
console.log(names.slice(0, 30).join("\n"));
