#!/usr/bin/env python3
"""List every class compiled into classes.wasm, from classes.wasm.teadbg.

`teadbg` carries TeaVM's own class table (teavm_cls), so this is the ground
truth for "is this class actually in the module?" -- far better than grepping
`strings` for a simple name, which matches substrings and method names too.

Cross-reference with `scripts/audit-classlib.py` to see which of the classes the
classlib lacks are nevertheless reachable (i.e. resolved from the host JDK and
compiled anyway) and therefore a real runtime risk.

usage:
  teadbg-classes.py FILE.teadbg            # "Class.Name<TAB>methodCount"
  teadbg-classes.py FILE.teadbg --names    # just the dotted class names
"""
import sys

sys.path.insert(0, __file__.rsplit("/", 1)[0])
from importlib.machinery import SourceFileLoader

_decode = SourceFileLoader(
    "decode_teadbg", __file__.rsplit("/", 1)[0] + "/decode-teadbg.py"
).load_module()


def load(path):
    b = open(path, "rb").read()
    assert b[:4] == b"tdbg", "not a teadbg file"
    p = 5
    sec = {}
    while p < len(b):
        n, p = _decode.uleb(b, p)
        name = b[p:p + n].decode()
        p += n
        dl, p = _decode.uleb(b, p)
        sec[name] = b[p:p + dl]
        p += dl
    strs = _decode.strings(sec["teavm_str"])
    pkgs = _decode.pairs(sec["teavm_pkg"])
    clss = _decode.pairs(sec["teavm_cls"])
    mtds = _decode.pairs(sec["teavm_mtd"])

    def pkg_name(i):
        if i == 0:
            return ""
        parent, nidx = pkgs[i - 1]
        pn = pkg_name(parent)
        return (pn + "." if pn else "") + strs[nidx]

    def cls_name(i):
        pi, ni = clss[i]
        pn = pkg_name(pi)
        return (pn + "." if pn else "") + strs[ni]

    counts = {}
    for ci, _ni in mtds:
        name = cls_name(ci)
        counts[name] = counts.get(name, 0) + 1

    # classes referenced anywhere in the table, even with no method entry
    all_classes = {cls_name(i): counts.get(cls_name(i), 0) for i in range(len(clss))}
    return all_classes, strs, mtds


def main():
    if len(sys.argv) < 2:
        raise SystemExit(__doc__)
    all_classes, _strs, _mtds = load(sys.argv[1])
    names_only = "--names" in sys.argv
    if names_only:
        for c in sorted(all_classes):
            print(c)
        return
    for c in sorted(all_classes):
        print("%s\t%d" % (c, all_classes[c]))


if __name__ == "__main__":
    main()
