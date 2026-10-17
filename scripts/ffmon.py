#!/usr/bin/env python3
"""
ffmon.py - headless-Firefox monitor for the wasm-gc build.

Runs a real Firefox (geckodriver) under xvfb against a copy of the served build,
presses a synthetic key (autokey.html) to clear the press-any-key overlay, and
every INTERVAL seconds takes a viewport screenshot, classifies the dominant
color (the old "solid red #EF323D" signature) and records it. The browser's
console output (which carries EagRuntimeJS lines AND our [diag] System.out logs)
is captured to the geckodriver log for later grepping.

Usage:
    xvfb-run -a python3 scripts/ffmon.py [DURATION_S] [INTERVAL_S] [URL]

Serves a COPY of target_teavm_wasm_gc/javascript on 127.0.0.1:8097 (so the
user-facing server directory on :8080 is never touched), generates autokey.html
in that copy, and launches geckodriver against it.
"""
import base64
import json
import os
import shutil
import signal
import subprocess
import sys
import tempfile
import time
import urllib.request
from collections import Counter

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BUILD = os.path.join(ROOT, "target_teavm_wasm_gc", "javascript")
LOGDIR = "/tmp/opencode"
DURATION = int(sys.argv[1]) if len(sys.argv) > 1 else 420
INTERVAL = int(sys.argv[2]) if len(sys.argv) > 2 else 15
URL = sys.argv[3] if len(sys.argv) > 3 else "http://127.0.0.1:8097/autokey.html"
PORT = 8097
GD_PORT = 4444
AUTOKEY_JS = """
    (function() {
        var sent = 0;
        var timer = setInterval(function() {
            try {
                window.dispatchEvent(new KeyboardEvent("keydown", {
                    key: "a", code: "KeyA", keyCode: 65, bubbles: true
                }));
            } catch (e) {}
            if (++sent >= 40) clearInterval(timer);
        }, 300);
    })();
"""

os.makedirs(LOGDIR, exist_ok=True)


def http(method, url, body=None, headers=None):
    req = urllib.request.Request(url, data=body, headers=headers or {}, method=method)
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            return json.loads(resp.read().decode())
    except urllib.error.HTTPError as e:
        try:
            return json.loads(e.read().decode())
        except Exception:
            return {"error": str(e)}


def main():
    if not os.path.isfile(os.path.join(BUILD, "index.html")):
        print(f"no {BUILD}/index.html - run generateWasmGC first", file=sys.stderr)
        sys.exit(1)

    # 1) a work copy of the build + autokey.html, served on :PORT
    work = tempfile.mkdtemp(prefix="ffmon-serve-")
    for f in os.listdir(BUILD):
        src = os.path.join(BUILD, f)
        if os.path.isfile(src):
            shutil.copy2(src, os.path.join(work, f))
    with open(os.path.join(BUILD, "index.html")) as fh:
        idx = fh.read()
    with open(os.path.join(work, "autokey.html"), "w") as fh:
        fh.write(idx + "\n<script type=\"text/javascript\">" + AUTOKEY_JS + "\n</script>\n")

    srv = subprocess.Popen(
        ["python3", "-m", "http.server", str(PORT), "--bind", "127.0.0.1", "--directory", work],
        stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
    )
    time.sleep(1)

    # 2) geckodriver - log everything so console messages land in the log
    gecko_log = os.path.join(LOGDIR, "ffmon_gecko.log")
    gd = subprocess.Popen(
        ["geckodriver", "--port", str(GD_PORT), "--log", "debug"],
        stdout=open(gecko_log, "ab", buffering=0),
        stderr=subprocess.STDOUT,
    )
    time.sleep(2)

    procs = [srv, gd]

    def shutdown(*_):
        for p in reversed(procs):
            try:
                p.terminate()
            except Exception:
                pass
        time.sleep(1)
        for p in reversed(procs):
            try:
                p.kill()
            except Exception:
                pass
        sys.exit(0)

    signal.signal(signal.SIGTERM, shutdown)

    try:
        # 3) WebDriver session with console->stdout prefs (JSPI needs real Firefox)
        caps = {
            "capabilities": {
                "alwaysMatch": {
                    "moz:firefoxOptions": {
                        "args": ["-no-remote"],
                        "prefs": {
                            "devtools.console.stdout.content": True,
                            "devtools.console.content.enabled": True,
                            "devtools.console.stdout.chrome": True,
                            "browser.warnOnQuit": False,
                            "media.autoplay.default": 0,
                            "webgl.force-enabled": True,
                            "javascript.options.wasm_js_promise_integration": True,
                            "javascript.options.wasm_exceptions": True,
                        },
                    },
                }
            }
        }
        sess = http("POST", f"http://127.0.0.1:{GD_PORT}/session", json.dumps(caps).encode(),
                    {"Content-Type": "application/json"})
        sid = sess.get("value", {}).get("sessionId")
        if not sid:
            print("no session:", sess, file=sys.stderr)
            shutdown()
        base = f"http://127.0.0.1:{GD_PORT}/session/{sid}"

        def cmd(method, path, body=None):
            return http(method, base + path, json.dumps(body).encode() if body is not None else None,
                        {"Content-Type": "application/json"})

        cmd("POST", "/window/rect", {"width": 1280, "height": 720})
        cmd("POST", "/url", {"url": URL})
        print("navigated", flush=True)

        start = time.time()
        try:
            from PIL import Image
            import io
            use_pil = True
        except Exception:
            use_pil = False

        i = 0
        while time.time() - start < DURATION:
            t0 = time.time()
            shot = None
            try:
                r = cmd("GET", "/screenshot")
                v = r.get("value")
                if isinstance(v, dict):
                    v = v.get("value")  # some geckodriver versions double-wrap
                if not isinstance(v, str):
                    raise TypeError(f"unexpected screenshot payload type {type(v).__name__}: {str(v)[:80]}")
                if use_pil:
                    im = Image.open(io.BytesIO(base64.b64decode(v))).convert("RGB")
                    px = list(im.getdata())
                    c = Counter(px)
                    (color, count), = c.most_common(1)
                    frac = count / len(px) * 100
                    distinct = len(c)
                    hexc = "#{:02X}{:02X}{:02X}".format(color[0], color[1], color[2])
                    probe = f"OK({int((time.time() - t0) * 1000)}ms)"
                else:
                    im, hexc, frac, distinct, probe = (None, "#??????", 0, 0, "noPIL")
                secs = int(time.time() - start)
                w = im.width if im else 0
                h = im.height if im else 0
                print(f"[{secs:5d}s] screen={hexc} {frac:.0f}% ({w}x{h}, distinct={distinct})  probe={probe}", flush=True)
                if im:
                    im.save(os.path.join(LOGDIR, f"mon_{i:04d}.png"))
            except Exception as e:
                print(f"[{int(time.time() - start):5d}s] screenshot failed: {e!r}", flush=True)
            i += 1
            time.sleep(max(1, INTERVAL - (time.time() - t0)))

        try:
            cmd("DELETE", "")
        except Exception:
            pass
    finally:
        shutdown()


if __name__ == "__main__":
    main()