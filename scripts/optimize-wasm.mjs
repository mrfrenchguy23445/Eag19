#!/usr/bin/env node
/*
 * Size-optimising post-link pass for the WASM-GC build.
 *
 * TeaVM is already compiled with OptimizationLevel.AGGRESSIVE and obfuscation,
 * and Closure already uses ADVANCED_OPTIMIZATIONS. Binaryen's -Oz is the next
 * lever: it removes unused GC types, rewrites local/GC traffic, and emits a
 * smaller module than TeaVM alone.
 *
 * The tool is optional so a plain Gradle build still works. Set WASM_OPT to a
 * wasm-opt executable (or to the Binaryen node wrapper) to enable it, or put
 * wasm-opt on PATH. Pass --require to make a missing tool a hard failure.
 *
 * Usage:
 *   node scripts/optimize-wasm.mjs <input.wasm> [-o <output.wasm>|--in-place] [--require]
 */

import fs from 'node:fs';
import path from 'node:path';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const argv = process.argv.slice(2);

function usage(message) {
	if (message) console.error(message);
	console.error('usage: node scripts/optimize-wasm.mjs <input.wasm> [-o <output.wasm>|--in-place] [--require]');
	process.exit(2);
}

let input = null;
let output = null;
let inPlace = false;
let requireTool = process.env.EAGLER_REQUIRE_WASM_OPT === '1';

for (let i = 0; i < argv.length; ++i) {
	const arg = argv[i];
	if (arg === '--in-place') {
		inPlace = true;
	} else if (arg === '--require') {
		requireTool = true;
	} else if (arg === '-o' || arg === '--output') {
		output = argv[++i];
		if (!output) usage('missing output path');
	} else if (!arg.startsWith('-') && input === null) {
		input = path.resolve(arg);
	} else {
		usage('unknown argument: ' + arg);
	}
}

if (!input) usage('missing input path');
if (inPlace) output = input;
if (!output) output = input.replace(/\.wasm$/i, '.optimized.wasm');
if (!fs.existsSync(input)) usage('input does not exist: ' + input);

const wasmOptArgs = [
	'-Oz',
	'--all-features',
	'--strip-debug',
	'--gto',
	'--remove-unused-types',
	input,
	'-o',
	null
];

function findWasmOpt() {
	const explicit = process.env.WASM_OPT;
	if (explicit) return explicit.trim();

	const local = [
		path.join(root, 'tools', 'binaryen', 'wasm-opt.exe'),
		path.join(root, 'tools', 'binaryen', 'wasm-opt'),
		path.join(root, 'tools', 'binaryen', 'wasm-opt.js')
	];
	for (const candidate of local) {
		if (fs.existsSync(candidate)) return candidate;
	}

	const finder = process.platform === 'win32' ? 'where' : 'which';
	const probe = spawnSync(finder, ['wasm-opt'], { encoding: 'utf8' });
	if (probe.status === 0) {
		const first = probe.stdout.split(/\r?\n/).map((s) => s.trim()).find(Boolean);
		if (first) return first;
	}
	return null;
}

const wasmOpt = findWasmOpt();
if (!wasmOpt) {
	if (requireTool) {
		console.error('wasm-opt was not found; set WASM_OPT or add it to PATH');
		process.exit(1);
	}
	if (path.resolve(output) !== path.resolve(input)) fs.copyFileSync(input, output);
	console.log('wasm-opt not found; leaving ' + path.basename(output) + ' unoptimized');
	process.exit(0);
}

const tmp = output + '.tmp';
const command = wasmOpt.toLowerCase().endsWith('.js') ? process.execPath : wasmOpt;
const args = wasmOpt.toLowerCase().endsWith('.js') ? [wasmOpt, ...wasmOptArgs] : wasmOptArgs;
args[args.length - 1] = tmp;

const result = spawnSync(command, args, { stdio: ['ignore', 'pipe', 'pipe'] });
if (result.status !== 0) {
	if (result.stderr && result.stderr.length) process.stderr.write(result.stderr);
	if (result.stdout && result.stdout.length) process.stdout.write(result.stdout);
	if (fs.existsSync(tmp)) fs.rmSync(tmp, { force: true });
	console.error('wasm-opt failed with status ' + result.status);
	process.exit(result.status || 1);
}

const optimized = fs.readFileSync(tmp);
if (optimized.length < 8 || optimized[0] !== 0x00 || optimized[1] !== 0x61 || optimized[2] !== 0x73 || optimized[3] !== 0x6d) {
	fs.rmSync(tmp, { force: true });
	console.error('wasm-opt produced an invalid module');
	process.exit(1);
}

const before = fs.statSync(input).size;
if (inPlace) {
	// Keep TeaVM's output so a browser-incompatible -Oz pass can be rolled back
	// without paying for a full relink.
	fs.copyFileSync(input, input + ".unoptimized");
}
fs.renameSync(tmp, output);
const after = optimized.length;
const saved = before - after;
console.log('wasm-opt -Oz: ' + (before / 1048576).toFixed(1) + ' MiB -> ' +
	(after / 1048576).toFixed(1) + ' MiB (' +
	(before > 0 ? (saved * 100 / before).toFixed(1) : '0.0') + '% smaller) -> ' + output);
