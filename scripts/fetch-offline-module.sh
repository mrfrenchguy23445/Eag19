#!/usr/bin/env bash
# Copy one resolved artifact into gradle/local-libs and fetch its metadata.
#
# Called by scripts/build-offline-repo.sh once per module, in parallel. Takes one
# line from the dumpOfflineDeps output: group|artifact|version|artifact-file-path.
#
# Gradle's module cache keeps artifact files but not POMs (descriptors live in a
# binary metadata store), so the .pom/.module are fetched from whichever upstream
# repository serves them. Without a POM, Gradle cannot see a module's transitive
# dependencies and an offline build fails on the next missing symbol.
set -uo pipefail

IFS='|' read -r group artifact version file <<< "${1:-}"
if [ -z "${group:-}" ] || [ -z "${artifact:-}" ] || [ -z "${version:-}" ]; then
	exit 2
fi

DIR="$(cd "$(dirname "$0")/.." && pwd)"
REPO="$DIR/gradle/local-libs"

# Most specific first. Mojang hosts its own libraries, the TeaVM fork hosts the
# patched toolchain, the rest is Central.
BASES=(
	"https://libraries.minecraft.net"
	"https://eaglercraft-teavm-fork.github.io/maven"
	"https://repo1.maven.org/maven2"
)

path="${group//.//}/$artifact/$version"
dest="$REPO/$path"
mkdir -p "$dest"

if [ -n "$file" ] && [ -f "$file" ]; then
	base="$(basename "$file")"
	[ -f "$dest/$base" ] || cp "$file" "$dest/$base"
fi

# No cached file (the plugins' own dependency closure is never in a project
# configuration, so dumpOfflineDeps cannot list it): download the jar itself.
if [ ! -f "$dest/$artifact-$version.jar" ]; then
	for b in "${BASES[@]}"; do
		code=$(curl -sS -L --connect-timeout 8 --max-time 60 -o "$dest/$artifact-$version.jar.tmp" \
			-w '%{http_code}' "$b/$path/$artifact-$version.jar" 2>/dev/null) || code=000
		if [ "$code" = "200" ] && [ -s "$dest/$artifact-$version.jar.tmp" ]; then
			mv "$dest/$artifact-$version.jar.tmp" "$dest/$artifact-$version.jar"
			break
		fi
		rm -f "$dest/$artifact-$version.jar.tmp"
	done
fi

for ext in pom module; do
	out="$dest/$artifact-$version.$ext"
	[ -s "$out" ] && continue
	for b in "${BASES[@]}"; do
		code=$(curl -sS -L --connect-timeout 8 --max-time 25 -o "$out.tmp" \
			-w '%{http_code}' "$b/$path/$artifact-$version.$ext" 2>/dev/null) || code=000
		if [ "$code" = "200" ] && [ -s "$out.tmp" ]; then
			mv "$out.tmp" "$out"
			break
		fi
		rm -f "$out.tmp"
	done
done

# A POM's <parent> is a module in its own right, and Gradle refuses the child
# without it -- e.g. guava-parent, netty-parent, oss-parent. Gradle only reports
# those one round at a time, so they are followed here instead: parent chains are
# shallow, but there are dozens of them and one round per link would take hours.
DEPTH="${FETCH_DEPTH:-0}"
if [ -s "$dest/$artifact-$version.pom" ] && [ "$DEPTH" -lt 6 ]; then
	pom="$dest/$artifact-$version.pom"
	block=$(sed -n '/<parent>/,/<\/parent>/p' "$pom")
	pg=$(printf '%s\n' "$block" | sed -n 's:.*<groupId>\([^<]*\)</groupId>.*:\1:p' | head -1)
	pa=$(printf '%s\n' "$block" | sed -n 's:.*<artifactId>\([^<]*\)</artifactId>.*:\1:p' | head -1)
	pv=$(printf '%s\n' "$block" | sed -n 's:.*<version>\([^<]*\)</version>.*:\1:p' | head -1)
	if [ -n "$pg" ] && [ -n "$pa" ] && [ -n "$pv" ] \
		&& [ "$pg:$pa:$pv" != "$group:$artifact:$version" ]; then
		FETCH_DEPTH=$((DEPTH + 1)) bash "$DIR/scripts/fetch-offline-module.sh" "$pg|$pa|$pv|"
	fi
fi
exit 0
