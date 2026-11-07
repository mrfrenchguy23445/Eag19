#!/usr/bin/env bash
# Build gradle/local-libs into a self-contained Maven repository for this workspace.
#
# The Gradle module cache keeps artifact *files* but not POMs (descriptors live in a
# binary metadata store), so a copied cache is not portable. This script turns the
# resolved-artifact list produced by scripts/dump-offline-deps.init.gradle into a real
# Maven repo layout instead: the jar from the cache, plus the .pom/.module that the
# upstream repo publishes. After this runs, the build resolves with no network at all.
#
# usage:
#   ./gradlew -I scripts/dump-offline-deps.init.gradle dumpOfflineDeps > deps.txt
#   scripts/build-offline-repo.sh deps.txt
#
# Idempotent: existing files are kept, so it can be re-run to fill gaps.
set -euo pipefail

DIR="$(cd "$(dirname "$0")/.." && pwd)"
REPO="$DIR/gradle/local-libs"
LIST="${1:-}"
[ -n "$LIST" ] && [ -f "$LIST" ] || {
	echo "usage: $0 <deps.txt from dumpOfflineDeps>" >&2
	exit 2
}

mkdir -p "$REPO"
WORK="$REPO/.modules.txt"
: > "$WORK"

# Artifacts: group|artifact|version|file. The root project's own jar is a project
# dependency, not something a repository serves, so it is not listed.
grep '^ART|' "$LIST" | awk -F'|' '{print $2"|"$3"|"$4"|"$5}' | sort -u |
	while IFS= read -r line; do
		case "$line" in
		eaglercraft-workspace\|*) ;;
		*) printf '%s\n' "$line" >> "$WORK" ;;
		esac
	done

# The plugin *markers* (org.teavm:org.teavm.gradle.plugin) are tiny POMs that no
# configuration resolves as an artifact, so they are added explicitly: 0.9.2 comes
# from Central, the EAGLER fork publishes its own for 0.12.1-EAGLER-R3.
printf '%s\n' \
	"org.teavm|org.teavm.gradle.plugin|0.9.2|" \
	"org.teavm|org.teavm.gradle.plugin|0.12.1-EAGLER-R3|" >> "$WORK"

total=$(wc -l < "$WORK")
echo "resolving metadata for $total modules (8 at a time)"
# shellcheck disable=SC2016
xargs -d '\n' -P 8 -I{} bash "$DIR/scripts/fetch-offline-module.sh" "{}" < "$WORK"
rm -f "$WORK"

jars=$(find "$REPO" -name '*.jar' | wc -l)
poms=$(find "$REPO" -name '*.pom' | wc -l)
echo "repository now holds $jars jar(s), $poms pom(s)"

echo "=== modules without a pom ==="
missing=0
while IFS= read -r jar; do
	dir="$(dirname "$jar")"
	base="$(basename "$jar" .jar)"
	if [ ! -f "$dir/$base.pom" ]; then
		echo "  NO-POM $jar"
		missing=$((missing + 1))
	fi
done < <(find "$REPO" -name '*.jar')
if [ "$missing" -eq 0 ]; then
	echo "  (none)"
fi
