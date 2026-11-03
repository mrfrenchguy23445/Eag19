#!/usr/bin/env python3
"""console_tail.py - tail a live Chrome tab's console from the terminal.

Streams Runtime.consoleAPICalled / Log.entryAdded events (the game's Java
[console/INFO] lines arrive through console.*) and prints them as they happen.

Works over the DevTools Protocol against a Chrome started with a debug port:

    google-chrome --remote-debugging-port=9222 "http://127.0.0.1:8097/"
    python3 scripts/console_tail.py                     # port defaults to 9222
    python3 scripts/console_tail.py --port 9222         # same thing, explicit
    python3 scripts/console_tail.py --grep 'console/INFO|Spawn'   # filter line

Notes:
  * Chrome must be LAUNCHED with the flag — an already-running instance cannot
    get the port retro-fitted (Chrome ignores --remote-debugging-port when it
    joins an existing process).
  * The script picks the target tab automatically: a page on the game URL it
    knows, else the first real page.
"""
import argparse
import json
import signal
import sys
import time
import urllib.request

try:
    from websocket import create_connection  # websocket-client
except ImportError:
    raise SystemExit("needs websocket-client: pip install websocket-client")


def parse_args():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--port", type=int, default=9222, help="CDP port (default 9222)")
    ap.add_argument("--host", default="127.0.0.1", help="CDP host (default 127.0.0.1)")
    ap.add_argument("--grep", default="", help="regex filter on the formatted line (case-insensitive)")
    ap.add_argument("--list", action="store_true", help="list debuggable tabs and exit")
    return ap.parse_args()


def devtools_ws(args):
    req = urllib.request.urlopen("http://%s:%d/json" % (args.host, args.port), timeout=5)
    targets = json.load(req)
    pages = [t for t in targets if t.get("type") == "page"]
    if args.list:
        for t in targets:
            print("%s  %s  %s" % (t.get("type"), t.get("title"), t.get("url")))
        raise SystemExit(0)
    if not pages:
        raise SystemExit("no debuggable page tabs on port %d" % args.port)
    # prefer the game page
    for t in pages:
        if "127.0.0.1:8097" in t.get("url", ""):
            return t
    return pages[0]


def fmt(args):
    return " ".join(str(a) for a in args if a is not None)


def main():
    args = parse_args()
    target = devtools_ws(args)
    ws = create_connection(target["webSocketDebuggerUrl"], timeout=10)
    for method in ("Runtime.enable", "Log.enable", "Runtime.setKeepCommandLineFunctions"):
        ws.send(json.dumps({"id": 1, "method": method}))
    print("tailing console of: %s  %s  (ctrl-c to stop)" % (target["title"], target["url"]))
    if args.grep:
        print("filter: %r" % args.grep)
    sys.stdout.flush()
    rx = None
    if args.grep:
        import re
        rx = re.compile(args.grep, re.I)

    def finish(*_):
        sys.exit(0)
    signal.signal(signal.SIGINT, finish)

    while True:
        try:
            raw = ws.recv()
        except Exception as e:
            print("connection lost: %s" % e)
            return 1
        if not raw:
            continue
        try:
            msg = json.loads(raw)
        except ValueError:
            continue
        m = msg.get("method", "")
        p = msg.get("params", {})
        if m == "Runtime.consoleAPICalled":
            lvl = p.get("type", "log")
            if lvl in ("log", "info"):
                lvl = "INFO"
            elif lvl in ("error", "assert"):
                lvl = "ERROR"
            elif lvl == "warning":
                lvl = "WARN"
            else:
                lvl = lvl.upper()
            line = "[console/%s] %s" % (lvl, fmt(p.get("args", [])))
        elif m == "Log.entryAdded":
            e = p.get("entry", {})
            line = "[%s] %s %s" % (e.get("level", "?").upper(), e.get("source", ""), e.get("text", ""))
        elif m == "Runtime.exceptionThrown":
            e = p.get("exceptionDetails", {})
            line = "[EXC] %s" % (e.get("exception", {}).get("description") or e.get("text", ""))
        else:
            continue
        if rx is None or rx.search(line):
            ts = time.strftime("%H:%M:%S")
            print("%s  %s" % (ts, line[:400]))
            sys.stdout.flush()


if __name__ == "__main__":
    sys.exit(main() or 0)
