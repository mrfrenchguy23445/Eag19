#!/usr/bin/env python3
"""One-pass link check: diff every import the wasm declares against what
eagruntime.js actually provides.

WebAssembly.instantiate resolves imports by exact module/name string, and V8
reports only the FIRST missing one (a LinkError aborts the whole link), so
checking them one boot at a time would mean one rebuild per mismatch. This walks
the import section once and reports every unresolved import together.

usage: check-wasm-imports.py <classes.wasm> <eagruntime.js> [<index.html> ...]
exit 1 if any declared import is not provided.
"""
import sys


def read_leb(buf, off):
    r = s = 0
    while True:
        b = buf[off]
        off += 1
        r |= (b & 0x7F) << s
        s += 7
        if not (b & 0x80):
            return r, off


def read_name(buf, off):
    n, off = read_leb(buf, off)
    return buf[off:off + n].decode("utf-8", "replace"), off + n


def skip_limits(buf, off):
    flags, off = read_leb(buf, off)
    _, off = read_leb(buf, off)
    if flags & 1:
        _, off = read_leb(buf, off)
    return off


def imports(path):
    buf = open(path, "rb").read()
    if buf[:4] != b"\x00asm":
        raise SystemExit("not a wasm module: " + path)
    off = 8
    out = []
    while off < len(buf):
        sid = buf[off]
        off += 1
        size, payload = read_leb(buf, off)
        end = payload + size
        if sid == 2:
            count, o = read_leb(buf, payload)
            for i in range(count):
                mod, o = read_name(buf, o)
                name, o = read_name(buf, o)
                kind = buf[o]
                o += 1
                if kind == 0:
                    _, o = read_leb(buf, o)
                elif kind == 1:
                    o += 1
                    o = skip_limits(buf, o)
                elif kind == 2:
                    o = skip_limits(buf, o)
                elif kind == 3:
                    o += 2
                elif kind == 4:
                    o += 1
                    _, o = read_leb(buf, o)
                else:
                    raise SystemExit("unknown import kind %d at %d" % (kind, o))
                out.append((i, mod, name, kind))
        off = end
    return out


def main():
    if len(sys.argv) < 3:
        raise SystemExit(__doc__)
    wasm, js = sys.argv[1], sys.argv[2]
    haystacks = [(js, open(js, "r", encoding="utf-8", errors="replace").read())]
    for extra in sys.argv[3:]:
        haystacks.append((extra, open(extra, "r", encoding="utf-8", errors="replace").read()))

    imps = imports(wasm)
    print("wasm declares %d imports (%d in %s)"
          % (len(imps), sum(1 for i in imps if i[3] == 0), wasm))

    missing = []
    for idx, mod, name, kind in imps:
        if not any(name in text for _, text in haystacks):
            missing.append((idx, mod, name, kind))

    print("unresolved: %d" % len(missing))
    for idx, mod, name, kind in missing:
        kindname = {0: "func", 1: "table", 2: "mem", 3: "global", 4: "tag"}[kind]
        print("  Import #%d  %s.%s  [%s]" % (idx, mod, name, kindname))
    return 1 if missing else 0


if __name__ == "__main__":
    sys.exit(main())
