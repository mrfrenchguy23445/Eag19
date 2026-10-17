#!/usr/bin/env python3
"""
chrome_mon.py - headless-Chrome (CDP) monitor for the wasm-gc build.

Same idea as ffmon.py but drives the extracted Chrome 155 via the DevTools
Protocol directly (no chromedriver => no version-matching problem). Captures
the page console (including the [pixprobe] lines) and takes periodic
color-classified screenshots.

Usage:
    python3 scripts/chrome_mon.py [DURATION_S] [INTERVAL_S] [URL]
"""
import base64
import io
import json
import os
import shutil
import signal
import socket
import subprocess
import sys
import tempfile
import time
import urllib.request
from collections import Counter

from websocket import create_connection

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BUILD = os.path.join(ROOT, "target_teavm_wasm_gc", "javascript")
LOGDIR = "/tmp/opencode"
DURATION = int(sys.argv[1]) if len(sys.argv) > 1 else 420
INTERVAL = int(sys.argv[2]) if len(sys.argv) > 2 else 10
PORT = 8098
CDP_PORT = 9223
CHROME = "/home/nalin/opt/google-chrome/opt/google/chrome/chrome"
URL = None
if len(sys.argv) > 3:
    URL = sys.argv[3]
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


def classify(buf_b64):
    from PIL import Image
    im = Image.open(io.BytesIO(base64.b64decode(buf_b64))).convert("RGB")
    im = im.resize((160, 100))
    colors = Counter(im.getdata())
    top = colors.most_common(1)[0]
    return "#%02X%02X%02X" % top[0], 100.0 * top[1] / (160 * 100), im.width, im.height, len(colors)


def main():
    if not os.path.isfile(os.path.join(BUILD, "index.html")):
        print("no %s/index.html - run generateWasmGC first" % BUILD, file=sys.stderr)
        sys.exit(1)

    # clean up any previous headless-chrome instances (ours use /tmp/chrome-prof-*)
    subprocess.run(["pkill", "-f", "user-data-dir=/tmp/chrome-prof-"],
                   stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    time.sleep(1)

    global PORT, URL
    if URL is None:
        # pick a free port so a zombie http.server can never hijack us
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.bind(("127.0.0.1", 0))
        PORT = s.getsockname()[1]
        s.close()
        URL = "http://127.0.0.1:%d/autokey.html" % PORT

    work = tempfile.mkdtemp(prefix="chrome-serve-")
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
    print("srv started on port", PORT, flush=True)
    time.sleep(0.5)
    try:
        srv.poll()
        if srv.returncode is not None:
            print("http.server DIED rc=%s" % srv.returncode, file=sys.stderr, flush=True)
    except Exception:
        pass
    profile = tempfile.mkdtemp(prefix="chrome-prof-")
    chrome_log = os.path.join(LOGDIR, "chrome_headless_stderr.log")
    chrome = subprocess.Popen(
        [
            CHROME,
            "--headless=new", "--no-sandbox", "--disable-dev-shm-usage",
            "--enable-features=WebAssemblyJSPI,WebAssemblyJsPromiseIntegration",
            "--use-gl=angle", "--use-angle=swiftshader", "--enable-unsafe-swiftshader",
            "--remote-debugging-port=%d" % CDP_PORT,
            "--remote-allow-origins=*",
            "--user-data-dir=" + profile,
            "--window-size=1280,720", "--hide-scrollbars",
            "about:blank",
        ],
        stdout=subprocess.DEVNULL, stderr=open(chrome_log, "ab", buffering=0),
    )
    time.sleep(3)
    try:
        _run(chrome, srv, profile, work)
    except BaseException:
        cleanup(chrome, srv, profile)
        raise


def _run(chrome, srv, profile, work):
    def get_json(u):
        with urllib.request.urlopen(u, timeout=5) as r:
            return json.loads(r.read().decode())

    tabs = []
    for _ in range(20):
        try:
            tabs = get_json("http://127.0.0.1:%d/json/list" % CDP_PORT)
            if tabs:
                break
        except Exception:
            pass
        time.sleep(1)
    print("tabs found:", len(tabs), flush=True)
    page = next((t for t in tabs if t.get("type") == "page"), None)
    if not page:
        print("no debug target", file=sys.stderr)
        cleanup(chrome, srv, profile)
        sys.exit(1)

    ws = create_connection(page["webSocketDebuggerUrl"], timeout=15)
    print("ws connected", flush=True)
    msg_id = 0

    def send(method, params=None):
        nonlocal msg_id
        msg_id += 1
        ws.send(json.dumps({"id": msg_id, "method": method, "params": params or {}}))
        return msg_id

    # fire the domain enables; we consume responses as ordinary messages below
    send("Runtime.enable")
    send("Page.enable")
    send("Log.enable")
    print("CDP domains enabled", flush=True)

    console_log = os.path.join(LOGDIR, "chrome_console.log")
    cl = open(console_log, "ab", buffering=0)
    pix = 0
    send("Page.navigate", {"url": URL})
    print("navigated to %s; entering loop" % URL, flush=True)

    start = time.time()
    next_shot = 0.0
    shot_id = None
    shot_sent_at = 0.0
    ws.settimeout(1.0)
    while time.time() - start < DURATION:
        try:
            m = json.loads(ws.recv())
        except Exception:
            m = None
        if m is not None:
            if "method" in m:
                # a CDP event
                if m["method"] == "Runtime.consoleAPICalled":
                    try:
                        parts = []
                        for a in m["params"]["args"]:
                            v = a.get("value")
                            parts.append(v if isinstance(v, str) else json.dumps(v))
                        txt = " ".join(parts)
                        cl.write(txt.encode("utf-8", "replace") + b"\n")
                        cl.flush()
                        if "pixprobe" in txt or "glprobe" in txt:
                            pix += 1
                            print("[console] " + txt, flush=True)
                    except Exception:
                        pass
            elif m.get("id") == shot_id and "result" in m:
                # our screenshot came back
                try:
                    b64 = m["result"].get("data", "")
                    hexc, pct, w, h, distinct = classify(b64)
                    print("[%5ds] screen=%s %.0f%% (%dx%d, distinct=%d) probes=%d"
                          % (int(time.time() - start), hexc, pct, w, h, distinct, pix), flush=True)
                except Exception as e:
                    print("[%5ds] classify failed: %s" % (int(time.time() - start), e), flush=True)
                shot_id = None
        now = time.time()
        if now - start >= next_shot:
            next_shot = now - start + INTERVAL
            if shot_id is None or now - shot_sent_at > INTERVAL * 3:
                shot_id = send("Page.captureScreenshot", {"format": "png"})
                shot_sent_at = now
                print("[%5ds] requested screenshot" % int(now - start), flush=True)

    cl.close()
    ws.close()
    cleanup(chrome, srv, profile)
    print("done; serve copy left at %s" % work)


def cleanup(chrome, srv, profile):
    for p in (chrome, srv):
        try:
            p.terminate()
        except Exception:
            pass
    time.sleep(1)
    for p in (chrome, srv):
        try:
            p.kill()
        except Exception:
            pass
    try:
        shutil.rmtree(profile, ignore_errors=True)
    except Exception:
        pass


if __name__ == "__main__":
    main()