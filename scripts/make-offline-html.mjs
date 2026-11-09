#!/usr/bin/env node
/*
 * Builds a single self-contained offline HTML for the WASM-GC TeaVM build.
 *
 * The stock makeMainWasmClientBundle task needs epw_src.txt / epw_meta.txt plus
 * an emscripten-built loader.wasm, neither of which exist in this workspace, so
 * this packs the same client without the EPW container: every EPK, classes.wasm,
 * eagruntime.js and the splash images are embedded as base64 and decoded in the
 * page. The result opens straight from file:// with no server and no network.
 *
 * Usage: node scripts/make-offline-html.mjs [--no-sounds] [--out <path>]
 */

import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const js = path.join(root, 'target_teavm_wasm_gc', 'javascript');

const argv = process.argv.slice(2);
const withSounds = !argv.includes('--no-sounds');
const outIdx = argv.indexOf('--out');
const outPath = outIdx >= 0 ? path.resolve(argv[outIdx + 1])
	: path.join(root, 'target_teavm_wasm_gc', 'javascript_dist', 'EaglercraftX_1.19.4_WASM_Offline.html');

const assets = [
	['assets.epk', path.join(js, 'assets.epk')],
	['lang.tmp.epk', path.join(js, 'lang.tmp.epk')],
	['data.epk', path.join(js, 'data.epk')],
	['eagler.epk', path.join(js, 'eagler.epk')]
];
if (withSounds) assets.push(['sounds.epk', path.join(js, 'sounds.epk')]);

const binaries = [['classes.wasm', path.join(js, 'classes.wasm')]];
const images = [
	['splash', path.join(js, 'splash.png'), 'image/png'],
	['pressAnyKey', path.join(js, 'pressAnyKey.png'), 'image/png'],
	['crash', path.join(js, 'crash.png'), 'image/png'],
	['favicon', path.join(js, 'favicon.png'), 'image/png']
];

for (const [, p] of [...assets, ...binaries]) {
	if (!fs.existsSync(p)) {
		console.error('missing input: ' + p);
		process.exit(1);
	}
}

fs.mkdirSync(path.dirname(outPath), { recursive: true });
const fd = fs.openSync(outPath, 'w');
const write = (s) => fs.writeSync(fd, s);

const CHUNK = 3 * 1024 * 1024;
function writeBase64File(p) {
	const buf = Buffer.allocUnsafe(CHUNK);
	const fdIn = fs.openSync(p, 'r');
	let carry = Buffer.alloc(0);
	let n;
	while ((n = fs.readSync(fdIn, buf, 0, CHUNK, null)) > 0) {
		let chunk = Buffer.concat([carry, buf.subarray(0, n)]);
		const usable = chunk.length - (chunk.length % 3);
		if (usable > 0) write(chunk.subarray(0, usable).toString('base64'));
		carry = Buffer.from(chunk.subarray(usable));
	}
	if (carry.length > 0) write(carry.toString('base64'));
	fs.closeSync(fdIn);
}

const runtimeSrc = fs.readFileSync(path.join(js, 'eagruntime.js'), 'utf8');
for (const bad of ['</script', '<!--']) {
	if (runtimeSrc.toLowerCase().includes(bad)) {
		console.error('eagruntime.js contains ' + bad + ' which cannot be inlined safely');
		process.exit(1);
	}
}

const title = withSounds ? 'EaglercraftX 1.19.4 (offline)' : 'EaglercraftX 1.19.4 (offline, no sounds)';

write(`<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>${title}</title>
<style>
html, body { margin: 0; padding: 0; width: 100%; height: 100%; background-color: #101014; }
#_eaglercraftX_game_root { position: relative; width: 100%; height: 100%; }
canvas { display: block; }
#eagOfflineGate { position: fixed; inset: 0; background: #000; color: #ddd;
	font: 16px/1.4 monospace; display: flex; align-items: center; justify-content: center;
	text-align: center; z-index: 999999; user-select: none; cursor: pointer; }
#eagOfflineGate h1 { font-size: 26px; color: #fff; margin: 0 0 18px; }
#eagOfflineGate .go { font-size: 22px; color: #ffff55; margin: 10px 0 4px; }
#eagOfflineGate .hint { font-size: 13px; color: #8b8b8b; margin-top: 26px; max-width: 560px; }
#eagOfflineGate .barWrap { width: 420px; height: 26px; border: 2px solid #808080; margin: 14px auto 0; background: #101010; }
#eagOfflineGate .bar { height: 100%; width: 0%; background: #00aa00; }
#eagOfflineGate .pct { margin-top: 8px; font-size: 13px; color: #a0a0a0; }
</style>
</head>
<body><div id="_eaglercraftX_game_root"></div>
<script>window.__EAG_ASSETS = {};</script>
`);

let totalBytes = 0;
for (const [name, p] of assets) {
	const size = fs.statSync(p).size;
	totalBytes += size;
	process.stdout.write('embedding ' + name + ' (' + (size / 1048576).toFixed(1) + ' MiB)\n');
	write(`<script>window.__EAG_ASSETS[${JSON.stringify(name)}]="`);
	writeBase64File(p);
	write('";</script>\n');
}
for (const [name, p] of binaries) {
	const size = fs.statSync(p).size;
	process.stdout.write('embedding ' + name + ' (' + (size / 1048576).toFixed(1) + ' MiB)\n');
	write(`<script>window.__EAG_ASSETS[${JSON.stringify(name)}]="`);
	writeBase64File(p);
	write('";</script>\n');
}

write(`<script id="eagruntime-src" type="text/plain">${runtimeSrc}</script>
`);

const imageMap = images
	.filter(([, p]) => fs.existsSync(p))
	.map(([name, p, mime]) => `\t\t${JSON.stringify(name)}: "data:${mime};base64,${fs.readFileSync(p).toString('base64')}"`)
	.join(',\n');

write(`<script>
"use strict";
(function() {
	const ASSETS = window.__EAG_ASSETS;
	const IMAGES = {
${imageMap}
	};

	const gateStyle = document.getElementById("eagOfflineGate");
	let phase = "waiting";
	let mainFunc = null;
	let wasmModule = null;
	const epkFiles = [];
	const loadedNames = new Set();
	const wantSounds = ${withSounds ? 'true' : 'false'};
	const order = ["assets.epk", "lang.tmp.epk", "data.epk", "eagler.epk"];
	if (wantSounds) order.push("sounds.epk");

	function toBytes(b64) {
		if (typeof Uint8Array.fromBase64 === "function") return Uint8Array.fromBase64(b64);
		const bin = atob(b64);
		const n = bin.length;
		const out = new Uint8Array(n);
		for (let i = 0; i < n; ++i) out[i] = bin.charCodeAt(i);
		return out;
	}

	// eagruntime.js is needed twice: as the page runtime and as the blob the
	// integrated server worker is started from. Blob URLs stay fetchable from a
	// file:// page, plain file:// URLs do not.
	const runtimeUrl = URL.createObjectURL(new Blob([document.getElementById("eagruntime-src").textContent], { type: "text/javascript" }));

	window.eagOfflineOpts = { "localStorageNamespace": "_eaglercraftX_wasm_offline" };

	window.__eaglercraftXLoaderContext = {
		"getEaglercraftXOpts": function() { return window.eagOfflineOpts; },
		"getEagRuntimeJSURL": function() { return runtimeUrl; },
		"getClassesWASMURL": function() { return wasmModule; },
		"getClassesDeobfWASMURL": function() { return null; },
		"getClassesTEADBGURL": function() { return null; },
		"getEPKFiles": function() { return epkFiles; },
		"getRootElement": function() { return document.getElementById("_eaglercraftX_game_root"); },
		"getMainArgs": function() { return []; },
		"getImageURL": function(idx) {
			switch (idx) {
				case 0: return IMAGES.splash || null;
				case 1: return IMAGES.pressAnyKey || null;
				case 2: return IMAGES.crash || null;
				case 3: return IMAGES.favicon || null;
				default: return null;
			}
		},
		"runMain": function(fn) { mainFunc = fn; showGate(); }
	};

	function showGate() {
		const style = document.createElement("style");
		document.head.appendChild(style);
		const div = document.createElement("div");
		div.id = "eagOfflineGate";
		div.innerHTML = '<div><h1>${title}</h1>' +
			'<div class="go">Click to start</div>' +
			'<div class="hint">Your browser only allows audio after a click. Everything is inside this file - no internet needed.</div></div>';
		document.body.appendChild(div);
		div.addEventListener("click", start);
	}

	function setStatus(text, pct) {
		const el = document.getElementById("eagOfflineGate");
		if (!el) return;
		el.querySelector(".go").textContent = text;
		if (pct !== undefined) {
			let ui = el.querySelector(".loadingUI");
			if (!ui) {
				ui = document.createElement("div");
				ui.className = "loadingUI";
				ui.innerHTML = '<div class="barWrap"><div class="bar"></div></div><div class="pct"></div>';
				el.querySelector("div").appendChild(ui);
			}
			ui.querySelector(".bar").style.width = Math.round(pct * 100) + "%";
			ui.querySelector(".pct").textContent = Math.round(pct * 100) + "%";
		}
	}

	const yieldToUi = () => new Promise(function(r) { setTimeout(r, 0); });

	async function start() {
		if (phase !== "waiting") return;
		phase = "loading";
		try {
			for (let i = 0; i < order.length; ++i) {
				const name = order[i];
				setStatus("Unpacking " + name + "...", i / (order.length + 1));
				await yieldToUi();
				epkFiles.push({ "name": name, "path": "", "data": toBytes(ASSETS[name]) });
				ASSETS[name] = null;
				loadedNames.add(name);
			}
			setStatus("Preparing runtime...", order.length / (order.length + 1));
			await yieldToUi();
			const wasmBytes = toBytes(ASSETS["classes.wasm"]);
			ASSETS["classes.wasm"] = null;
			try {
				wasmModule = await WebAssembly.compile(wasmBytes, { "builtins": ["js-string"] });
			} catch (e) {
				console.warn("compile with string builtins failed, retrying without: " + e);
				wasmModule = await WebAssembly.compile(wasmBytes);
			}
			setStatus("Starting...", 1);
			await yieldToUi();
			const el = document.getElementById("eagOfflineGate");
			if (el) el.remove();
			phase = "running";
			const fn = mainFunc;
			mainFunc = null;
			fn();
		} catch (e) {
			phase = "error";
			console.error("offline boot failed", e);
			const el = document.getElementById("eagOfflineGate");
			if (el) {
				el.querySelector(".go").textContent = "Failed to start: " + (e && e.message ? e.message : e);
				el.querySelector(".go").style.color = "#ff5555";
			}
		}
	}

	if (typeof WebAssembly.promising !== "function" || typeof WebAssembly.Suspending !== "function") {
		document.body.insertAdjacentHTML("beforeend",
			'<div id="eagOfflineGate"><div><h1>Browser too old</h1>' +
			'<div class="go" style="color:#ff5555">This build needs JSPI</div>' +
			'<div class="hint">Use Chrome/Edge 137 or newer (or Firefox with javascript.options.wasm_js_promise_integration enabled).</div></div></div>');
		return;
	}

	const s = document.createElement("script");
	s.src = runtimeUrl;
	document.body.appendChild(s);
})();
</script>
</body>
</html>
`);

fs.closeSync(fd);
const size = fs.statSync(outPath).size;
process.stdout.write('wrote ' + outPath + ' (' + (size / 1048576).toFixed(1) + ' MiB, embedded ' + (totalBytes / 1048576).toFixed(1) + ' MiB)\n');
