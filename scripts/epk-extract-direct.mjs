// Extract EPK entries matching a prefix into a folder (comp '0' or 'G').
// usage: node scripts/epk-extract-direct.mjs <in.epk> <prefix> <outdir>
import fs from "node:fs";
import zlib from "node:zlib";
const inPath = process.argv[2];
const prefix = process.argv[3] || "assets/eagler/";
const outdir = process.argv[4];
const buf = fs.readFileSync(inPath);
let off = 8;
const vl = buf[off++]; off += vl;
const fnl = buf[off++]; off += fnl;
const cmt = buf.readUInt16BE(off); off += 2 + cmt;
off += 8;
const numFiles = buf.readUInt32BE(off); off += 4;
const comp = String.fromCharCode(buf[off]); off += 1;
const rawStream = buf.subarray(off, buf.length - 8);
const raw = comp === "0" ? rawStream : (comp === "G" ? zlib.gunzipSync(rawStream) : null);
if (!raw) { console.error("comp", JSON.stringify(comp), "unsupported"); process.exit(1); }

let o = 0;
const u32 = () => { const v = raw.readUInt32BE(o); o += 4; return v; };
const nameSeq = () => { const n = raw[o++]; const s = raw.subarray(o, o + n).toString(); o += n; return s; };
const FILE = (0x46 << 24) | (0x49 << 16) | (0x4c << 8) | 0x45;
const crc32 = (that, crc = 0) => { // slow table-less crc for verification
};
let dataset = {}; // name -> Buffer
for (let i = 0; i < numFiles; i++) {
	const type = u32();
	const nm = nameSeq();
	const len = u32();
	if (i === 0) { o += len; o++; continue; }
	if (type === FILE) {
		const crc = raw.readUInt32BE(o); o += 4;
		const data = raw.subarray(o, o + (len - 5)); o += len - 5; o += 1; // ':' 
		if (nm.startsWith(prefix)) dataset[nm] = data;
		dataset["$lastKey"] = nm;
	} else {
		o += len;
		dataset["$lastKey"] = nm;
	}
	if (raw[o++] !== 0x3e) { console.error(`bad tail near #${i} name=${nm}`); process.exit(1); }
}
let n = 0;
for (const [k, v] of Object.entries(dataset)) {
	if (k.startsWith("$")) continue;
	const f = outdir + "/" + k;
	fs.mkdirSync(f.slice(0, f.lastIndexOf("/")), { recursive: true });
	fs.writeFileSync(f, v);
	n++;
}
console.log(`extracted ${n} entries from under ${JSON.stringify(prefix)} into ${outdir}`);
