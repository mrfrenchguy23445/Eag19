#!/usr/bin/env python3
"""Decode the exact wasm-GC type mismatch V8 refuses to instantiate.

V8 reports

    Compiling function #5512 failed: struct.set[1] expected type (ref null 22),
    found global.get of type (ref 11928) @+2788072

TeaVM emits no name section, so this script supplies the parts V8 leaves out:
what types 22/11928/12225 actually are, what the global on the stack is typed
as, and every struct.set in the offending function with its field index.

    python3 scripts/decode-gc-error.py classes.wasm 5512 2788072

Binary-format note: TeaVM's type section uses 0x5E=array, 0x5F=struct, 0x60=func
(verified against org.teavm.backend.wasm.parser.TypeSectionParser, tableswitch
cases 94..96), so 0x61 must not be read as an array -- doing so desynchronises
the whole section and makes type 22 decode as garbage.
"""
import sys

SHORT_REFTYPE = {
    0x70: "funcref", 0x6F: "externref", 0x6E: "anyref", 0x6D: "eqref",
    0x6C: "i31ref", 0x6B: "structref", 0x6A: "arrayref", 0x69: "nullref",
    0x68: "nullexternref", 0x67: "nullfuncref", 0x73: "exnref",
}
ABSTRACT_HEAP = {
    -16: "func", -17: "extern", -18: "any", -19: "eq", -20: "i31",
    -21: "struct", -22: "array", -23: "none", -24: "noextern",
    -25: "nofunc", -26: "exn", -27: "noexn",
}


def leb(buf, i):
    out = shift = 0
    while True:
        b = buf[i]
        i += 1
        out |= (b & 0x7F) << shift
        if not b & 0x80:
            return out, i
        shift += 7


def sleb(buf, i):
    out = shift = 0
    while True:
        b = buf[i]
        i += 1
        out |= (b & 0x7F) << shift
        shift += 7
        if not b & 0x80:
            if b & 0x40:
                out -= 1 << shift
            return out, i


def heaptype(buf, i):
    v, i = sleb(buf, i)
    if v < 0:
        return ABSTRACT_HEAP.get(v, "abstract(%d)" % v), i
    return v, i


def valtype(buf, i):
    """Decode a valtype. Only 0x63/0x64 carry a heap type payload; 0x70..0x67
    are self-contained shorthand references and consume exactly one byte --
    reading a payload after them desynchronises the whole type section (which
    is what made the first version of this report type 22 as garbage)."""
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
    if b in (0x63, 0x64):                      # ref null <ht> / ref <ht>
        ht, i = heaptype(buf, i)
        return ("ref null " if b == 0x63 else "ref ") + str(ht), i
    if b in SHORT_REFTYPE:
        return SHORT_REFTYPE[b], i
    raise ValueError("unknown valtype 0x%02X at %d" % (b, i - 1))


def storage_type(buf, i):
    """GC packed storage types sit where a valtype would."""
    if buf[i] in (0x78, 0x77):
        return {0x78: "i8", 0x77: "i16"}[buf[i]], i + 1
    return valtype(buf, i)


def parse_types(buf, start, size):
    """The section counts *rectypes*, but every subtype inside a rec group owns
    its own type index. Counting a whole rec group as one index shifts every
    later number, which is what made type 11928 decode as a func instead of the
    struct V8 says it is."""
    i = start
    count, i = leb(buf, i)
    types = []
    for _ in range(count):
        try:
            group, i = one_type(buf, i)
        except ValueError as e:
            raise ValueError("%s (after %d type indices)" % (e, len(types)))
        types.extend(group)
    return types


def one_type(buf, i):
    """-> (list of type descriptions, next index). A rec group yields one entry
    per member; everything else yields exactly one."""
    tag = buf[i]
    i += 1
    if tag == 0x4E:                             # rec group (TeaVM: bipush 78)
        n, i = leb(buf, i)
        out = []
        for _ in range(n):
            sub, i = one_type(buf, i)
            out.extend(sub)
        return out, i
    supers = None
    if tag in (0x4F, 0x50):                    # sub final / sub
        n, i = leb(buf, i)
        supers = []
        for _ in range(n):
            s, i = heaptype(buf, i)
            supers.append(str(s))
        tag = buf[i]
        i += 1
    prefix = "" if supers is None else "sub(%s) " % ",".join(supers)
    if tag == 0x5F:                             # struct (TeaVM TypeSectionParser case 95)
        n, i = leb(buf, i)
        fields = []
        for _ in range(n):
            t, i = storage_type(buf, i)
            mut = buf[i]
            i += 1
            fields.append(t + (" mut" if mut else ""))
        return ["%sstruct{%s}" % (prefix, ", ".join(fields))], i
    if tag == 0x5E:                             # array (TeaVM TypeSectionParser case 94)
        t, i = storage_type(buf, i)
        mut = buf[i]
        i += 1
        return ["%sarray{%s}" % (prefix, t + (" mut" if mut else ""))], i
    if tag == 0x60:
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
        return ["%sfunc(%s)->(%s)" % (prefix, ",".join(params), ",".join(rets))], i
    raise ValueError("unknown comptype 0x%02X at %d" % (tag, i - 1))


def parse_globals(buf, start, size):
    i = start
    count, i = leb(buf, i)
    out = []
    for _ in range(count):
        t, i = valtype(buf, i)
        mut = buf[i]
        i += 1
        # Init expr: a constant instruction sequence ended by 0x0B. Global
        # initialisers under wasm-GC can be struct.new/array.new_default, so
        # those carry a type immediate that must be consumed or we desync.
        while True:
            b = buf[i]
            if b == 0x0B:
                i += 1
                break
            if b == 0x41 or b == 0x42:
                _, i = sleb(buf, i + 1)
            elif b == 0x43:
                i += 5
            elif b == 0x44:
                i += 9
            elif b in (0x63, 0x64, 0xD0):
                _, i = heaptype(buf, i + 1)
            elif b == 0xD2:                    # ref.func
                _, i = leb(buf, i + 1)
            elif b == 0x23:                    # global.get
                _, i = leb(buf, i + 1)
            elif b == 0xFB:
                op = buf[i + 1]
                i += 2
                if op in (0x00, 0x01, 0x06, 0x07):
                    _, i = leb(buf, i)
                elif op in (0x08, 0x09, 0x0A):
                    _, i = leb(buf, i)
                    _, i = leb(buf, i)
                else:
                    raise ValueError("unhandled gc opcode 0x%02X in global "
                                     "init at %d" % (op, i - 2))
            else:
                raise ValueError("unhandled opcode 0x%02X in global init "
                                 "at %d" % (b, i))
        out.append((t, mut))
    return out


def sections(buf):
    i = 8
    while i < len(buf):
        sid = buf[i]
        i += 1
        size, i = leb(buf, i)
        yield sid, i, size
        i += size


def struct_sets(buf, body_start, body_size):
    """Every struct.set / global.get in a body, found by decoding LEBs at
    candidate prefixes. Bodies are short; a byte scan that only commits to a
    match when both immediates decode is good enough and avoids writing a full
    decoder for every opcode in the language."""
    i = body_start
    end = body_start + body_size
    locals_n, i = leb(buf, i)
    for _ in range(locals_n):
        _, i = leb(buf, i)
        i += 1
    found = []
    j = i
    while j < end:
        if buf[j] == 0x23:
            g, nxt = leb(buf, j + 1)
            found.append(("global.get", j, g, None, None))
            j = nxt
            continue
        if buf[j] == 0xFB and j + 1 < end and buf[j + 1] == 0x05:
            t, nxt = leb(buf, j + 2)
            f, nxt = leb(buf, nxt)
            found.append(("struct.set", j, None, t, f))
            j = nxt
            continue
        j += 1
    return i, found


def main():
    path = sys.argv[1]
    want_func = int(sys.argv[2]) if len(sys.argv) > 2 else None
    offset = int(sys.argv[3]) if len(sys.argv) > 3 else None

    buf = open(path, "rb").read()
    types, globals_, code, n_imports = [], [], None, 0
    for sid, start, size in sections(buf):
        if sid == 1:
            types = parse_types(buf, start, size)
        elif sid == 6:
            globals_ = parse_globals(buf, start, size)
        elif sid == 2:
            n_imports, _ = leb(buf, start)
        elif sid == 10:
            code = (start, size)

    print("types: %d   globals: %d   imports: %d" % (len(types), len(globals_), n_imports))
    for t in sorted({22, 11928, 12225, 7914, 8281, 1}):
        desc = types[t] if t < len(types) else "<oob>"
        if len(desc) > 400:
            desc = desc[:400] + " ...(%d chars)" % len(types[t])
        print("  type %-6s = %s" % (t, desc))

    start, size = code
    i = start
    n, i = leb(buf, i)
    bodies = []
    for _ in range(n):
        bsize, i = leb(buf, i)
        bodies.append((i, bsize))
        i += bsize

    target_func = None
    if offset is not None:
        for idx, (bs, sz) in enumerate(bodies):
            if bs <= offset < bs + sz:
                target_func = idx
                print("\n@+%d -> defined func #%d, function index #%d"
                      % (offset, idx, idx + n_imports))
                break
    if target_func is None and want_func is not None:
        target_func = want_func - n_imports

    if target_func is None:
        return
    bs, sz = bodies[target_func]
    locals_end, events = struct_sets(buf, bs, sz)
    print("body [%d,%d) locals_end=%d" % (bs, bs + sz, locals_end))
    print("events in this function:")
    for kind, at, g, t, f in events:
        if kind == "global.get":
            gt = globals_[g] if g < len(globals_) else ("<oob>", 0)
            print("  +%-8d global.get #%-6d : %s%s   %s"
                  % (at, g, gt[0], " mut" if gt[1] else "",
                     "   <== THE VALUE V8 REJECTED" if at == offset else ""))
        else:
            ft = types[t] if t < len(types) else "<oob>"
            mark = ""
            if t == 12225 or f == 1:
                mark = "   <== field type here is %s" % _field(types, t, f)
            print("  +%-8d struct.set type=%-6d field=%d : %s%s"
                  % (at, t, f, ft, mark))


def _field(types, t, f):
    desc = types[t] if t < len(types) else "?"
    at = desc.find("struct{")
    if at < 0:
        return desc
    inner = desc[at + len("struct{"):]
    inner = inner[:inner.rfind("}")]
    parts = [p.strip() for p in inner.split(",")]
    return parts[f] if f < len(parts) else "<oob>"


if __name__ == "__main__":
    main()
