#!/usr/bin/env python3
"""List every Java record declaration whose component list contains a primitive long.

Why this matters: javac synthesises record `equals()` as an
`invokedynamic` to java.lang.runtime.ObjectMethods.bootstrap, and TeaVM turns
that into IR with `ObjectMethodsSubstitutor.compareEquality`.  That method does

    case LONG: return a.isEqualTo(b);

which builds a `BinaryBranchingInstruction(EQUAL)` -- and
`StatementGenerator.visit(BinaryBranchingInstruction)` hardcodes
`OperationType.INT` for EQUAL/NOT_EQUAL.  So every `long` component of a record
emits `i32.eq`/`i32.ne` on i64 operands, which V8 rejects.

Only LONG is affected: BOOLEAN/BYTE/SHORT/CHAR/INT map to INT (correct), and
FLOAT/DOUBLE go through Float.compare/Double.compare (correct).

Adding an explicit `equals()` to the record stops javac emitting the
invokedynamic at all, so TeaVM compiles ordinary `lcmp; ifne` -- a path that is
already known-good across the rest of the codebase.

usage: list-long-records.py <srcRoot> [<srcRoot> ...]
"""
import os
import re
import sys

REC = re.compile(
    r"\brecord\s+([A-Za-z_$][\w$]*)\s*(\([^;{}]*?\))\s*(?:\b(?:extends|implements)\b[^;{]*)?\{"
)


def main():
    hits = []
    for root in sys.argv[1:]:
        for dirpath, _dirs, files in os.walk(root):
            for fn in files:
                if not fn.endswith(".java"):
                    continue
                path = os.path.join(dirpath, fn)
                try:
                    text = open(path, encoding="utf-8", errors="replace").read()
                except OSError:
                    continue
                for m in REC.finditer(text):
                    comps = m.group(2)
                    # strip annotations so @Nullable long x still matches
                    bare = re.sub(r"@\w+(?:\([^)]*\))?\s*", "", comps)
                    if re.search(r"\blong\b", bare):
                        line = text.count("\n", 0, m.start()) + 1
                        hits.append((path, line, m.group(1), " ".join(bare.split())))

    for path, line, name, comps in sorted(hits):
        print("%s:%d: record %s %s" % (path, line, name, comps))
    print("TOTAL %d" % len(hits))
    return 0


if __name__ == "__main__":
    sys.exit(main())
