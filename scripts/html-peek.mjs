import fs from "node:fs";
const html = fs.readFileSync("C:/Users/Disas/AppData/Local/Temp/opencode/eagler18/unz/eaglercraft1-8-wasm.html", "utf8");
let idx = 0; let count = 0;
while ((idx = html.indexOf("assetsURI", idx)) !== -1 && count < 20) {
	console.log("at", idx, JSON.stringify(html.slice(idx - 160, idx + 300)));
	count++; idx += 9;
}
