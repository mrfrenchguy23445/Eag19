// Tiny static server for previewing site/index.html. No dependencies, no caching.
import http from 'node:http';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));
const repoRoot = path.resolve(here, '..');
const siteRoot = path.resolve(repoRoot, 'site');
const port = Number(process.env.PORT || 8123);

const mime = {
	'.html': 'text/html; charset=utf-8',
	'.css': 'text/css; charset=utf-8',
	'.js': 'text/javascript; charset=utf-8',
	'.json': 'application/json; charset=utf-8',
	'.png': 'image/png',
	'.svg': 'image/svg+xml',
};

http.createServer((req, res) => {
	const rawPath = decodeURIComponent(new URL(req.url, 'http://localhost').pathname);

	// The page lives in site/, but its links point at real repo artifacts such
	// as sites-web/public/index.html. Serve from the repo root so those links
	// resolve in the preview exactly as they will when hosted from the repo.
	const route = rawPath === '/'
		? '/site/index.html'
		: (rawPath === '/styles.css' || rawPath === '/launcher.js')
			? '/site' + rawPath
			: rawPath;
	const file = path.resolve(repoRoot, '.' + route);

	if (!file.startsWith(repoRoot + path.sep)) {
		res.writeHead(403).end('forbidden');
		return;
	}

	fs.readFile(file, (err, data) => {
		if (err) {
			res.writeHead(404, { 'content-type': 'text/plain; charset=utf-8' }).end('not found');
			return;
		}
		res.writeHead(200, {
			'content-type': mime[path.extname(file)] || 'application/octet-stream',
			'cache-control': 'no-store',
		}).end(data);
	});
}).listen(port, () => {
	console.log(`exclamationstudios preview: http://localhost:${port}`);
});
