#!/usr/bin/env python3
"""Authoritative wasm-GC type / global decoder, transcribed from TeaVM's own parser.

Format source (extracted from teavm-core-0.12.1-EAGLER-R3-sources.jar):

    org.teavm.backend.wasm.parser.TypeSectionParser
    org.teavm.backend.wasm.parser.WasmBinaryReader

Two rules that earlier scripts got wrong and that decide everything:

  * heap types are read by WasmBinaryReader.readHeapType():
        if ((typeId & 0xC0) == 0x40) -> one byte, abstract heap type
        else                          -> UNSIGNED LEB128, concrete type index
    A signed LEB read of index 11928 (bytes 98 5D) yields -4456, which is where
    the bogus "abstract(-4459)" supertypes came from.

  * the type-section count counts *rectypes*, and a rec group (0x4E) holds a
    vec of subtypes.  Each subtype, not each rectype, owns a type index, so rec
    members must be flattened before looking an index up.

Array is 0x5E (the spec's comptype tags are 0x5E array / 0x5F struct / 0x60
func); reading 0x61 as an array desynchronises the whole section.

    python3 scripts/gc-types.py classes.wasm 22 11928 12225
    python3 scripts/gc-types.py classes.wasm --global 23837
"""
import sys

SHORT_REFTYPE = {
    0x70: "func", 0x6F: "extern", 0x6E: "any", 0x6D: "eq",
    0x6C: "i31", 0x6B: "struct", 0x6A: "array", 0x73: "exn",
}
ABSTRACT = {
    -16: "func", -17: "extern", -18: "any", -19: "eq", -20: "i31",
    -21: "struct", -22: "array", -23: "none", -24: "noextern",
    -25: "nofunc", -26: "exn", -27: "noexn",
}


class Reader:
    def __init__(self, buf, pos, end):
        self.buf = buf
        self.p = pos
        self.end = end

    def uleb(self):
        out = 0
        shift = 0
        while True:
            b = self.buf[self.p]
            self.p += 1
            out |= (b & 0x7F) << shift
            if not b & 0x80:
                return out
            shift += 7

    def heaptype(self):
        """WasmBinaryReader.readHeapType()."""
        b = self.buf[self.p]
        if (b & 0xC0) == 0x40:
            self.p += 1
            # single-byte negative s33 -> abstract heap type
            v = b if b < 0x40 else b - 0x80
            return ABSTRACT.get(v, "abstract(%d)" % v)
        return self.uleb()

    def valtype(self):
        """WasmBinaryReader.readType()."""
        b = self.buf[self.p]
        self.p += 1
        if b == 0x7F:
            return "i32"
        if b == 0x7E:
            return "i64"
        if b == 0x7D:
            return "f32"
        if b == 0x7C:
            return "f64"
        if b == 0x63:
            return "ref null %s" % self.heaptype()
        if b == 0x64:
            return "ref %s" % self.heaptype()
        if b == 0x40:
            return "none"
        if b in SHORT_REFTYPE:
            return "%sref" % SHORT_REFTYPE[b]
        raise ValueError("unknown valtype 0x%02X at %d" % (b, self.p - 1))

    def storage(self):
        """WasmBinaryReader.readStorageType()."""
        b = self.buf[self.p]
        if b == 0x78:
            self.p += 1
            return "i8"
        if b == 0x77:
            self.p += 1
            return "i16"
        return self.valtype()


def parse_types(buf, pos, end):
    r = Reader(buf, pos, end)
    count = r.uleb()
    out = []          # flat, one entry per type index
    groups = []       # (first_index, size) for each rec group

    def one_sub():
        i = len(out)
        tag = buf[r.p]
        if tag == 0x50:
            r.p += 1
            supers = [r.heaptype() for _ in range(r.uleb())]
            head = "sub final(%s)" % ",".join(map(str, supers))
        elif tag == 0x4F:
            r.p += 1
            supers = [r.heaptype() for _ in range(r.uleb())]
            head = "sub(%s)" % ",".join(map(str, supers))
        else:
            head = None                      # bare = final, no supers
        return i, head

    while r.p < end and len(groups) < count:
        if buf[r.p] == 0x4E:
            r.p += 1
            n = r.uleb()
            first = len(out)
            heads = []
            for _ in range(n):
                i, head = one_sub()
                heads.append(head)
                out.append(composite(r, head))
            groups.append((first, n, heads))
        else:
            i, head = one_sub()
            out.append(composite(r, head))
            groups.append((i, 1, [head]))
    return out, groups


def composite(r, head):
    tag = r.buf[r.p]
    r.p += 1
    if tag == 0x5F:
        n = r.uleb()
        fields = []
        for _ in range(n):
            t = r.storage()
            mut = r.buf[r.p]
            r.p += 1
            fields.append(t + (" mut" if mut else ""))
        body = "struct{%s}" % ", ".join(fields)
    elif tag == 0x5E:
        t = r.storage()
        mut = r.buf[r.p]
        r.p += 1
        body = "array{%s%s}" % (t, " mut" if mut else "")
    elif tag == 0x60:
        n = r.uleb()
        params = [r.valtype() for _ in range(n)]
        n = r.uleb()
        rets = [r.valtype() for _ in range(n)]
        body = "func(%s)->(%s)" % (",".join(params), ",".join(rets))
    else:
        raise ValueError("unknown comptype 0x%02X at %d" % (tag, r.p - 1))
    return "%s %s" % (head, body) if head else body


def parse_globals(buf, pos, end):
    """Return list of (type, mut, init_bytes_hex) in module-global order."""
    r = Reader(buf, pos, end)
    n = r.uleb()
    out = []
    for _ in range(n):
        t = r.valtype()
        mut = r.buf[r.p]
        r.p += 1
        start = r.p
        depth = 0
        while True:
            b = r.buf[r.p]
            r.p += 1
            if b == 0x0B:
                depth -= 1
                if depth < 0:
                    break
            elif b in (0x02, 0x03, 0x04):
                depth += 1
        out.append((t + (" mut" if mut else ""), buf[start:r.p - 1].hex(" ")))
    return out


def count_imported(buf, pos, end, kind):
    r = Reader(buf, pos, end)
    n = r.uleb()
    c = 0
    for _ in range(n):
        r.uleb()          # module name
        r.uleb()          # field name
        k = r.buf[r.p]
        r.p += 1
        if k == 0x00:
            r.uleb()
        elif k == 0x01:
            r.p += 1
            if buf[r.p - 1] == 0x02:
                r.p += 1
        elif k == 0x02:
            r.p += 2
        elif k == 0x03:
            r.valtype()
            r.p += 1
        elif k == 0x04:
            r.uleb()
            r.p += 1
        if k == kind:
            c += 1
    return c


def main():
    path = sys.argv[1]
    want = []
    want_glob = None
    args = sys.argv[2:]
    i = 0
    while i < len(args):
        if args[i] == "--global":
            want_glob = int(args[i + 1])
            i += 2
        else:
            want.append(int(args[i]))
            i += 1

    buf = open(path, "rb").read()
    secs = {}
    order = []
    p = 8
    while p < len(buf):
        sid = buf[p]
        p += 1
        n = 0
        shift = 0
        while True:
            b = buf[p]
            p += 1
            n |= (b & 0x7F) << shift
            if not b & 0x80:
                break
            shift += 7
        secs.setdefault(sid, []).append((p, n))
        order.append((sid, p, n))
        p += n

    tstart, tsize = secs[1][0]
    types, groups = parse_types(buf, tstart, tstart + tsize)
    print("type index space: %d entries in %d rectypes" % (len(types), len(groups)))
    multi = [g for g in groups if g[1] > 1]
    print("rec groups with >1 member: %d%s" % (
        len(multi), ("  (first: %s)" % (multi[0],)) if multi else ""))

    if want_glob is not None:
        imported = count_imported(buf, *secs[2][0], 0x03)
        gi = secs[6][0]
        globs = parse_globals(buf, gi[0], gi[0] + gi[1])
        print("global section: %d entries (+%d imported)" % (len(globs), imported))
        idx = want_glob - imported
        print("global #%d -> section entry #%d" % (want_glob, idx))
        if 0 <= idx < len(globs):
            print("  type: %s" % globs[idx][0])
            print("  init: %s" % globs[idx][1])

    for t in want:
        if t < len(types):
            print("  type %-6d = %s" % (t, types[t]))
        else:
            print("  type %-6d = <out of range>" % t)


if __name__ == "__main__":
    main()
