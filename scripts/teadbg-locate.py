#!/usr/bin/env python3
"""Locate a Java method + source line for an address in classes.wasm, using TeaVM's
classes.wasm.teadbg debug file.

Format taken verbatim from teavm-core 0.12.1-EAGLER-R3 sources
(DebugStringParser / DebugFileParser / DebugPackageParser / DebugClassParser /
DebugMethodParser / DebugLinesParser / DebugConstants).

usage: teadbg-locate.py <classes.wasm.teadbg> <address> [<address> ...]
"""
import bisect
import sys


def uleb(b, p):
    r = 0
    s = 0
    while True:
        x = b[p]
        p += 1
        r |= (x & 0x7F) << s
        if not x & 0x80:
            return r, p
        s += 7


def sleb(b, p):
    r = 0
    s = 0
    while True:
        x = b[p]
        p += 1
        r |= (x & 0x7F) << s
        s += 7
        if not x & 0x80:
            if x & 0x40:
                r -= 1 << s
            return r, p


LOC_START, LOC_END, LOC_LINE, LOC_FILE, LOC_PTR, LOC_USER = 0, 1, 2, 3, 4, 10


def load(path):
    b = open(path, "rb").read()
    assert b[:4] == b"tdbg", "not a tdbg file"
    p = 5
    sec = {}
    while p < len(b):
        n, p = uleb(b, p)
        name = b[p:p + n].decode()
        p += n
        dl, p = uleb(b, p)
        sec[name] = b[p:p + dl]
        p += dl

    # teavm_str : repeated uleb-length-prefixed utf8
    strs = []
    d = sec["teavm_str"]
    p = 0
    while p < len(d):
        n, p = uleb(d, p)
        strs.append(d[p:p + n].decode("utf8", "replace"))
        p += n

    # teavm_file : repeated { uleb parent, uleb nameIndex [, uleb ext] } (1-based)
    files = []
    d = sec["teavm_file"]
    p = 0
    while p < len(d):
        parent, p = uleb(d, p)
        ni, p = uleb(d, p)
        if ni & 1:
            ext, p = uleb(d, p)
            name = strs[ni >> 1] + "." + strs[ext]
        else:
            name = strs[ni >> 1]
        files.append((parent, name))

    def file_name(i):
        if i == 0:
            return None
        parent, name = files[i - 1]
        pn = file_name(parent)
        return (pn + "/" if pn else "") + name

    # teavm_pkg : repeated { uleb parent, uleb nameStr } (1-based, 0 = none)
    pkgs = []
    d = sec["teavm_pkg"]
    p = 0
    while p < len(d):
        a, p = uleb(d, p)
        bidx, p = uleb(d, p)
        pkgs.append((a, strs[bidx]))

    def pkg_name(i):
        if i == 0:
            return ""
        parent, name = pkgs[i - 1]
        pn = pkg_name(parent)
        return (pn + "." if pn else "") + name

    # teavm_cls : repeated { uleb pkg, uleb nameStr } (0-based)
    clss = []
    d = sec["teavm_cls"]
    p = 0
    while p < len(d):
        a, p = uleb(d, p)
        bidx, p = uleb(d, p)
        clss.append((a, strs[bidx]))

    def cls_name(i):
        pi, ni = clss[i]
        pn = pkg_name(pi)
        return (pn + "." if pn else "") + ni

    # teavm_mtd : repeated { uleb class, uleb nameStr } (0-based)
    mtds = []
    d = sec["teavm_mtd"]
    p = 0
    while p < len(d):
        a, p = uleb(d, p)
        bidx, p = uleb(d, p)
        mtds.append((a, strs[bidx]))

    def mtd_name(i):
        ci, ni = mtds[i]
        return cls_name(ci) + "." + ni

    # teavm_line : command stream
    d = sec["teavm_line"]
    p = 0
    address = 0
    line = 1
    file_idx = None
    seqs = []          # (start, end, methodIdx, stackOfMethods)
    marks = []         # (address, fileIdx, line, seqIndex)
    cur = None         # [start, methodIdx, stack]
    stack = []
    while p < len(d):
        cmd = d[p]
        p += 1
        if cmd == LOC_START:
            m, p = uleb(d, p)
            if cur is None:
                cur = [address, m]
            else:
                stack.append((cur[1], file_idx, line))
                cur[1] = m
            file_idx = None
            line = 1
            marks.append((address, file_idx, line, len(seqs) + (1 if cur else 0)))
        elif cmd == LOC_END:
            if not stack:
                if cur is not None:
                    seqs.append((cur[0], address, cur[1]))
                    cur = None
                file_idx = None
                line = 1
            else:
                _, file_idx, line = stack.pop()
                marks.append((address, file_idx, line, len(seqs) + (1 if cur else 0)))
        elif cmd == LOC_LINE:
            adv, p = sleb(d, p)
            line += adv
            marks.append((address, file_idx, line, len(seqs) + (1 if cur else 0)))
        elif cmd == LOC_FILE:
            file_idx, p = uleb(d, p)
            line = 1
            if p < len(d) and d[p] == LOC_LINE:
                p += 1
                adv, p = sleb(d, p)
                line += adv
            marks.append((address, file_idx, line, len(seqs) + (1 if cur else 0)))
        elif cmd == LOC_PTR:
            adv, p = uleb(d, p)
            address += adv
        elif cmd >= LOC_USER:
            v = cmd - LOC_USER
            address += v % 32
            line += v // 32 - 3
            marks.append((address, file_idx, line, len(seqs) + (1 if cur else 0)))
        else:
            raise SystemExit(f"bad line command {cmd} at {p - 1}")

    return dict(strs=strs, files=files, file_name=file_name, clss=clss,
                cls_name=cls_name, mtds=mtds, mtd_name=mtd_name,
                seqs=seqs, marks=marks, pkgs=pkgs, pkg_name=pkg_name)


def main():
    if len(sys.argv) < 3:
        raise SystemExit(__doc__)
    info = load(sys.argv[1])
    seqs = sorted(info["seqs"], key=lambda s: s[0])
    starts = [s[0] for s in seqs]
    marks = info["marks"]
    mstarts = [m[0] for m in marks]

    print(f"sequences={len(seqs)} address span 0x{seqs[0][0]:x}..0x{seqs[-1][1]:x}")

    for a in sys.argv[2:]:
        a = int(a, 0)
        i = bisect.bisect_right(starts, a) - 1
        if i < 0 or not (seqs[i][0] <= a < seqs[i][1]):
            print(f"\n@{a} (0x{a:x}): no method sequence")
            continue
        st, en, m = seqs[i]
        j = bisect.bisect_right(mstarts, a) - 1
        fl, ln = (None, None)
        if j >= 0:
            _, fl, ln, _ = marks[j]
        fname = info["file_name"](fl) if fl else None
        print(f"\n@{a} (0x{a:x}): {info['mtd_name'](m)}")
        print(f"    seq [0x{st:x}, 0x{en:x})  methodIndex={m}  "
              f"file={fname}  line={ln}")


if __name__ == "__main__":
    main()
