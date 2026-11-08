import fs from "node:fs";
const html = fs.readFileSync("C:/Users/Disas/AppData/Local/Temp/opencode/eagler18/unz/eaglercraft1-8-wasm.html", "utf8");
const marker = 'window.eaglercraftXOpts.assetsURI = "data:application/octet-stream;base64,';
const start = html.indexOf(marker);
if (start < 0) { console.error("marker not found"); process.exit(1); }
const b64start = start + marker.length;
// scan until a '"' (end of the string literal)
const end = html.indexOf('"', b64start);
const b64 = html.slice(b64start, end);
console.log("b64 length:", b64.length);
const buf = Buffer.from(b64, "base64");
fs.writeFileSync("C:/Users/Disas/AppData/Local/Temp/opencode/eagler18/upstream-assets.epk", buf);
console.log("saved", buf.length, "bytes");
console.log("header:", buf.subarray(0, 40).toString("latin1").replace(/[^\x20-\x7e]/g, "."));
