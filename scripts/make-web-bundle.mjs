#!/usr/bin/env node
/*
 * Builds a static web bundle of the WASM-GC client for hosting (Sites/Pages).
 *
 * Hosting assets are limited in size (10 MiB on Freebuff Sites, 25 MiB on
 * Cloudflare Pages), so classes.wasm / sounds.epk / assets.epk are split into
 * 8 MiB parts and the page concatenates them before use. Every file in the
 * output therefore fits both hosts, and the bundle is also servable as-is by
 * `npx wrangler pages deploy public`.
 *
 * Usage: node scripts/make-web-bundle.mjs [--no-sounds] [--out <dir>]
 */

import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const js = path.join(root, 'target_teavm_wasm_gc', 'javascript');

const argv = process.argv.slice(2);
const withSounds = !argv.includes('--no-sounds');
const outIdx = argv.indexOf('--out');
const out = outIdx >= 0 ? path.resolve(argv[outIdx + 1]) : path.join(root, 'sites-web', 'public');
const pub = out;
const dataDir = path.join(pub, 'data');

const CHUNK = 8 * 1024 * 1024;

const splitAssets = [
	['assets.epk', path.join(js, 'assets.epk')],
	['classes.wasm', path.join(js, 'classes.wasm')]
];
if (withSounds) splitAssets.push(['sounds.epk', path.join(js, 'sounds.epk')]);

const wholeAssets = [
	['lang.tmp.epk', path.join(js, 'lang.tmp.epk')],
	['data.epk', path.join(js, 'data.epk')],
	['eagler.epk', path.join(js, 'eagler.epk')]
];

const statics = ['eagruntime.js', 'splash.png', 'pressAnyKey.png', 'crash.png', 'favicon.png'];

for (const [, p] of [...splitAssets, ...wholeAssets]) {
	if (!fs.existsSync(p)) {
		console.error('missing input: ' + p);
		process.exit(1);
	}
}

fs.rmSync(pub, { recursive: true, force: true });
fs.mkdirSync(dataDir, { recursive: true });

const manifest = [];

function norm(name, size, parts) {
	manifest.push({ name, size, parts });
}

for (const [name, p] of splitAssets) {
	const size = fs.statSync(p).size;
	const fd = fs.openSync(p, 'r');
	const buf = Buffer.allocUnsafe(CHUNK);
	let parts = 0;
	let n;
	while ((n = fs.readSync(fd, buf, 0, CHUNK, null)) > 0) {
		fs.writeFileSync(path.join(dataDir, name + '.' + String(parts).padStart(2, '0')), buf.subarray(0, n));
		parts++;
	}
	fs.closeSync(fd);
	norm(name, size, parts);
	process.stdout.write('split ' + name + ' -> ' + parts + ' parts (' + (size / 1048576).toFixed(1) + ' MiB)\n');
}

for (const [name, p] of wholeAssets) {
	const size = fs.statSync(p).size;
	fs.copyFileSync(p, path.join(dataDir, name + '.00'));
	norm(name, size, 1);
	process.stdout.write('staged ' + name + ' (' + (size / 1048576).toFixed(1) + ' MiB)\n');
}

for (const f of statics) {
	const src = path.join(js, f);
	if (!fs.existsSync(src)) continue;
	fs.copyFileSync(src, path.join(pub, f));
}
fs.writeFileSync(path.join(pub, 'manifest.json'), JSON.stringify(manifest, null, '\t'));

const title = 'EaglercraftX 1.19.4';

const html = `<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>${title}</title>
<link rel="icon" href="favicon.png">
<style>
html, body { margin: 0; padding: 0; width: 100%; height: 100%; background-color: #101014; }
#_eaglercraftX_game_root { position: relative; width: 100%; height: 100%; }
canvas { display: block; }
#eagGate { position: fixed; inset: 0; background: #000; color: #ddd;
	font: 16px/1.4 monospace; display: flex; align-items: center; justify-content: center;
	text-align: center; z-index: 999999; user-select: none; cursor: pointer; }
#eagGate h1 { font-size: 26px; color: #fff; margin: 0 0 18px; }
#eagGate .go { font-size: 22px; color: #ffff55; margin: 10px 0 4px; }
#eagGate .hint { font-size: 13px; color: #8b8b8b; margin-top: 26px; max-width: 560px; }
#eagGate .barWrap { width: 420px; height: 26px; border: 2px solid #808080; margin: 14px auto 0; background: #101010; }
#eagGate .bar { height: 100%; width: 0%; background: #00aa00; }
#eagGate .pct { margin-top: 8px; font-size: 13px; color: #a0a0a0; }
</style>
</head>
<body><div id="_eaglercraftX_game_root"></div>
<script>
"use strict";
(function() {
	const GATE_HTML = '<div><h1>${title}</h1>' +
		'<div class="go">Click to play</div>' +
		'<div class="hint">Your browser only allows audio after a click. The client is ~215 MB and downloads once, then it is cached by the browser.</div></div>';

	let phase = "waiting";
	let mainFunc = null;
	let wasmModule = null;
	let runtimeUrl = null;
	const epkFiles = [];

	const gate = document.createElement("div");
	gate.id = "eagGate";
	gate.innerHTML = GATE_HTML;
	document.body.appendChild(gate);
	gate.addEventListener("click", start);

	window.eagWebOpts = { "localStorageNamespace": "_eaglercraftX_wasm_web" };

	window.__eaglercraftXLoaderContext = {
		"getEaglercraftXOpts": function() { return window.eagWebOpts; },
		"getEagRuntimeJSURL": function() { return runtimeUrl; },
		"getClassesWASMURL": function() { return wasmModule; },
		"getClassesDeobfWASMURL": function() { return null; },
		"getClassesTEADBGURL": function() { return null; },
		"getEPKFiles": function() { return epkFiles; },
		"getRootElement": function() { return document.getElementById("_eaglercraftX_game_root"); },
		"getMainArgs": function() { return []; },
		"getImageURL": function(idx) {
			switch (idx) {
				case 0: return "splash.png";
				case 1: return "pressAnyKey.png";
				case 2: return "crash.png";
				case 3: return "favicon.png";
				default: return null;
			}
		},
		"runMain": function(fn) { mainFunc = fn; }
	};

	function setStatus(text, pct) {
		const el = document.querySelector("#eagGate .go");
		if (el) el.textContent = text;
		if (pct === undefined) return;
		let ui = document.querySelector("#eagGate .loadingUI");
		if (!ui) {
			ui = document.createElement("div");
			ui.className = "loadingUI";
			ui.innerHTML = '<div class="barWrap"><div class="bar"></div></div><div class="pct"></div>';
			gate.querySelector("div").appendChild(ui);
		}
		ui.querySelector(".bar").style.width = Math.round(pct * 100) + "%";
		ui.querySelector(".pct").textContent = Math.round(pct * 100) + "% of " + Math.round(totalBytes / 1048576) + " MB";
	}

	let totalBytes = 0;
	let doneBytes = 0;

	async function fetchPart(url) {
		const resp = await fetch(url);
		if (!resp.ok) throw new Error("HTTP " + resp.status + " for " + url);
		return new Uint8Array(await resp.arrayBuffer());
	}

	async function loadAsset(entry) {
		const parts = [];
		for (let i = 0; i < entry.parts; ++i) {
			parts.push(await fetchPart("data/" + entry.name + "." + String(i).padStart(2, "0")));
		}
		doneBytes += entry.size;
		setStatus("Loading " + entry.name + "...", Math.min(0.98, doneBytes / totalBytes));
		const merged = new Uint8Array(entry.size);
		let off = 0;
		for (const p of parts) {
			merged.set(p, off);
			off += p.length;
		}
		return merged;
	}

	async function start() {
		if (phase !== "waiting") return;
		phase = "loading";
		try {
			const manifest = await (await fetch("manifest.json")).json();
			totalBytes = manifest.reduce((a, e) => a + e.size, 0);
			const runtimeText = await (await fetch("eagruntime.js")).text();
			runtimeUrl = URL.createObjectURL(new Blob([runtimeText], { type: "text/javascript" }));

			let wasmBytes = null;
			for (const entry of manifest) {
				if (entry.name === "classes.wasm") {
					wasmBytes = await loadAsset(entry);
				} else {
					epkFiles.push({ "name": entry.name, "path": "", "data": await loadAsset(entry) });
				}
			}

			setStatus("Compiling...", 0.99);
			await new Promise(function(r) { setTimeout(r, 0); });
			try {
				wasmModule = await WebAssembly.compile(wasmBytes, { "builtins": ["js-string"] });
			} catch (e) {
				wasmModule = await WebAssembly.compile(wasmBytes);
			}

			gate.remove();
			phase = "running";
			const fn = mainFunc;
			mainFunc = null;
			fn();
		} catch (e) {
			phase = "error";
			console.error("boot failed", e);
			const go = document.querySelector("#eagGate .go");
			if (go) {
				go.textContent = "Failed to start: " + (e && e.message ? e.message : e);
				go.style.color = "#ff5555";
			}
		}
	}

	if (typeof WebAssembly.promising !== "function" || typeof WebAssembly.Suspending !== "function") {
		const go = document.querySelector("#eagGate .go");
		go.textContent = "This build needs JSPI - use Chrome/Edge 137+";
		go.style.color = "#ff5555";
		return;
	}

	const s = document.createElement("script");
	s.src = "eagruntime.js";
	s.onload = function() {
		// eagruntime.js calls back into runMain() once the loader context exists.
		if (!mainFunc) console.warn("runtime loaded without calling runMain");
	};
	document.body.appendChild(s);
})();
</script>
</body>
</html>
`;

fs.writeFileSync(path.join(pub, 'index.html'), html);

let total = 0;
let count = 0;
for (const f of fs.readdirSync(pub, { recursive: true, withFileTypes: true })) {
	if (f.isFile()) {
		const st = fs.statSync(path.join(f.parentPath || f.path, f.name));
		total += st.size;
		count++;
	}
}
process.stdout.write('wrote ' + count + ' files, ' + (total / 1048576).toFixed(1) + ' MiB total -> ' + pub + '\n');
