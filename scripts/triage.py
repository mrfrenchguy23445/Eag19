#!/usr/bin/env python3
"""Turn a boot log into a diagnosis in one command.

The slow part of fixing wasm errors has never been the fix, it has been the
reading: crash reports are wrapped in CONSOLE quoting, traces are 300 copies of
one frame, and the top frame always names a file:line you then have to go find
by hand. This does the mechanical part:

  * unwraps the CONSOLE quoting and prints the exception
  * collapses runs of identical frames (a stack overflow is 339 of one frame)
  * picks the deepest frame that is actually project code, resolves the
    file:line on disk and prints the source
  * classifies the failure: wasm validation vs Java exception vs stack
    overflow vs missing import, and for a wasm validation error maps the code
    index to the Java method through .teadbg automatically

usage: triage.py [boot.log] [classes.wasm.teadbg]
       (defaults: /tmp/opencode/boot-4.log, the wasm-gc teadbg)
"""
import os
import re
import subprocess
import sys

DEFAULT_LOG = "/tmp/opencode/boot-4.log"
HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
DEFAULT_TDBG = os.path.join(ROOT, "target_teavm_wasm_gc/javascript/classes.wasm.teadbg")

# frames we never consider "project code" - they are TeaVM/classlib plumbing
SKIP_PKGS = ("java.lang.", "java.util.", "org.teavm.", "sun.", "jdk.")


def unwrap(log):
    """Pull the quoted payload out of Chromium CONSOLE lines."""
    out = []
    for raw in log.splitlines():
        m = re.search(r'INFO:CONSOLE:\d+\] "(.*)", source:', raw)
        if m:
            out.append(m.group(1))
        else:
            out.append(raw)
    return out


def frames(lines):
    fs = []
    for ln in lines:
        m = re.match(r"\s+at (.+?)(\((.*?)\))?$", ln)
        if m:
            fs.append((m.group(1).strip(), m.group(3) or ""))
    return fs


def collapse(fs):
    """[(frame, count)] keeping order, merging runs of identical frames."""
    out = []
    for f in fs:
        if out and out[-1][0] == f:
            out[-1][1] += 1
        else:
            out.append([f, 1])
    return out


def find_source(fname):
    """Locate a source file by name anywhere under src/ or jdk-shims/."""
    for root in ("src", "jdk-shims"):
        for dirpath, dirnames, filenames in os.walk(os.path.join(ROOT, root)):
            if "/build/" in dirpath:
                continue
            if fname in filenames:
                return os.path.join(dirpath, fname)
    return None


def show_source(path, lineno, ctx=4):
    try:
        with open(path, encoding="utf-8", errors="replace") as fh:
            src = fh.readlines()
    except OSError:
        return []
    lo = max(1, lineno - ctx)
    hi = min(len(src), lineno + ctx)
    out = []
    for i in range(lo, hi + 1):
        mark = ">>" if i == lineno else "  "
        out.append(f"  {mark} {i:5d} | {src[i - 1].rstrip()}")
    return out


def teadbg_name(tdbg, addr):
    """Which Java method owns this module byte offset?"""
    try:
        r = subprocess.run(
            [sys.executable, os.path.join(HERE, "decode-teadbg.py"), tdbg, str(addr)],
            capture_output=True, text=True, timeout=300,
        )
    except Exception:
        return None
    for line in r.stdout.splitlines():
        if "raw:" in line:
            return line.split("raw:", 1)[1].strip()
    return None


def main():
    logpath = sys.argv[1] if len(sys.argv) > 1 else DEFAULT_LOG
    tdbg = sys.argv[2] if len(sys.argv) > 2 else DEFAULT_TDBG
    if not os.path.exists(logpath):
        sys.exit(f"no such log: {logpath}")
    with open(logpath, encoding="utf-8", errors="replace") as fh:
        raw = fh.read()

    lines = unwrap(raw)
    joined = "\n".join(lines)
    fs = frames(lines)
    col = collapse(fs)

    print("=" * 72)
    print(f"LOG: {logpath}  ({len(raw.splitlines())} lines)")
    print("=" * 72)

    # ---- classify -------------------------------------------------------
    kind = "unknown"
    detail = ""

    v8 = re.search(r"Compiling function #(\d+) failed: (.*?) @\+(\d+)", joined)
    if v8:
        kind = "WASM VALIDATION ERROR"
        detail = f"fn#{v8.group(1)}  offset={v8.group(3)}\n  {v8.group(2)}"
    elif "Maximum call stack size exceeded" in joined:
        kind = "STACK OVERFLOW (infinite recursion)"
    elif re.search(r"Could not load required resource", joined):
        m = re.search(r"Could not load required resource from EPK: (\S+)", joined)
        kind = "MISSING EPK RESOURCE"
        detail = m.group(0) if m else ""
    elif re.search(r'CONSOLE:\d+\] "java\.', raw) or re.search(r"^\s+at ", joined, re.M):
        kind = "JAVA EXCEPTION"

    exc = None
    for ln in lines:
        m = re.match(r'^(java|com|net|org)[\w.$]+(?:Exception|Error)(?::\s*.*)?$', ln.strip())
        if m:
            exc = ln.strip()
            break
    if exc is None:
        for ln in lines:
            if ln.strip().startswith(("java.", "Native Browser")):
                exc = ln.strip()
                break

    print(f"\nCLASS : {kind}")
    if detail:
        print(f"DETAIL: {detail}")
    if exc:
        print(f"CAUSE : {exc}")

    # ---- browser-side type/desc block -----------------------------------
    t = re.search(r"Type:\s*(\S+).*?Desc:\s*(.+)", joined, re.S)
    if t:
        print(f"BROWSER: {t.group(1)} - {t.group(2).strip().splitlines()[0]}")

    # ---- trace ----------------------------------------------------------
    if col:
        print("\nTRACE (identical frames collapsed):")
        for (f, src), n in col:
            suffix = f"   x{n}" if n > 1 else ""
            print(f"    at {f}{suffix}")

    # ---- resolve the deepest project frame ------------------------------
    # frames are printed innermost-first, so the FIRST project frame with a
    # real file:line is the one that actually threw.
    hasline = lambda s: bool(re.search(r":\d+$", s)) and "Unknown Source" not in s
    target = None
    for f, src in fs:
        if not hasline(src) or f.startswith(SKIP_PKGS):
            continue
        target = (f, src)
        break
    if target is None:
        for f, src in reversed(fs):
            if hasline(src):
                target = (f, src)
                break

    if target:
        f, src = target
        m = re.match(r"(.*?):(\d+)$", src)
        if m:
            fname, lineno = m.group(1), int(m.group(2))
            path = find_source(fname)
            print("\n" + "-" * 72)
            print(f"BOTTOM PROJECT FRAME: {f}")
            print(f"  {src}")
            if path:
                print(f"  resolved -> {os.path.relpath(path, ROOT)}:{lineno}")
                body = show_source(path, lineno)
                if body:
                    print("  " + "-" * 66)
                    print("\n".join(body))
            else:
                print(f"  (source {fname} not found under src/ or jdk-shims/)")

    # ---- map a wasm validation error to a Java method -------------------
    if v8:
        print("\n" + "-" * 72)
        print("FUNCTION OWNERSHIP (via .teadbg):")
        code_idx = int(re.search(r"fn#(\d+)", detail).group(1)) - 199
        print(f"  codeIdx = {code_idx}   funcIdx = {code_idx + 199}")
        if os.path.exists(tdbg):
            off = int(v8.group(3))
            for cand in (off, off - 1):
                nm = teadbg_name(tdbg, cand)
                if nm:
                    print(f"  offset {cand} -> {nm}")
                    break
        else:
            print(f"  (no teadbg at {tdbg})")

    print("\n" + "=" * 72)


if __name__ == "__main__":
    main()
