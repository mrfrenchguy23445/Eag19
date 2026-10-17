#!/usr/bin/env python3
"""Keep exactly one function body real and mask every other body with
`unreachable; end`, preserving the full index space.

The result still compiles (a 3-byte body is valid for any signature), so
`wasm-tools print` can disassemble it and the one remaining real body is the
only thing to read. Used because wabt (wasm-objdump/wasm2wat) cannot decode GC
opcodes at all: "error: unexpected opcode: 0xfb".

usage: wasm-only-func.py <in.wasm> <out.wasm> <codeIndex>
"""
import sys

sys.path.insert(0, __file__.rsplit("/", 1)[0])
from importlib import import_module

_sweep = import_module("wasm-error-sweep-lib") if False else None


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


def parse_sections(buf):
    off = 8
    secs = []
    while off < len(buf):
        sid = buf[off]
        hdr = off
        off += 1
        size, payload = read_leb(buf, off)
        secs.append((sid, hdr, payload, size, payload + size))
        off = payload + size
    return secs


def main():
    if len(sys.argv) != 4:
        raise SystemExit(__doc__)
    src, dst, want = sys.argv[1], sys.argv[2], int(sys.argv[3])
    buf = open(src, "rb").read()

    sec = next((s for s in parse_sections(buf) if s[0] == 10), None)
    if sec is None:
        raise SystemExit("no code section")
    _, hdr, payload, size, end = sec

    o = payload
    count, o = read_leb(buf, o)
    if not 0 <= want < count:
        raise SystemExit("code index %d out of range 0..%d" % (want, count - 1))

    parts = [write_leb(count)]
    for i in range(count):
        sz, cs = read_leb(buf, o)
        if i == want:
            parts.append(write_leb(sz))
            parts.append(buf[cs:cs + sz])
        else:
            body = b"\x00\x00\x0b"
            parts.append(write_leb(len(body)))
            parts.append(body)
        o = cs + sz

    payload_bytes = b"".join(parts)
    out = buf[:hdr] + b"\x0a" + write_leb(len(payload_bytes)) + payload_bytes + buf[end:]
    open(dst, "wb").write(out)
    print("kept code[%d] real, masked %d bodies; %d -> %d bytes"
          % (want, count - 1, len(buf), len(out)))


if __name__ == "__main__":
    sys.exit(main())
