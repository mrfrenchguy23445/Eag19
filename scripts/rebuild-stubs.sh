#!/usr/bin/env bash
# Re-measure and re-apply the B1/B2 whole-method stubs after a generateWasmGC.
#
# Any generateWasmGC produces a fresh classes.wasm: it wipes the stubs AND shifts
# the code indices, so indices are never carried over from a previous run - they
# are always re-read from a fresh sweep of this exact artifact.
#
# usage: rebuild-stubs.sh [codeIdx:kind,codeIdx:kind,...]
#
# With no argument it only reports what the fresh build fails on and exits 1, so
# the stub kinds are chosen deliberately instead of guessed. With specs it stubs,
# re-sweeps to prove the result is clean, and runs the artifact gates.
#
# Exit is non-zero at the first sign of anything unexpected, per the standing
# rule that one surprise stops the chain rather than inviting the next guess.
set -eu

DIR="$(cd "$(dirname "$0")/.." && pwd)"
JS="$DIR/target_teavm_wasm_gc/javascript"
WASM="$JS/classes.wasm"
SWEEP="$DIR/scripts/wasm-error-sweep.mjs"
STUB="$DIR/scripts/stub-wasm-funcs.py"
UNSTUB=/tmp/opencode/classes-unstubbed.wasm
LOGDIR=/tmp/opencode
FRESH_LOG="$LOGDIR/sweep-fresh.log"
STUB_LOG="$LOGDIR/sweep-stubbed.log"

[ -f "$WASM" ] || { echo "no $WASM - run generateWasmGC first" >&2; exit 1; }
mkdir -p "$LOGDIR"

cp -f "$WASM" "$UNSTUB"
FRESH_SIZE=$(stat -c%s "$WASM")
echo "=== fresh build: size=$FRESH_SIZE sha1=$(sha1sum "$WASM" | cut -d' ' -f1) ==="

echo "=== sweep of the fresh (unstubbed) build ==="
node "$SWEEP" "$UNSTUB" "$LOGDIR/sweep-fresh.out.wasm" 500 700 | tee "$FRESH_LOG"
FRESH_FAILS=$(grep -c 'code#' "$FRESH_LOG" || true)
echo "fresh failing bodies: $FRESH_FAILS"
grep -E 'code#' "$FRESH_LOG" || true

if [ $# -eq 0 ]; then
	echo
	echo "No stub specs supplied. Failing code indices above; re-run as:"
	echo "  $0 <codeIdx:kind>[,<codeIdx:kind>...]"
	echo "  $0 auto          # derive kinds from the module's own type section"
	echo "(kind = i32 | none | trap; prefer i32/none - a trap kills the boot)"
	exit 1
fi

if [ "$1" = "auto" ]; then
	echo "=== deriving stub kinds from the module types ==="
	WTOOLS=/tmp/opencode/wasm-tools-1.231.0-x86_64-linux/wasm-tools
	WAT=/tmp/opencode/classes.wat
	[ -x "$WTOOLS" ] || { echo "no wasm-tools at $WTOOLS" >&2; exit 1; }
	"$WTOOLS" print "$UNSTUB" > "$WAT"
	IDS=$(grep -oE 'code#[0-9]+' "$FRESH_LOG" | sed 's/code#//' | sort -n | uniq | tr '\n' ' ')
	echo "failing code indices: $IDS"
	SPECS=$(python3 "$DIR/scripts/auto-stub-kind.py" "$WAT" $IDS | tr ',' ' ')
	echo "derived specs: $SPECS"
else
	SPECS=$(echo "$1" | tr ',' ' ')
fi
echo
echo "=== stubbing: $SPECS ==="
python3 "$STUB" "$UNSTUB" "$WASM" $SPECS

STUB_SIZE=$(stat -c%s "$WASM")
echo "stubbed size=$STUB_SIZE"
if [ "$STUB_SIZE" != "$FRESH_SIZE" ]; then
	echo "FATAL: size changed ($FRESH_SIZE -> $STUB_SIZE) - .teadbg offsets would shift" >&2
	exit 1
fi

echo "=== sweep of the stubbed build (must be clean) ==="
node "$SWEEP" "$WASM" "$LOGDIR/sweep-stubbed.out.wasm" 500 700 | tee "$STUB_LOG"
if ! grep -q 'status=clean masked=0' "$STUB_LOG"; then
	echo "FATAL: stubbed build is not clean - STOP, do not chain a fix onto this" >&2
	exit 1
fi

echo "=== gates ==="
python3 "$DIR/scripts/find-bad-classinfo.py" "$WASM"
python3 "$DIR/scripts/check-wasm-imports.py" "$WASM" "$JS/eagruntime.js" "$JS/index.html" \
	&& echo "check-wasm-imports: exit 0"

echo "=== installed ==="
echo "size=$STUB_SIZE sha1=$(sha1sum "$WASM" | cut -d' ' -f1)"
echo "unstubbed backup: $UNSTUB (sha1=$(sha1sum "$UNSTUB" | cut -d' ' -f1))"
