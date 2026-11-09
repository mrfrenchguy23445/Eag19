// Build sounds.epk (EPK ver2, gzip 'G') from the Mojang 1.19.4 asset index.
// The vanilla client.jar ships NO sound files at all (they live in asset index
// id=3), so this script fetches them from resources.download.minecraft.net by
// content hash and packs them at their original paths.
//
// usage: node scripts/make-sounds-epk.mjs [out.epk] [--omit=PREFIX]... [--add=PREFIX]...
//
// Default selection: every gameplay/UI sound + menu & creative music.
// (Full music/ = 327MB, ambient/ = 37MB, records/ = 31MB - omitted by default;
// oggs are incompressible so every MB fetched is an MB on disk.)
import fs from "node:fs";
import zlib from "node:zlib";
import path from "node:path";

const outPath = process.argv[2] && !process.argv[2].startsWith("--") ? process.argv[2] : "target_teavm_wasm_gc/javascript/sounds.epk";
const omitRules = (process.argv.filter((a) => a.startsWith("--omit=")).map((a) => a.slice(7)));
const addRules = process.argv.filter((a) => a.startsWith("--add=")).map((a) => a.slice(6));

const omitDefaults = ["minecraft/sounds/music/", "minecraft/sounds/ambient/", "minecraft/sounds/records/"];
const addDefaults = ["minecraft/sounds/music/menu/", "minecraft/sounds/music/game/creative/"];
const omits = omitDefaults.concat(omitRules);
const adds = addDefaults.concat(addRules);

const assetIndexURL = "https://piston-meta.mojang.com/v1/packages/2d1c3dd61f7bae3831fa847c41ad4f559fc04ede/3.json"; // 1.19.4, assetIndex id=3
const resourceBase = "https://resources.download.minecraft.net/";
const cacheDir = "target_teavm_wasm_gc/sounds-cache";

// --- selection ---
console.log("fetching asset index...");
const index = await (await fetch(assetIndexURL)).json();
const selected = {};
for (const [name, obj] of Object.entries(index.objects)) {
	// only sound assets: the asset index also carries lang/icons entries that
	// would shadow assets.epk / eagler.epk content if mounted alongside
	if (!name.startsWith("minecraft/sounds")) continue;
	let inc = false;
	if (omits.every((o) => !(name.startsWith(o) || name === o.replace(/\/$/, "")))) inc = true;
	if (!inc && adds.some((a) => name.startsWith(a))) inc = true;
	if (inc) selected[name] = obj;
}
const names = Object.keys(selected).sort();
const totalMB = names.reduce((s, n) => s + selected[n].size, 0) / 1048576;
console.log(`selected ${names.length} entries, ${totalMB.toFixed(1)} MB raw`);
if (names.length === 0) throw new Error("nothing selected - check omit/add rules");

// --- fetch (hash-addressed, cached by content hash) ---
let errors = 0;
const queue = names.map((n) => [n, selected[n].hash]);
const results = new Map();
async function worker() {
	for (;;) {
		const item = queue.shift();
		if (!item) return;
		const [name, hash] = item;
		const cacheFile = path.join(cacheDir, hash.slice(0, 2), hash);
		if (fs.existsSync(cacheFile)) {
			const data = fs.readFileSync(cacheFile);
			results.set(name, data);
			continue;
		}
		const url = resourceBase + hash.slice(0, 2) + "/" + hash;
		for (let attempt = 1; attempt <= 4; attempt++) {
			try {
				const resp = await fetch(url);
				if (!resp.ok) throw new Error(`HTTP ${resp.status}`);
				const data = Buffer.from(await resp.arrayBuffer());
				fs.mkdirSync(path.dirname(cacheFile), { recursive: true });
				fs.writeFileSync(cacheFile, data);
				results.set(name, data);
				break;
			} catch (e) {
				if (attempt === 4) {
					errors++;
					console.error(`FAILED ${name}: ${e.message}`);
				} else {
					await new Promise((r) => setTimeout(r, attempt * 1500));
				}
			}
		}
	}
}
const t0 = Date.now();
await Promise.all(Array.from({ length: 24 }, worker));
console.log(`fetched ${results.size}/${names.length} entries in ${((Date.now() - t0) / 1000).toFixed(0)}s, ${errors} errors`);
if (errors > 0) throw new Error(`${errors} downloads failed`);

// --- assemble EPK blocks (same writer contract as make-data-epk.mjs / make-folder-epk.mjs) ---
const parts = [];
const i32b = (v) => { const b = Buffer.alloc(4); b.writeInt32BE(v | 0); parts.push(b); };
const neascii = (s) => { const b = Buffer.from(s, "latin1"); if (b.length > 255) throw new Error("name too long " + s); parts.push(Buffer.from([b.length]), b); };

// HEAD
i32b(Buffer.from("HEAD", "latin1").readInt32BE(0));
neascii("file-type");
i32b(13);
parts.push(Buffer.from("epk/resources", "latin1"), Buffer.from([0x3e]));

for (const name of names) {
	const data = results.get(name);
	i32b(Buffer.from("FILE", "latin1").readInt32BE(0));
	neascii(name);
	i32b(5 + data.length);
	const cb = Buffer.alloc(4); cb.writeInt32BE(zlib.crc32(data) | 0); parts.push(cb);
	parts.push(data);
	parts.push(Buffer.from([0x3a, 0x3e]));
}
i32b(Buffer.from("END$", "latin1").readInt32BE(0));

// oggs are incompressible; level 1 keeps pack time short with ~zero size penalty
const compressed = zlib.gzipSync(Buffer.concat(parts), { level: 1 });

const hdr = [];
{
	const o = [];
	const v = Buffer.from("ver2.0", "latin1");
	o.push(Buffer.from([v.length]), v);
	o.push(Buffer.from([0]));           // filename
	o.push(Buffer.from([0, 0]));        // comment
	const d = Buffer.alloc(8); d.writeFloatBE(Date.now(), 0); o.push(d);
	const n = Buffer.alloc(4); n.writeInt32BE(names.length + 1); o.push(n);
	o.push(Buffer.from("G", "latin1"));
	hdr.push(Buffer.concat(o));
}

fs.writeFileSync(outPath, Buffer.concat([Buffer.from("EAGPKG$$", "latin1"), Buffer.concat(hdr), compressed, Buffer.from(":::YEE:>", "latin1")]));
console.log("wrote", outPath, (fs.statSync(outPath).size / 1048576).toFixed(1), "MB");
