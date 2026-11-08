// Build data.epk (EPK ver2, gzip compression 'G') from the vanilla data zip.
// usage: node scripts/make-data-epk.mjs <vanilla-data.zip> <out.epk>
import fs from "node:fs";
import zlib from "node:zlib";

const zipPath = process.argv[2] || "src/wasm-gc-teavm/resources/minecraft-1.19.4-vanilla-data.zip";
const outPath = process.argv[3] || "target_teavm_wasm_gc/javascript/data.epk";
const buf = fs.readFileSync(zipPath);

// --- read zip ---
const entries = [];
{
	let off = 0;
	while (off + 4 <= buf.length) {
		if (buf.readUInt32LE(off) !== 0x04034b50) break;
		const method = buf.readUInt16LE(off + 8);
		const compSize = buf.readUInt32LE(off + 18);
		const crc = buf.readUInt32LE(off + 14);
		const nameLen = buf.readUInt16LE(off + 26);
		const extraLen = buf.readUInt16LE(off + 28);
		const name = buf.subarray(off + 30, off + 30 + nameLen).toString();
		const dataStart = off + 30 + nameLen + extraLen;
		const raw = buf.subarray(dataStart, dataStart + compSize);
		// data descriptor stripping: local header sizes are authoritative; zero-terminated stored entries
		// reached via dataStart+compSize are correct for this zip (single-segment, no descriptor)
		const data = method === 0 ? Buffer.from(raw) : zlib.inflateRawSync(raw);
		if (name.endsWith("/")) { off = dataStart + compSize; continue; }
		entries.push({ name, data, crc: method === 0 ? crc : (zlib.crc32 ? zlib.crc32(data) : 0) | 0 });
		off = dataStart + compSize;
	}
}
entries.sort((a, b) => (a.name < b.name ? -1 : a.name > b.name ? 1 : 0));
console.log("files:", entries.length, "plain bytes:", entries.reduce((s, e) => s + e.data.length, 0));

// --- assemble EPK blocks (inside the gzip stream) ---
const parts = [];
const i8 = (arr) => parts.push(arr);
const i32 = (v) => { const b = Buffer.alloc(4); b.writeInt32BE(v | 0); parts.push(b); };
const ascii = (s) => { const b = Buffer.from(s, "latin1"); if (b.length > 255) throw new Error("name too long " + s); parts.push(Buffer.from([b.length]), b); };

// HEAD block
i32(Buffer.from("HEAD", "latin1").readInt32BE(0));
ascii("file-type");
i32(13);
i8(Buffer.from("epk/resources", "latin1"));
i8(Buffer.from([0x3e]));

// FILE blocks
for (const e of entries) {
	i32(Buffer.from("FILE", "latin1").readInt32BE(0));
	ascii(e.name);
	i32(5 + e.data.length);
	const crcBuf = Buffer.alloc(4);
	crcBuf.writeInt32BE(e.crc | 0);
	parts.push(crcBuf);
	parts.push(e.data);
	i8(Buffer.from([0x3a])); // ':'
	i8(Buffer.from([0x3e])); // '>'
}

// END block
i32(Buffer.from("END$", "latin1").readInt32BE(0));

const compressed = zlib.gzipSync(Buffer.concat(parts), { level: 9 });

// --- EPK header ---
const head = [];
{
	const o = [];
	const b8 = (v) => o.push(Buffer.from([v]));
	const b32 = (v) => { const b = Buffer.alloc(4); b.writeInt32BE(v | 0); o.push(b); };
	const bs = (s) => { const b = Buffer.from(s, "latin1"); b8(b.length); o.push(b); };
	bs("ver2.0");                       // version string (len-prefixed)
	b8(0); // filename
	b8(0); b8(0); // comment len 0
	const date = Buffer.alloc(8);
	date.writeFloatBE(Date.now(), 0);
	o.push(date);
	b32(entries.length + 1); // numFiles (incl. HEAD)
	o.push(Buffer.from("G", "latin1"));
	head.push(Buffer.concat(o));
}

const footer = Buffer.from(":::YEE:>", "latin1");
fs.writeFileSync(outPath, Buffer.concat([Buffer.from("EAGPKG$$", "latin1"), Buffer.concat(head), compressed, footer]));
console.log("wrote", outPath, fs.statSync(outPath).length, "bytes");
