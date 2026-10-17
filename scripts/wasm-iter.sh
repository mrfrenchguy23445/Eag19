#!/usr/bin/env bash
# One command = one full iteration of the wasm-error fix loop:
#
#   build  ->  re-stub (auto)  ->  boot  ->  triage
#
# Each stage fails closed and prints its own diagnosis, so a run that goes wrong
# tells you where and why instead of leaving a stale artifact behind.
#
# usage: wasm-iter.sh [BUDGET_MS]
set -eu

DIR="$(cd "$(dirname "$0")/.." && pwd)"
LOGDIR=/tmp/opencode
BUILD_LOG="$LOGDIR/build.log"
BOOT_LOG="$LOGDIR/boot.log"
BUDGET_MS="${1:-300000}"

mkdir -p "$LOGDIR"

# generateWasmGC only transpiles the Java to classes.wasm; the JS runtime
# (javascript/eagruntime.js) is closure-compiled by a SEPARATE task,
# compileMainWasmRuntime, which generateWasmGC does NOT depend on. Without it a
# change under src/wasm-gc-teavm/js/ never reaches the served bundle, and a new
# wasm import (e.g. platformOpenGL.glVertexAttribIPointer) fails at
# WebAssembly.instantiate with "function import requires a callable". Run both.
echo "############ 1/4 BUILD ############"
if ! JAVA_HOME="$HOME/.local/jdk17" ./gradlew \
		:target_teavm_wasm_gc:generateWasmGC \
		:target_teavm_wasm_gc:compileMainWasmRuntime \
		--console=plain > "$BUILD_LOG" 2>&1; then
	echo "BUILD FAILED - last 40 lines:"
	tail -40 "$BUILD_LOG"
	exit 1
fi
grep -E "BUILD (SUCCESSFUL|FAILED)" "$BUILD_LOG" | tail -1

echo
echo "############ 2/4 STUB ############"
"$DIR/scripts/rebuild-stubs.sh" auto

echo
echo "############ 3/4 BOOT (BUDGET_MS=$BUDGET_MS) ############"
BUDGET_MS="$BUDGET_MS" "$DIR/scripts/boot-title-screen.sh" > "$BOOT_LOG" 2>&1 || true

echo
echo "############ 4/4 TRIAGE ############"
python3 "$DIR/scripts/triage.py" "$BOOT_LOG"

echo
echo "boot log : $BOOT_LOG"
echo "screenshot: ${SHOT:-/tmp/opencode/title.png}"
