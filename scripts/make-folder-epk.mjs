// Repack a folder tree into an EPK ver2 (gzip, comp 'G').
// usage: node scripts/make-folder-epk.mjs <folder> <out.epk> [overlay.json]
// overlay.json (optional): map of epk path -> file content (string or Buffer-ish),
// merged over the tree (replaces existing entries, e.g. the eagler lang overlay).
import fs from "node:fs";
import zlib from "node:zlib";
const folder = process.argv[2] || "C:/Users/Disas/AppData/Local/Temp/opencode/eagler18/eagler-tree";
const outPath = process.argv[3] || "target_teavm_wasm_gc/javascript/eagler.epk";

const walk = (dir, base = "") => {
	const out = [];
	for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
		const local = e.name;
		const rel = base ? base + "/" + local : local;
		if (e.isDirectory()) out.push(...walk(dir + "/" + local, rel));
		else out.push({ name: rel, data: fs.readFileSync(dir + "/" + local) });
	}
	return out;
};
	let entries = walk(folder);
	// overlay: injected/overridden entries (e.g. the eagler i18n lang file)
	const overlayArg = process.argv[4];
	if (overlayArg) {
		const overlay = JSON.parse(fs.readFileSync(overlayArg, "utf8"));
		const byName = new Map(entries.map((e) => [e.name, e]));
		for (const [name, content] of Object.entries(overlay)) {
			byName.set(name, { name, data: Buffer.from(content, "utf8") });
		}
		entries = [...byName.values()].sort((a, b) => (a.name < b.name ? -1 : 1));
		console.log("overlay:", Object.keys(overlay).length, "entries");
	}
console.log("files:", entries.length, "plain bytes:", entries.reduce((s, e) => s + e.data.length, 0));

const parts = [];
const i32b = (v) => { const b = Buffer.alloc(4); b.writeInt32BE(v | 0); parts.push(b); };
const neascii = (s) => { const b = Buffer.from(s, "latin1"); if (b.length > 255) throw new Error("name too long " + s); parts.push(Buffer.from([b.length]), b); };

// HEAD
i32b(Buffer.from("HEAD", "latin1").readInt32BE(0));
neascii("file-type");
i32b(13);
parts.push(Buffer.from("epk/resources", "latin1"), Buffer.from([0x3e]));

for (const e of entries) {
	const crc = zlib.crc32(e.data) | 0;
	i32b(Buffer.from("FILE", "latin1").readInt32BE(0));
	neascii(e.name);
	i32b(5 + e.data.length);
	const cb = Buffer.alloc(4); cb.writeInt32BE(crc); parts.push(cb);
	parts.push(e.data);
	parts.push(Buffer.from([0x3a, 0x3e]));
}
i32b(Buffer.from("END$", "latin1").readInt32BE(0));

const compressed = zlib.gzipSync(Buffer.concat(parts), { level: 9 });
const hdr = [];
{
	const o = [];
	const v = Buffer.from("ver2.0", "latin1");
	o.push(Buffer.from([v.length]), v);              // version string (len-prefixed)
	o.push(Buffer.from([0]));                       // filename
	o.push(Buffer.from([0, 0]));                    // comment
	const d = Buffer.alloc(8); d.writeFloatBE(Date.now(), 0); o.push(d);
	const n = Buffer.alloc(4); n.writeInt32BE(entries.length + 1); o.push(n);
	o.push(Buffer.from("G", "latin1"));
	hdr.push(Buffer.concat(o));
}
fs.writeFileSync(outPath, Buffer.concat([Buffer.from("EAGPKG$$", "latin1"), Buffer.concat(hdr), compressed, Buffer.from(":::YEE:>", "latin1")]));
console.log("wrote", outPath, fs.statSync(outPath).size, "bytes");
