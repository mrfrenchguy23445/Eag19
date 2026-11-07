// Minimal static HTTP server for the WASM-GC dev client.
//
// usage: node scripts/serve-wasm-dev.mjs [port]
//
// Serves target_teavm_wasm_gc/javascript (index.html + eagruntime.js +
// classes.wasm + EPKs) on http://localhost:8097. Only GET/HEAD are
// supported; every response is no-store so classes.wasm refreshes land.
import http from "http";
import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";

const READDIR = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..", "target_teavm_wasm_gc", "javascript");
const PORT = Number(process.argv[2] || 8097);

const MIME = {
	".html": "text/html; charset=utf-8",
	".js": "text/javascript; charset=utf-8",
	".mjs": "text/javascript; charset=utf-8",
	".wasm": "application/wasm",
	".teadbg": "application/octet-stream",
	".epk": "application/octet-stream",
	".png": "image/png",
	".jpg": "image/jpeg",
	".gif": "image/gif",
	".txt": "text/plain; charset=utf-8",
	".map": "application/json"
};

const server = http.createServer((req, res) => {
	if (req.method !== "GET" && req.method !== "HEAD") {
		res.writeHead(405).end();
		return;
	}
	let urlPath;
	try {
		urlPath = decodeURIComponent((req.url || "/").split("?")[0]);
	} catch (e) {
		res.writeHead(400).end();
		return;
	}
	if (urlPath === "/") urlPath = "/index.html";
	const rel = path.normalize(urlPath).replace(/^([.][.][\\/])+/, "");
	const file = path.join(READDIR, rel);
	if (!file.startsWith(READDIR + path.sep) && file !== READDIR) {
		res.writeHead(403).end();
		return;
	}
	fs.stat(file, (err, st) => {
		if (err || !st.isFile()) {
			res.writeHead(404, { "Content-Type": "text/plain" }).end("404: " + urlPath);
			return;
		}
		res.writeHead(200, {
			"Content-Type": MIME[path.extname(file).toLowerCase()] || "application/octet-stream",
			"Content-Length": st.size,
			"Cache-Control": "no-store",
			"Access-Control-Allow-Origin": "*"
		});
		if (req.method === "HEAD") {
			res.end();
		} else {
			fs.createReadStream(file).pipe(res);
		}
	});
});

server.listen(PORT, "127.0.0.1", () => {
	console.log("WASM-GC dev client: http://localhost:" + PORT + "  (" + READDIR + ")");
});
