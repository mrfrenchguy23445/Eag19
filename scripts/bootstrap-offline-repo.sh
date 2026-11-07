#!/usr/bin/env bash
# Fill gradle/local-libs until an offline build resolves completely.
#
# Some of what a build needs is invisible to dumpOfflineDeps: the build plugins'
# own dependency closure (org.teavm:teavm-gradle-plugin, the eaglercraft build
# plugin's Kotlin runtime, xz, parent POMs) is resolved by plugin management, not
# by any project configuration. Rather than reimplementing POM traversal, this
# runs the real offline build, reads the modules Gradle says it cannot find, adds
# them, and repeats until the build stops complaining.
#
# usage: scripts/bootstrap-offline-repo.sh <gradle-launcher> <empty-gradle-home> [tasks...]
#
# <empty-gradle-home> must be EMPTY (or at least not contain the module cache):
# a warm cache hides exactly the gaps this is looking for.
set -euo pipefail

DIR="$(cd "$(dirname "$0")/.." && pwd)"
GRADLE_BIN="${1:-}"
GRADLE_HOME_ARG="${2:-}"
shift 2 || { echo "usage: $0 <gradle-launcher> <empty-gradle-home> [tasks...]" >&2; exit 2; }
TASKS=("$@")
[ ${#TASKS[@]} -eq 0 ] && TASKS=(:jdk-shims:jar :target_teavm_javascript:compileJava :target_teavm_wasm_gc:compileJava)

LOG="$(mktemp)"
trap 'rm -f "$LOG"' EXIT

for round in $(seq 1 12); do
	echo "=== round $round ==="
	set +e
	"$GRADLE_BIN" --offline --console=plain --no-daemon -g "$GRADLE_HOME_ARG" \
		-p "$DIR" "${TASKS[@]}" > "$LOG" 2>&1
	status=$?
	set -e

	if [ $status -eq 0 ]; then
		echo "offline build succeeded"
		exit 0
	fi

	missing=$(grep -oE 'Could not find [^ ]+\.' "$LOG" | sed 's/^Could not find //; s/\.$//' | sort -u || true)
	if [ -z "$missing" ]; then
		echo "build failed, but not on a missing module:"
		tail -40 "$LOG"
		exit 1
	fi

	echo "missing modules:"
	echo "$missing" | sed 's/^/  /'
	while IFS= read -r coord; do
		group="${coord%%:*}"
		rest="${coord#*:}"
		artifact="${rest%%:*}"
		version="${rest#*:}"
		bash "$DIR/scripts/fetch-offline-module.sh" "$group|$artifact|$version|"
	done <<< "$missing"
done

echo "still unresolved after 12 rounds; see the last build output" >&2
exit 1
