#!/usr/bin/env python3
"""Pick a stub kind for each code index from the module's own type section.

The choice is not cosmetic: a body must typecheck against the function's real
signature, or V8 rejects the module again and the "fix" looks like a new error.

  (result i32)  -> i32   returns 0 instead of trapping (preferred)
  no result     -> none   plain void return (preferred)
  anything else -> trap   valid for any signature, but unwinds into the game's
                          exception handler and usually kills boot

funcIdx = codeIdx + 199 (199 imports), matching every other script here.

usage: auto-stub-kind.py <classes.wasm> <codeIdx> [codeIdx ...]
       (prints comma-separated codeIdx:kind)
"""
import re
import sys

IMPORTS = 199
HEADER = re.compile(r"^\s*\(func \(;(\d+);\)")


def main():
    if len(sys.argv) < 3:
        sys.exit(__doc__)
    wat = sys.argv[1]
    code_idxs = [int(a) for a in sys.argv[2:]]
    want = {c + IMPORTS: c for c in code_idxs}
    found = {}

    with open(wat, encoding="utf-8", errors="replace") as fh:
        for line in fh:
            m = HEADER.match(line)
            if not m:
                continue
            func_idx = int(m.group(1))
            if func_idx not in want:
                continue
            if "(result i32)" in line:
                kind = "i32"
            elif "(result " not in line:
                kind = "none"
            else:
                kind = "trap"
            found[want[func_idx]] = kind
            if len(found) == len(want):
                break

    missing = [c for c in code_idxs if c not in found]
    if missing:
        sys.exit("not found in wat: %s" % missing)
    print(",".join("%d:%s" % (c, found[c]) for c in code_idxs))


if __name__ == "__main__":
    main()
