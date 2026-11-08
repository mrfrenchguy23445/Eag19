#!/usr/bin/env node
/**
 * Build the Eagler i18n overlay: assets/eagler/lang/en_us.json
 *
 * The 1.19.4 port's lang is the vanilla en_us.json inside assets.epk, which has
 * no Eagler-specific strings, so screens that render Component.translatable for
 * eagler-invented keys (eagler.profile.title / eagler.profile.username /
 * menu.editprofile on the title screen) fall back to the raw key.
 *
 * Upstream EaglercraftX 1.8.8 ships these strings inside its 1.8-era
 * assets/minecraft/lang/en_US.lang under the "eaglercraft.*" prefix; this script
 * lifts them out of the extracted upstream EPK (scripts/epw-split.py +
 * scripts/epk-extract-direct.mjs) and emits them under the "eaglercraft.*" keys
 * verbatim plus the port-renamed aliases it actually references.
 *
 * The output is merged into eagler.epk by scripts/make-folder-epk.mjs
 * (ClientPackSource exposes the "eagler" namespace, and
 * ClientLanguage.loadFrom loads lang/en_us.json from every namespace, so the
 * merge is pure data - no Java rebuild required).
 *
 * usage: node scripts/make-eagler-lang.mjs <en_US.lang> <out-overlay.json>
 */
import fs from "node:fs";

const inPath = process.argv[2] || "C:/Users/Disas/AppData/Local/Temp/opencode/eagler18/langs/assets/minecraft/lang/en_US.lang";
const outPath = process.argv[3] || "scripts/eagler-lang-en_us.json";

const text = fs.readFileSync(inPath, "utf8");
const lines = text.split(/\r\n|\n/);
const map = {};
let n = 0;
for (let line of lines) {
	line = line.trimEnd();
	if (!line || line.startsWith("#") || line.startsWith("//")) continue;
	const eq = line.indexOf("=");
	if (eq <= 0) continue;
	const key = line.slice(0, eq).trim();
	if (!key.startsWith("eaglercraft.")) continue; // only Eagler-owned keys
	let value = line.slice(eq + 1);
	// legacy .lang escapes
	value = value.replace(/\\n/g, "\n").replace(/\\u([0-9a-fA-F]{4})/g, (_, h) =>
		String.fromCharCode(parseInt(h, 16))).replace(/\\\\/g, "\\");
	map[key] = value;
	n++;
}

// Port-renamed aliases (src/main/java/.../profile/EditProfileScreen.java and
// TitleScreen.java translate these; upstream generates them at build time from
// eaglercraft.menu.editProfile / eaglercraft.editProfile.*).
map["menu.editprofile"] = "Edit Profile";
map["eagler.profile.title"] = "Edit Profile";
map["eagler.profile.username"] = "Username";

fs.writeFileSync(outPath, JSON.stringify({ "assets/eagler/lang/en_us.json": JSON.stringify(map, null, "\t") + "\n" }));
console.log(`built ${outPath}: ${Object.keys(map).length} keys (lifted ${n} eaglercraft.* keys)`);
