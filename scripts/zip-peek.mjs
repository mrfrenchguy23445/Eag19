import fs from "node:fs";
import zlib from "node:zlib";

// minimal zip reader: list entries (name, size, method, compression using zlib)
const buf = fs.readFileSync("src/wasm-gc-teavm/resources/minecraft-1.19.4-vanilla-data.zip");
let off = 0;
const names = [];
while (off + 4 <= buf.length) {
	const sig = buf.readUInt32LE(off);
	if (sig !== 0x04034b50) break;
	const flags = buf.readUInt16LE(off + 6);
	const method = buf.readUInt16LE(off + 8);
	let nameLen = buf.readUInt16LE(off + 26);
	const extraLen = buf.readUInt16LE(off + 28);
	let name = buf.subarray(off + 30, off + 30 + nameLen).toString();
	let ptr = off + 30 + nameLen + extraLen;
	const compSize = buf.readUInt32LE(off + 18);
	names.push({ name, method, compSize });
	off = ptr + compSize;
}
console.log("entries:", names.length);
const groups = {};
for (const n of names) {
	const parts = n.name.split("/");
	const key = parts[1] || n.name;
	groups[key] = (groups[key] || 0) + 1;
}
console.log("top-level under first seg:", JSON.stringify(groups, null, 1));
console.log(names.slice(0, 15).map(n => n.name).join("\n"));
