#!/usr/bin/env python3
"""Rename a single wasm import without rebuilding.

WebAssembly links by exact (module, name) string, so a one-character mismatch
between the Java @Import annotation and the JS import object is a hard LinkError
and V8 reports only the first one. Patching the name here lets a hypothesis be
boot-tested in seconds instead of costing a 12-minute generateWasmGC link.

usage: wasm-rename-import.py <in.wasm> <out.wasm> <module> <oldName> <newName>
exit 1 if the import is not declared.
"""
import sys


def read_leb(buf, off):
    r = s = 0
    while True:
        b = buf[off]
        off += 1
        r |= (b & 0x7F) << s
        s += 7
        if not (b & 0x80):
            return r, off


def write_leb(n):
    out = bytearray()
    while True:
        b = n & 0x7F
        n >>= 7
        if n:
            b |= 0x80
        out.append(b)
        if not n:
            return bytes(out)


def read_name(buf, off):
    n, off = read_leb(buf, off)
    return buf[off:off + n], off + n


def main():
    if len(sys.argv) != 6:
        raise SystemExit(__doc__)
    src, dst, want_mod, old, new = sys.argv[1:6]
    old, new = old.encode(), new.encode()
    buf = open(src, "rb").read()

    off = 8
    while off < len(buf):
        sid = buf[off]
        hdr = off
        off += 1
        size, payload = read_leb(buf, off)
        end = payload + size
        if sid == 2:
            break
        off = end
    else:
        raise SystemExit("no import section")

    count, o = read_leb(buf, payload)
    parts = [write_leb(count)]
    hits = 0
    for _ in range(count):
        mod, o = read_name(buf, o)
        name, o = read_name(buf, o)
        if mod == want_mod.encode() and name == old:
            name = new
            hits += 1
        parts.append(write_leb(len(mod)) + mod)
        parts.append(write_leb(len(name)) + name)
        kind = buf[o]
        o += 1
        parts.append(bytes([kind]))
        if kind == 0:
            t, o = read_leb(buf, o)
            parts.append(write_leb(t))
        elif kind == 1:
            parts.append(buf[o:o + 1])
            o += 1
            flags, o = read_leb(buf, o)
            mn, o = read_leb(buf, o)
            parts += [write_leb(flags), write_leb(mn)]
            if flags & 1:
                mx, o = read_leb(buf, o)
                parts.append(write_leb(mx))
        elif kind == 2:
            flags, o = read_leb(buf, o)
            mn, o = read_leb(buf, o)
            parts += [write_leb(flags), write_leb(mn)]
            if flags & 1:
                mx, o = read_leb(buf, o)
                parts.append(write_leb(mx))
        elif kind == 3:
            parts.append(buf[o:o + 2])
            o += 2
        elif kind == 4:
            parts.append(buf[o:o + 1])
            o += 1
            t, o = read_leb(buf, o)
            parts.append(write_leb(t))
        else:
            raise SystemExit("unknown import kind %d" % kind)
    if hits == 0:
        raise SystemExit("import not found: %s.%s" % (want_mod, old))

    payload_bytes = b"".join(parts)
    out = buf[:hdr] + bytes([2]) + write_leb(len(payload_bytes)) + payload_bytes + buf[end:]
    open(dst, "wb").write(out)
    print("renamed %s.%s -> %s (%d hit); %d -> %d bytes"
          % (want_mod, old.decode(), new.decode(), hits, len(buf), len(out)))


if __name__ == "__main__":
    sys.exit(main())
