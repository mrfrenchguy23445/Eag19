#!/usr/bin/env python3
"""Summarise the problems a TeaVM link reported, and diff two links against each other.

A wasm_gc link reports one block per unresolved or unsupported symbol: a line naming
the problem, then the call stack TeaVM found for it. This reads those blocks back out
of a gradle log and answers the only two questions that matter while iterating on a
link -- how many problems are left, and which of them are new since the last run.

    scripts/count-link-problems.py ../wasm14.keep.log
    scripts/count-link-problems.py --diff ../wasm13.keep.log ../wasm14.keep.log

Stack frames are union artifacts: the root call site is where TeaVM happened to be
when it hit the symbol, not necessarily where the symbol became reachable, so --top
prints only the frames nearest the failure and drops the tail.
"""
import argparse
import re
import sys

# "Class java.net.InetAddress was not found"
# "Method java.lang.Long.sum(JJ)J was not found"
# "Class org.teavm.runtime.Fiber is not supported on current target"
PROBLEM = re.compile(
    r"^(?:Class|Method|Field) (.+?) (was not found|is not supported on current target)$"
)
# A different failure class that only shows up once every PROBLEM is gone: TeaVM's
# reachability is clean but its wasm body generator is not. Both of these print a
# message at column 0 and then, after TeaVMProblemRenderer's CallLocation, the
# program's call stack -- same shape as a PROBLEM block, so it parses the same way.
#   "Failed generating method body due to internal exception: <toString>"
#   "Method is not annotated with org.teavm.interop.Import"
CODEGEN = re.compile(
    r"^(?:Failed generating method body due to internal exception:.*"
    r"|Method is not annotated with \S+)$"
)
# Only space-indented frames: the internal-exception message carries its own JVM
# stack, which TeaVM prints tab-indented, and those frames are TeaVM's own call
# graph rather than the program's. The program stack starts at the CallLocation.
FRAME = re.compile(r"^ +at (\S+)")

LABEL = {"was not found": "missing", "is not supported on current target": "unsupported",
         "codegen": "codegen"}


def parse(path):
    """Return (problems, exit_code) from one link log.

    A problem is a PROBLEM line plus every FRAME line that follows it; anything else
    ends the block. Exit code comes from the `EXIT=<n>` marker the caller appends, and
    is None for a log that was never finished.
    """
    problems = []
    exit_code = None
    with open(path, encoding="utf-8", errors="replace") as handle:
        for line in handle:
            line = line.rstrip("\n")
            match = PROBLEM.match(line)
            if match:
                problems.append({
                    "symbol": match.group(1),
                    "kind": match.group(2),
                    "frames": [],
                })
                continue
            if CODEGEN.match(line):
                problems.append({
                    "symbol": line,
                    "kind": "codegen",
                    "frames": [],
                })
                continue
            if problems:
                frame = FRAME.match(line)
                if frame:
                    problems[-1]["frames"].append(frame.group(1))
                elif line.strip() and not line.startswith("\t"):
                    # blank lines and tab-indented causes belong to nothing here
                    pass
            if line.startswith("EXIT="):
                exit_code = int(line[len("EXIT="):])
    return problems, exit_code


def key(problem):
    """Symbols repeat across sites; a set of these is what --diff compares."""
    return problem["symbol"], problem["kind"]


def describe(problem, top):
    symbol = problem["symbol"]
    kind = LABEL.get(problem["kind"], problem["kind"])
    out = ["[%s] %s" % (kind, symbol)]
    for frame in problem["frames"][:top]:
        out.append("      at " + frame)
    hidden = len(problem["frames"]) - top
    if hidden > 0:
        out.append("      ... %d more frame(s)" % hidden)
    return "\n".join(out)


def counts(problems):
    missing = sum(1 for p in problems if p["kind"] == "was not found")
    unsupported = sum(1 for p in problems if p["kind"] == "is not supported on current target")
    codegen = sum(1 for p in problems if p["kind"] == "codegen")
    return missing, unsupported, codegen


def main(argv):
    parser = argparse.ArgumentParser(description=__doc__,
                                     formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("logs", nargs="+", metavar="LOG",
                        help="link log(s); with --diff: PREV CURR")
    parser.add_argument("--diff", action="store_true",
                        help="compare two logs: report problems CURR has that PREV did not")
    parser.add_argument("--top", type=int, default=3, metavar="N",
                        help="stack frames to print per problem (default 3)")
    parser.add_argument("--all", action="store_true",
                        help="print every problem, not just the diff")
    args = parser.parse_args(argv)

    if args.diff and len(args.logs) != 2:
        parser.error("--diff takes exactly two logs: PREV CURR")

    if args.diff:
        previous, _ = parse(args.logs[0])
        current, code = parse(args.logs[1])
        prev_keys = {key(p) for p in previous}
        new = [p for p in current if key(p) not in prev_keys]
        curr_keys = {key(p) for p in current}
        gone = [p for p in previous if key(p) not in curr_keys]

        print("%s -> %s" % (args.logs[0], args.logs[1]))
        pm, pu, pc = counts(previous)
        cm, cu, cc = counts(current)
        print("  previous: %d total (%d missing, %d unsupported, %d codegen)" % (len(previous), pm, pu, pc))
        print("  current : %d total (%d missing, %d unsupported, %d codegen)" % (len(current), cm, cu, cc))
        print("  gone    : %d" % len(gone))
        print("  new     : %d" % len(new))
        if new or args.all:
            print("\n-- new in %s --" % args.logs[1])
            for problem in new:
                print(describe(problem, args.top))
        if gone:
            print("\n-- fixed since %s --" % args.logs[0])
            for problem in gone:
                symbol, kind = key(problem)
                print("  %s (%s)" % (symbol, LABEL.get(kind, kind)))
        if code == 0:
            print("\nlink exited 0 -- no problems expected")
        return 0 if not new else 1

    problems, code = parse(args.logs[0])
    missing, unsupported, codegen = counts(problems)
    print("%s" % args.logs[0])
    print("  %d problem(s): %d missing, %d unsupported, %d codegen"
          % (len(problems), missing, unsupported, codegen))
    print("  exit code: %s" % ("not recorded (log unfinished)" if code is None else code))
    if problems:
        print()
        for problem in problems:
            print(describe(problem, args.top))
    return 0 if not problems else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
