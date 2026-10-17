#!/usr/bin/env python3
"""Decode classes.wasm.teadbg (TeaVM GC debug file) and map an address to a Java method.

Format reverse-engineered from teavm-core 0.12.1-EAGLER-R3:
  "tdbg" | u8 version | repeated { uleb namelen, name, uleb datalen, data }
  teavm_str  : repeated { uleb strlen, utf8 }
  teavm_pkg  : repeated { uleb parentPkg, uleb nameStrIdx }
  teavm_cls  : repeated { uleb pkg, uleb nameStrIdx }
  teavm_mtd  : repeated { uleb classIdx, uleb nameStrIdx }
  teavm_line : byte stream of commands driving an `address` counter:
                 0 start(method) 1 end 2 moveToLine 3 moveToFile 4 advanceAddress
               >=10 special (compact line+address delta)

A sequence [startAddress, endAddress) belongs to one method.

usage: decode-teadbg.py FILE.teadbg ADDRESS [CODE_SECTION_OFFSET]
"""
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


def strings(d):
    out = []
    p = 0
    while p < len(d):
        n, p = uleb(d, p)
        out.append(d[p:p + n].decode("utf8", "replace"))
        p += n
    return out


def pairs(d):
    out = []
    p = 0
    while p < len(d):
        a, p = uleb(d, p)
        b, p = uleb(d, p)
        out.append((a, b))
    return out


def lines(d):
    """Return list of (startAddress, endAddress, methodIndex)."""
    seqs = []
    address = 0
    seq_start = None
    cur_method = None
    line = 1
    file_idx = None
    p = 0
    depth = 0
    while p < len(d):
        c = d[p]
        p += 1
        if c == 0:                                    # start
            m, p = uleb(d, p)
            if cur_method is None:
                cur_method = m
                seq_start = address
            else:
                depth += 1
        elif c == 1:                                  # end
            if depth > 0:
                depth -= 1
            else:
                if cur_method is not None:
                    seqs.append((seq_start, address, cur_method))
                cur_method = None
                seq_start = None
        elif c == 2:                                  # moveToLine
            d2, p = sleb(d, p)
            line += d2
        elif c == 3:                                  # moveToFile
            file_idx, p = uleb(d, p)
            line = 1
            if p < len(d) and d[p] == 2:
                p += 1
                dd, p = sleb(d, p)
                line += dd
        elif c == 4:                                  # advanceAddress
            adv, p = uleb(d, p)
            address += adv
        elif c >= 10:
            v = c - 10
            address += v % 32
            line += (v // 32) - 3
        else:
            raise SystemExit(f"unknown line command {c} at {p-1}")
    if cur_method is not None:
        seqs.append((seq_start, address, cur_method))
    return seqs


def main():
    path = sys.argv[1]
    addr = int(sys.argv[2], 0)
    code_off = int(sys.argv[3], 0) if len(sys.argv) > 3 else 0

    b = open(path, "rb").read()
    assert b[:4] == b"tdbg"
    p = 5
    sec = {}
    while p < len(b):
        n, p = uleb(b, p)
        name = b[p:p + n].decode()
        p += n
        dl, p = uleb(b, p)
        sec[name] = b[p:p + dl]
        p += dl

    strs = strings(sec["teavm_str"])
    pkgs = pairs(sec["teavm_pkg"])
    clss = pairs(sec["teavm_cls"])
    mtds = pairs(sec["teavm_mtd"])

    def pkg_name(i):
        # package indices are 1-based: 0 means "no package"
        if i == 0:
            return ""
        parent, nidx = pkgs[i - 1]
        name = strs[nidx]
        pn = pkg_name(parent)
        return (pn + "." if pn else "") + name

    def cls_name(i):
        pi, ni = clss[i]
        pn = pkg_name(pi)
        return (pn + "." if pn else "") + strs[ni]

    seqs = lines(sec["teavm_line"])
    seqs.sort(key=lambda s: s[0])
    print(f"strings={len(strs)} pkgs={len(pkgs)} classes={len(clss)} "
          f"methods={len(mtds)} sequences={len(seqs)}")
    print(f"address range: 0x{seqs[0][0]:x} .. 0x{seqs[-1][1]:x}")

    def show(a, label):
        import bisect
        starts = [s[0] for s in seqs]
        i = bisect.bisect_right(starts, a) - 1
        if i < 0 or not (seqs[i][0] <= a < seqs[i][1]):
            print(f"  {label}: no sequence contains {a} (0x{a:x})")
            return
        st, en, m = seqs[i]
        ci, ni = mtds[m]
        print(f"  {label}: {cls_name(ci)}.{strs[ni]}"
              f"  seq=[{st}, {en}) 0x{st:x}..0x{en:x} methIdx={m}")

    show(addr, "raw")
    if code_off:
        show(addr - code_off, "code-relative")
    show(addr + code_off, "code+off")


if __name__ == "__main__":
    main()
