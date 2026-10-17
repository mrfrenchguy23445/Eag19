#!/bin/sh
# Serve the wasm_gc build directory over HTTP and load it in headless Chromium far
# enough to take a screenshot, so "boots to the title screen" is a measurable claim
# instead of an eyeball judgement.
#
# Why HTTP: index.html refuses file: outright ("run a local HTTP server").
# Why Chromium: the page needs JSPI (WebAssembly.Suspending); Chromium is the
# browser that has it here, and #17 of index.html alerts instead of booting if it
# is missing.
#
# Usage:
#   scripts/boot-title-screen.sh              # serve + screenshot to /tmp/opencode/title.png
#   scripts/boot-title-screen.sh --serve-only # just run the server in the foreground
set -eu

DIR="$(cd "$(dirname "$0")/.." && pwd)/target_teavm_wasm_gc/javascript"
PORT="${PORT:-8097}"
SHOT="${SHOT:-/tmp/opencode/title.png}"
BUDGET_MS="${BUDGET_MS:-45000}"

[ -f "$DIR/classes.wasm" ] || { echo "no $DIR/classes.wasm - run generateWasmGC first" >&2; exit 1; }
[ -f "$DIR/index.html" ] || { echo "no $DIR/index.html" >&2; exit 1; }

if [ "${1:-}" = "--serve-only" ]; then
	echo "serving $DIR on http://127.0.0.1:$PORT/"
	exec python3 -m http.server "$PORT" --bind 127.0.0.1 --directory "$DIR"
fi

mkdir -p "$(dirname "$SHOT")"

# eagruntime.js gates startup on navigator.userActivation.hasBeenActive; when that is
# false it overlays pressAnyKey.png and waits for a keydown/mousedown/touchstart on
# window/document.body (the y() handler above the overlay). A headless screenshot has no
# real user, so we serve a copy of index.html with an auto-key script appended that fires
# synthetic keydowns into that same window. Nothing else about the page is altered.
AUTO="$DIR/autokey.html"
{
	cat "$DIR/index.html"
	cat <<'EOF'
<script type="text/javascript">
	(function() {
		var sent = 0;
		var timer = setInterval(function() {
			try {
				window.dispatchEvent(new KeyboardEvent("keydown", {
					key: "a", code: "KeyA", keyCode: 65, bubbles: true
				}));
			} catch (e) {}
			if (++sent >= 40) clearInterval(timer);   // ~12s, then leave the game alone
		}, 300);
	})();
</script>
EOF
} > "$AUTO"

# Exactly one server, started after AUTO exists: a second `python3 -m http.server` on the
# same port fails to bind but still succeeds as a backgrounded command, which silently
# replaced $SRV with a dead pid and leaked the live one past the EXIT trap.
python3 -m http.server "$PORT" --bind 127.0.0.1 --directory "$DIR" &
SRV=$!
trap 'kill "$SRV" 2>/dev/null || true; rm -f "$AUTO"' EXIT
sleep 1

# --headless=new, JSPI explicitly allowed, long virtual time budget so the wasm
# finishes starting before the shot. Server-side console output goes to $SHOT.log.
# not exec: the EXIT trap removes the generated autokey.html
/usr/bin/chromium \
	--headless=new \
	--no-sandbox \
	--disable-gpu \
	--enable-unsafe-webassembly \
	--autoplay-policy=no-user-gesture-required \
	--window-size=1280,720 \
	--virtual-time-budget="$BUDGET_MS" \
	--enable-logging=stderr --v=0 \
	--screenshot="$SHOT" \
	"http://127.0.0.1:$PORT/autokey.html"
