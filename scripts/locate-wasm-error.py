#!/usr/bin/env python3
"""Localise a V8 wasm-GC type error: find the function and instruction it names.

V8 reports errors like

    Compiling function #5512 failed: struct.set[1] expected type (ref null 22),
    found global.get of type (ref 11928) @+2788072

which carries an offset but no names (TeaVM emits no name section, only .teadbg).
This walks the code section to the body containing that offset, reports its index in
the function index space, and dumps the instruction bytes around it. It also decodes
the two type indices so the mismatch is readable.

    python3 scripts/locate-wasm-error.py classes.wasm 5512 2788072
"""
import struct
import sys


def leb(buf, i):
    """Unsigned LEB128 -> (value, next_index)."""
    out = 0
    shift = 0
    while True:
        b = buf[i]
        i += 1
        out |= (b & 0x7F) << shift
        if not b & 0x80:
            return out, i
        shift += 7


def sleb(buf, i):
    """Signed LEB128 -> (value, next_index)."""
    out = 0
    shift = 0
    while True:
        b = buf[i]
        i += 1
        out |= (b & 0x7F) << shift
        shift += 7
        if not b & 0x80:
            if b & 0x40:
                out -= 1 << shift
            return out, i


def sections(buf):
    i = 8
    while i < len(buf):
        sid = buf[i]
        i += 1
        size, i = leb(buf, i)
        yield sid, i, size
        i += size


HEAP = {-16: "func", -17: "extern", -18: "any", -19: "eq", -20: "i31",
        -21: "struct", -22: "array", -23: "none", -24: "noextern",
        -25: "nofunc", -26: "exn", -27: "noexn"}


def heaptype(buf, i):
    """Heap type (s33: negative = abstract, non-negative = type index)."""
    v, i = sleb(buf, i)
    if v < 0:
        return HEAP.get(v, "abstract(%d)" % v), i
    return v, i


def valtype(buf, i):
    b = buf[i]
    i += 1
    if b == 0x7F:
        return "i32", i
    if b == 0x7E:
        return "i64", i
    if b == 0x7D:
        return "f32", i
    if b == 0x7C:
        return "f64", i
    if b == 0x7B:
        return "v128", i
    if b in (0x63, 0x64, 0x6E, 0x6F, 0x70, 0x6B, 0x6C, 0x73, 0x6D):
        # ref null / ref / ... + heap type
        ht, i = heaptype(buf, i - 1 + 1 - 1 if False else i)
        return ("ref null " if b in (0x63, 0x6E) else "ref ") + str(ht), i
    # GC also lets heaptype be written directly (s33)
    i -= 1
    ht, j = heaptype(buf, i)
    if isinstance(ht, str) or j > i:
        return str(ht), j
    return "?0x%02X" % b, i + 1


def parse_types(buf, start, size):
    """Decode a wasm GC type section -> list of printable type descriptions."""
    i = start
    end = start + size
    count, i = leb(buf, i)
    types = []
    while len(types) < count and i < end:
        tag = buf[i]
        i += 1
        if tag == 0x4E:  # rec
            n, i = leb(buf, i)
            group = []
            for _ in range(n):
                desc, i = one_type(buf, i)
                group.append(desc)
            types.append("rec[" + ", ".join(group) + "]")
        else:
            i -= 1
            desc, i = one_type(buf, i)
            types.append(desc)
    return types


def one_type(buf, i):
    """Decode a single (possibly sub/final-prefixed) type definition."""
    tag = buf[i]
    i += 1
    final = True
    if tag in (0x4F, 0x50):  # final, sub
        final = tag == 0x4F
        n, i = leb(buf, i)
        supers = []
        for _ in range(n):
            s, i = heaptype(buf, i)
            supers.append(str(s))
        head = ("final " if final else "") + "sub(" + ",".join(supers) + ")"
        tag = buf[i]
        i += 1
    else:
        head = "final" if True else ""
    if tag == 0x5F:  # struct
        n, i = leb(buf, i)
        fields = []
        for _ in range(n):
            t, i = valtype(buf, i)
            mut = buf[i]
            i += 1
            fields.append(("%s mut" % t if mut else t))
        return "struct{%s}" % ", ".join(fields), i
    if tag == 0x60:  # func
        n, i = leb(buf, i)
        params = []
        for _ in range(n):
            t, i = valtype(buf, i)
            params.append(t)
        n, i = leb(buf, i)
        rets = []
        for _ in range(n):
            t, i = valtype(buf, i)
            rets.append(t)
        return "func(%s)->(%s)" % (",".join(params), ",".join(rets)), i
    if tag == 0x61:  # array
        t, i = valtype(buf, i)
        mut = buf[i]
        i += 1
        return "array{%s}" % (t + " mut" if mut else t), i
    return "tag=0x%02X" % tag, i


def main():
    path = sys.argv[1]
    want_func = int(sys.argv[2]) if len(sys.argv) > 2 else None
    offset = int(sys.argv[3]) if len(sys.argv) > 3 else None

    buf = open(path, "rb").read()
    code = None
    imports_count = None
    types = []
    for sid, start, size in sections(buf):
        if sid == 1:
            types = parse_types(buf, start, size)
        elif sid == 2:
            n, i = leb(buf, start)
            imports_count = n
        elif sid == 10:
            code = (start, size)

    print("types parsed: %d" % len(types))
    for t in (22, 11928):
        if t < len(types):
            print("  type %-6d = %s" % (t, types[t]))

    # Walk the code section, recording each body's absolute offset.
    start, size = code
    i = start
    end = start + size
    n, i = leb(buf, i)
    bodies = []          # (abs_offset_of_body_start, size)
    pos = i
    for _ in range(n):
        bsize, pos = leb(buf, pos)   # pos now points at the body itself
        bodies.append((pos, bsize))
        pos += bsize
    print("defined functions: %d (imports: %d)" % (len(bodies), imports_count))

    if offset is not None:
        for label, target in (("module-absolute", offset),
                              ("code-relative", start + offset)):
            hit = None
            for idx, (bs, sz) in enumerate(bodies):
                if bs <= target < bs + sz:
                    hit = idx
                    break
            if hit is None:
                print("  %-16s %d : not inside any body" % (label, target))
                continue
            print("  %-16s %d : defined func #%d, function index #%d"
                  % (label, target, hit, hit + imports_count))
            if want_func is None or hit + imports_count == want_func or hit == want_func:
                dump(buf, bodies[hit][0], bodies[hit][1], target, types)

    if want_func is not None:
        for idx in (want_func - imports_count, want_func):
            if 0 <= idx < len(bodies):
                bs, sz = bodies[idx]
                print("candidate defined func #%d (size %d)" % (idx, sz))
                dump(buf, bs, sz, None, types)


def dump(buf, body_start, body_size, target, types):
    """Print the instructions near `target`, flagging struct.set / global.get."""
    i = body_start
    locals_n, i = leb(buf, i)
    for _ in range(locals_n):
        _, i = leb(buf, i)
        i += 1
    end = body_start + body_size
    lo = target - 96 if target else i
    lo = max(lo, i)
    hi = (target + 40) if target else min(i + 160, end)
    print("    locals_end=%d body=[%d,%d) window=[%d,%d)"
          % (i, body_start, body_start + body_size, lo, hi))
    # annotate known opcodes in the window
    j = lo
    while j < hi and j < end:
        b = buf[j]
        if b == 0x23:  # global.get
            g, nxt = leb(buf, j + 1)
            print("      +%d global.get #%d" % (j, g))
            j = nxt
            continue
        if b == 0xFB and j + 1 < end:
            op = buf[j + 1]
            if op == 0x05:  # struct.set
                t, nxt = leb(buf, j + 2)
                f, nxt = leb(buf, nxt)
                print("      +%d struct.set type=%d field=%d%s"
                      % (j, t, f, "   <== FAILING INSTRUCTION" if target and j == target else ""))
                j = nxt
                continue
            if op in (0x02, 0x03, 0x04):  # struct.get*
                t, nxt = leb(buf, j + 2)
                f, nxt = leb(buf, nxt)
                print("      +%d struct.get%s type=%d field=%d"
                      % (j, ["", "", "", "_s", "_u"][op - 2], t, f))
                j = nxt
                continue
        j += 1
    print("    bytes[%d..%d]: %s" % (lo, hi, buf[lo:hi].hex()))


if __name__ == "__main__":
    main()
