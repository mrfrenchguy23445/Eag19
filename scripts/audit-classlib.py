#!/usr/bin/env python3
"""Find java.*/javax.* classes the project uses that the TeaVM wasm-gc classlib lacks.

Why this matters more than a compile check: `ClasspathResourceMapper.apply()`
resolves an unknown `java.*` name from the *host JDK* (step 3, the jrt image) or,
for a catch type, degrades silently to a hollow struct. Neither shows up as a
link problem. The `TUnknownHostException` episode is the proof: the class was
absent, nothing was reported, and the symptom was a `br_on_cast` validation
error thousands of lines away.

So this walks every import in the sources that actually compile into the wasm-gc
target and checks whether the class is reachable from
  * the patched teavm-classlib jar  (org/teavm/classlib/java/<pkg>/T<Name>.class)
  * jdk-shims;src*                  (shipped by hand, e.g. TUnknownHostException)
  * the project's own sources
Anything that resolves from none of those is a class the host JDK will answer,
which is exactly the fragile case.

usage: audit-classlib.py [--verbose]
"""
import os
import re
import subprocess
import sys
import zipfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
JAR = os.path.join(
    ROOT,
    "gradle/local-libs/org/teavm/teavm-classlib/0.12.1-EAGLER-R3/"
    "teavm-classlib-0.12.1-EAGLER-R3.jar",
)

# source roots that are compiled into the wasm-gc target
SRC_ROOTS = [
    "src/main/java",
    "src/wasm-gc-teavm/java",
    "src/teavm/java",
    "src/teavmc-classpath/java",
]

IMPORT = re.compile(r"^\s*import\s+(static\s+)?((?:java|javax)\.[\w.]+)\s*;")


def classlib_names():
    """Every T<Name>.class the patched classlib ships."""
    if not os.path.exists(JAR):
        sys.exit("no classlib jar at %s" % JAR)
    out = set()
    with zipfile.ZipFile(JAR) as z:
        for n in z.namelist():
            out.add(n)
    return out


def shim_names():
    """Simple names shipped under jdk-shims (by hand).

    The shims are relocated, and both a plain name and a T/X-prefixed name can
    stand in, so record each simple name and its de-prefixed form.
    """
    names = set()
    for dirpath, _dirnames, filenames in os.walk(os.path.join(ROOT, "jdk-shims")):
        for f in filenames:
            if not f.endswith(".java"):
                continue
            s = f[:-5]
            names.add(s)
            if s[:1] in ("T", "X") and len(s) > 1 and s[1:2].isupper():
                names.add(s[1:])
    return names


def classlib_entries(fqn):
    """Return every jar path this public name could resolve to.

    Two mapping rules matter, and they are read straight off the classlib's own
    META-INF/teavm.properties:

      mapPackageHierarchy|org.teavm.classlib.java=java
        stripPrefixFromPackageHierarchyClasses|org.teavm.classlib.java=T
          java.io.BufferedReader -> org/teavm/classlib/java/io/TBufferedReader.class

      mapPackageHierarchy|org.threeten.bp=java.time
          java.time.Instant       -> org/threeten/bp/Instant.class      (NO T)
          java.time.format.X      -> org/threeten/bp/format/X.class

    The ThreeTen rule is why java.time is *not* a host-JDK fallthrough despite
    shipping no classlib/java/time entry: the fork vendors the backport under
    org.threeten.bp and aliases it. Missing this is exactly how a naive audit
    reports java.time as unimplemented.
    """
    parts = fqn.split(".")
    if parts[:1] not in (["java"], ["javax"]):
        return []
    root, rest = parts[0], parts[1:]
    if root == "java" and rest[:1] == ["time"]:
        return ["org/threeten/bp/%s.class" % "/".join(rest[1:])]
    idx = next((i for i, p in enumerate(rest) if p[:1].isupper()), None)
    if idx is None:
        return []
    pkg = rest[:idx]
    name = "T" + rest[idx] + "".join("$" + x for x in rest[idx + 1:])
    return ["org/teavm/classlib/%s/%s/%s.class" % (root, "/".join(pkg), name)]


def local_classes():
    """Top-level class names defined in the target's own sources."""
    names = set()
    for rel in SRC_ROOTS:
        d = os.path.join(ROOT, rel)
        for dirpath, _dirnames, filenames in os.walk(d):
            for f in filenames:
                if f.endswith(".java"):
                    names.add(f[:-5])
    return names


def main():
    verbose = "--verbose" in sys.argv
    jar = classlib_names()
    shims = shim_names()
    local = local_classes()

    used = {}
    for rel in SRC_ROOTS:
        d = os.path.join(ROOT, rel)
        for dirpath, _dirnames, filenames in os.walk(d):
            for f in filenames:
                if not f.endswith(".java"):
                    continue
                path = os.path.join(dirpath, f)
                try:
                    with open(path, encoding="utf-8", errors="replace") as fh:
                        for i, line in enumerate(fh, 1):
                            m = IMPORT.match(line)
                            if not m:
                                continue
                            if m.group(1):  # 'static' -> member, not a class
                                continue
                            fqn = m.group(2)
                            used.setdefault(fqn, []).append(
                                (os.path.relpath(path, ROOT), i)
                            )
                except OSError:
                    pass

    missing = []
    for fqn, sites in sorted(used.items()):
        entries = classlib_entries(fqn)
        if entries and any(e in jar for e in entries):
            continue
        simple = fqn.rpartition(".")[2]
        top = simple.split("$")[0]
        if top in shims or simple in shims:
            continue
        if top in local:
            continue
        missing.append((fqn, top, sites))

    print("imports scanned : %d" % len(used))
    print("host-JDK-served : %d  (no classlib entry, no shim, not in src)" % len(missing))
    print()
    for fqn, top, sites in missing:
        print("%-52s %d site(s)" % (fqn, len(sites)))
        if verbose:
            for path, ln in sites[:6]:
                print("      %s:%d" % (path, ln))
            if len(sites) > 6:
                print("      ... +%d more" % (len(sites) - 6))
    print()
    print("REMINDER: reachable -> real failure; unreachable -> silently dropped.")
    print("For each of these, grep the project for its simple name to see if any")
    print("site is on the boot path.")


if __name__ == "__main__":
    main()
