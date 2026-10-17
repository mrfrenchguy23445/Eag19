#!/usr/bin/env python3
"""Scan the code section for `global.get <idx>` sites of given globals.

usage: find-global-uses.py <classes.wasm> <globalidx>...
prints absolute module addresses (module-absolute, matching V8/teadbg).
"""
import sys, importlib.util, os

spec = importlib.util.spec_from_file_location("gct",
        os.path.join(os.path.dirname(os.path.abspath(__file__)), "gc-types.py"))
gct = importlib.util.module_from_spec(spec); spec.loader.exec_module(gct)


def sections(buf):
    assert buf[:4] == b'\0asm'
    p = 8
    out = []
    while p < len(buf):
        sid = buf[p]; p, n = gct.Reader(buf, p, len(buf)).uleb(), 0
        # Reader.uleb returns (val, pos)? check
        raise SystemExit("placeholder")


def scan_sections(buf):
    class R(gct.Reader):
        pass
    p = 8
    out = []
    while p < len(buf):
        r = gct.Reader(buf, p, len(buf))
        sid = buf[p]
        r.p += 1
        n, pos = None, None
        # uleb returns value and advances? inspect
        v = r.uleb()
        if isinstance(v, tuple):
            n, q = v
        else:
            n = v; q = r.p
        out.append((sid, q, n))
        p = q + n
    return out


def leb(n):
    out = bytearray()
    while True:
        b = n & 0x7f; n >>= 7
        if n:
            out.append(b | 0x80)
        else:
            out.append(b); break
    return bytes(out)


def main():
    path = sys.argv[1]
    targets = [int(x) for x in sys.argv[2:]]
    buf = open(path, 'rb').read()
    secs = scan_sections(buf)
    codes = [s for s in secs if s[0] == 10]
    if not codes:
        print("no code section"); return
    _, cs, cl = codes[0]
    pats = {g: b'\x23' + leb(g) for g in targets}
    # NOTE: pattern includes the 0x23 opcode byte, so match at p (not p+1)
    hits = {g: [] for g in targets}
    end = cs + cl
    p = cs
    while p < end:
        if buf[p] == 0x23:
            for g, pt in pats.items():
                if buf[p:p+len(pt)] == pt:
                    hits[g].append(p)
        p += 1
    for g in targets:
        print(f"global #{g}: {len(hits[g])} sites first={hits[g][:5]}")


main()
