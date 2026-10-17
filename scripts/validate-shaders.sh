#!/usr/bin/env bash
# Compile every vanilla core shader offline, under the same WebGL2 / GLSL ES
# 3.00 compiler the game uses, so desktop-GLSL-only constructs are caught
# without a 9-minute generateWasmGC + boot cycle.
#
# Exits non-zero if any shader fails to compile. Prints the failing shaders and
# their info logs.
#
# This mirrors PlatformOpenGL.translateVanillaGlsl and GlslPreprocessor.process
# in JVM-side scripts/glsl-assemble.py; see that file's docstring for the
# keep-in-sync warning.
#
# usage: scripts/validate-shaders.sh [--root <shaders dir>]
set -eu

DIR="$(cd "$(dirname "$0")/.." && pwd)"
OUT=/tmp/opencode/glslcheck
CHROMIUM="${CHROMIUM:-/usr/bin/chromium}"

python3 "$DIR/scripts/glsl-assemble.py" --out "$OUT" "$@"

"$CHROMIUM" \
	--headless=new \
	--no-sandbox \
	--disable-gpu \
	--enable-unsafe-webassembly \
	--enable-unsafe-swiftshader \
	--virtual-time-budget=30000 \
	--dump-dom "file://$OUT/index.html" > "$OUT/dump.html" 2> "$OUT/chrome.err"

RESULT="$(grep -a -oE 'GLSLCHECK_RESULT total=[0-9]+ failed=[0-9]+( ALL_OK)?' "$OUT/dump.html" | head -1)"
if [ -z "$RESULT" ]; then
	echo "validate-shaders: no result marker in $OUT/dump.html (see $OUT/chrome.err)" >&2
	exit 1
fi
echo "$RESULT"

FAILED="$(echo "$RESULT" | sed -E 's/.*failed=([0-9]+).*/\1/')"
if [ "$FAILED" != "0" ]; then
	echo
	grep -a -oE 'FAIL shaders/[^ ]*' "$OUT/dump.html" || true
	echo
	echo "first failure detail:"
	python3 - "$OUT/dump.html" <<'EOF'
import re, sys
t = open(sys.argv[1], 'rb').read().decode('utf-8', 'replace')
m = re.search(r'GLSLCHECK_RESULT[^\n<]*\n(.*?)</pre>', t, re.S)
print(m.group(1).strip()[:2000] if m else 'no detail')
EOF
	exit 1
fi
