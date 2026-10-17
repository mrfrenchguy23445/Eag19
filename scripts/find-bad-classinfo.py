#!/usr/bin/env python3
"""Find globals whose declared type is a *root* struct that cannot be stored
into an object's `ref null 22` classinfo slot.

Object field 0 is always typed `ref null 22` (22 = java.lang.Object's
classinfo: {Class, m1, m2, m3, m4}).  Any classinfo global declared with a
struct type that is NOT a declared subtype of 22 is a V8 type error waiting to
happen at the first `struct.set` that consumes it.

    python3 scripts/find-bad-classinfo.py target_teavm_wasm_gc/javascript/classes.wasm
"""
import importlib.util
import sys

import os
spec = importlib.util.spec_from_file_location(
    "gt", os.path.join(os.path.dirname(os.path.abspath(__file__)), "gc-types.py"))
gt = importlib.util.module_from_spec(spec)
spec.loader.exec_module(gt)

BASE = 22           # java.lang.Object's classinfo type
OBJ = 11            # java.lang.Object


def read_sections(buf):
    secs = {}
    p = 8
    while p < len(buf):
        sid = buf[p]
        p += 1
        n = 0
        sh = 0
        while True:
            b = buf[p]
            p += 1
            n |= (b & 0x7F) << sh
            if not b & 0x80:
                break
            sh += 7
        secs.setdefault(sid, []).append((p, n))
        p += n
    return secs


def const_expr(r):
    """Decode one constant expression; returns opcode summary."""
    ops = []
    while True:
        b = r.buf[r.p]
        if b == 0x0B:
            r.p += 1
            return ops
        if b == 0x41:
            r.p += 1
            v, r.p = gt_sleb(r.buf, r.p)
            ops.append("i32.const %d" % v)
        elif b == 0x42:
            r.p += 1
            v, r.p = gt_sleb(r.buf, r.p)
            ops.append("i64.const %d" % v)
        elif b == 0x43:
            r.p += 5
            ops.append("f32.const")
        elif b == 0x44:
            r.p += 9
            ops.append("f64.const")
        elif b == 0x23:
            r.p += 1
            v, r.p = gt_uleb(r.buf, r.p)
            ops.append("global.get %d" % v)
        elif b == 0xD0:
            r.p += 1
            ops.append("ref.null %s" % r.heaptype())
        elif b == 0xD1:
            r.p += 1
            ops.append("ref.is_null")
        elif b == 0xD2:
            r.p += 1
            v, r.p = gt_uleb(r.buf, r.p)
            ops.append("ref.func %d" % v)
        elif b == 0xFB:
            r.p += 1
            op, r.p = gt_uleb(r.buf, r.p)
            name = {0: "struct.new", 1: "struct.new_default", 2: "struct.get",
                    3: "struct.get_s", 4: "struct.get_u", 5: "struct.set",
                    6: "array.new", 7: "array.new_default", 8: "array.new_fixed",
                    9: "array.new_data", 10: "array.new_elem"}.get(op, "gc.%d" % op)
            t, r.p = gt_uleb(r.buf, r.p)
            extra = ""
            if op in (2, 3, 4, 5):
                # struct.get / struct.get_s / struct.get_u / struct.set: typeidx fieldidx
                f, r.p = gt_uleb(r.buf, r.p)
                extra = "[%d]" % f
            elif op == 8:
                # array.new_fixed: typeidx count  -- count is a second uleb;
                # not consuming it desynchronises the whole expression.
                c, r.p = gt_uleb(r.buf, r.p)
                extra = "[%d]" % c
            elif op in (9, 10):
                # array.new_data / array.new_elem: typeidx dataidx|elemidx
                x, r.p = gt_uleb(r.buf, r.p)
                extra = ",%d" % x
            ops.append("%s %d%s" % (name, t, extra))
        elif b == 0x0C:                       # select t*
            r.p += 1
            ops.append("select")
            while r.buf[r.p] != 0x0B and r.buf[r.p] in (0x7F, 0x7E, 0x7D, 0x7C, 0x63, 0x64):
                r.p += 1
        else:
            raise ValueError("const expr opcode 0x%02X at %d" % (b, r.p))


def gt_uleb(buf, i):
    out = 0
    sh = 0
    while True:
        b = buf[i]
        i += 1
        out |= (b & 0x7F) << sh
        if not b & 0x80:
            return out, i
        sh += 7


def gt_sleb(buf, i):
    out = 0
    sh = 0
    while True:
        b = buf[i]
        i += 1
        out |= (b & 0x7F) << sh
        sh += 7
        if not b & 0x80:
            if b & 0x40:
                out -= 1 << sh
            return out, i


def main():
    path = sys.argv[1]
    buf = open(path, "rb").read()
    secs = read_sections(buf)

    ts, tsz = secs[1][0]
    types, groups = gt.parse_types(buf, ts, ts + tsz)
    print("types: %d" % len(types))

    # subtype edges
    supers = {}
    for i, t in enumerate(types):
        if t.startswith("sub(") or t.startswith("sub final("):
            inside = t[t.index("(") + 1:t.index(")")]
            supers[i] = [int(x) for x in inside.split(",") if x.strip().isdigit()]

    def is_sub(a, b, seen=None):
        if a == b:
            return True
        seen = seen or set()
        for s in supers.get(a, []):
            if s in seen:
                continue
            seen.add(s)
            if is_sub(s, b, seen):
                return True
        return False

    def root_classinfo(i):
        t = types[i]
        return (t.startswith("sub final() struct{ref null 38 mut")
                or t.startswith("sub() struct{ref null 38 mut"))

    imported = 0
    isec = secs.get(2)
    if isec:
        r = gt.Reader(buf, isec[0][0], isec[0][0] + isec[0][1])
        n = r.uleb()
        for _ in range(n):
            r.uleb()
            r.uleb()
            k = r.buf[r.p]
            r.p += 1
            if k == 0x00:
                r.uleb()
            elif k == 0x01:
                r.p += 1
                f = r.buf[r.p - 1]
                if f & 1:
                    r.uleb()
                if f & 2:
                    r.uleb()
            elif k == 0x02:
                f = r.buf[r.p]
                r.p += 1
                r.uleb()
                if f & 1:
                    r.uleb()
            elif k == 0x03:
                r.valtype()
                r.p += 1
            elif k == 0x04:
                r.uleb()
                r.p += 1
            if k == 0x03:
                imported += 1

    gs, gsz = secs[6][0]
    r = gt.Reader(buf, gs, gs + gsz)
    n = r.uleb()
    print("globals: %d section entries + %d imported" % (n, imported))
    bad = []
    for idx in range(n):
        t = r.valtype()
        r.p += 1
        init = const_expr(r)
        gidx = imported + idx
        m = gt_types_index(types, t)
        if m is not None and root_classinfo(m):
            bad.append((gidx, m, t, init))
    print()
    print("globals whose type is a ROOT struct shaped like a classinfo "
          "(stored into object field0, typed ref null %d):" % BASE)
    if not bad:
        print("  (none)")

    # A bad global only *matters* if something reads it: a `global.get` is the
    # value that ends up on the stack when V8 type-checks the store.  Scan the
    # code section and the global inits for reads so one pass gives a complete
    # picture instead of V8's first-error-only report.
    read_sites = find_global_gets(buf, secs)
    actionable = []
    for gidx, m, t, init in bad:
        ok = is_sub(m, BASE)
        sites = read_sites.get(gidx, [])
        verdict = ("OK (subtype of %d)" % BASE) if ok else (
            "ACTIONABLE: not a subtype of %d, read %d time(s)" % (BASE, len(sites))
            if sites else "harmless: never read")
        print("  global #%d : %s   [%s]" % (gidx, t, verdict))
        print("      type %d = %s" % (m, types[m]))
        print("      init: %s" % " ; ".join(init))
        if sites:
            print("      reads at: %s" % ", ".join(str(x) for x in sites[:6]))
        if not ok and sites:
            actionable.append(gidx)

    print()
    if actionable:
        print("VERDICT: %d root classinfo global(s) that ARE read -> "
              "V8 will reject the module: %s"
              % (len(actionable), ", ".join("#%d" % g for g in actionable)))
        return 1
    print("VERDICT: no read root-classinfo global -> this class of V8 "
          "CompileError cannot occur.")
    return 0


def find_global_gets(buf, secs):
    """Map global index -> [absolute addresses] for `global.get` in code
    bodies and in global initialisers."""
    out = {}

    def scan(start, size):
        end = start + size
        p = start
        while p < end:
            if buf[p] == 0x23:
                g, q = gt_uleb(buf, p + 1)
                out.setdefault(g, []).append(p)
                p = q
            else:
                p += 1

    if 10 in secs:
        scan(*secs[10][0])
    if 6 in secs:
        scan(*secs[6][0])
    return out


def gt_types_index(types, t):
    if t.startswith("ref ") and not t.startswith("ref null "):
        v = t[4:]
        return int(v) if v.isdigit() else None
    if t.startswith("ref null "):
        v = t[9:]
        return int(v) if v.isdigit() else None
    return None


if __name__ == "__main__":
    sys.exit(main())
