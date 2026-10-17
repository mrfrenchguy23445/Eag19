#!/usr/bin/env python3
"""Print one function block out of a `.wat` produced by `wasm-tools print`.

usage: show-wat-func.py <file.wat> <funcIndexInFunctionSpace>

`wasm-tools print` labels blocks `func (;N;)` using the full function index
space (imports first), so funcIndex = codeIndex + importCount.
"""
import re
import sys


def main():
    path, want = sys.argv[1], int(sys.argv[2])
    lines = open(path, encoding="utf-8", errors="replace").read().split("\n")
    starts = [i for i, l in enumerate(lines) if re.match(r"^\s*\(func ", l)]
    bounds = starts + [len(lines)]
    for a, b in zip(bounds, bounds[1:]):
        m = re.match(r"^\s*\(func \(;(\d+);\)", lines[a])
        if m and int(m.group(1)) == want:
            print("\n".join(lines[a:b]))
            return
    raise SystemExit("func (; %d ;) not found (%d blocks)" % (want, len(starts)))


if __name__ == "__main__":
    sys.exit(main())
