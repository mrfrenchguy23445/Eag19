#!/usr/bin/env python3
"""Replace the bodies of specific functions with a blunt stub, keeping the
module's index space (types, imports, exports, tables) fully intact.

This is the sanctioned "whole-method stub" escape hatch: V8 rejects an entire
wasm module if *any* function body fails to typecheck, so one bad body means
nothing runs. Replacing just that body with a stub makes the whole module
valid while leaving every other function byte-identical.

Three stub kinds, chosen per function:

  i32   body = 0 locals, i32.const 0, end   -- returns default instead of trapping
  none  body = 0 locals, end                -- plain void return
  trap  body = 0 locals, unreachable, end   -- valid for ANY signature, but traps

`i32`/`none` are strongly preferred over `trap`: a trapped call unwinds into
the game's exception handling and typically kills the boot, whereas a default
return just gives subtly wrong behaviour that the title screen never notices.

OFFSET PRESERVATION MATTERS. The stack deobfuscator maps runtime code addresses
through the external `.teadbg` sidecar, which records byte offsets into the
module. If stubbing shifts any offset, every deobfuscated frame points at the
wrong method and the crash report becomes nonsense. So this script is careful
to leave the module byte-identical apart from the two stub payloads:

  * body-size LEBs are re-padded to their ORIGINAL byte width (TeaVM writes
    them wider than minimal), so no size field changes length;
  * each stub is padded out with `nop` to the exact size of the body it
    replaces, so the code section payload keeps its total length.

usage: stub-wasm-funcs.py <in.wasm> <out.wasm> <codeIdx:kind> [codeIdx:kind ...]
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


def write_leb_padded(n, minlen):
    """Encode `n` using at least `minlen` bytes, so re-encoding a size field
    cannot change its width and shift every offset after it."""
    out = bytearray()
    while True:
        b = n & 0x7F
        n >>= 7
        if n:
            b |= 0x80
        out.append(b)
        if not n:
            break
    while len(out) < minlen:
        # continuation bit on every byte except the last
        out[-1] |= 0x80
        out.append(0x00)
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


STUBS = {
    "i32": b"\x00\x41\x00\x0b",
    "none": b"\x00\x0b",
    "trap": b"\x00\x00\x0b",
}


def main():
    if len(sys.argv) < 4:
        raise SystemExit(__doc__)
    src, dst = sys.argv[1], sys.argv[2]
    wanted = {}
    for spec in sys.argv[3:]:
        idx, _, kind = spec.partition(":")
        if kind not in STUBS:
            raise SystemExit("bad kind %r (want one of %s)" % (kind, "/".join(STUBS)))
        wanted[int(idx)] = STUBS[kind]
    buf = open(src, "rb").read()

    sec = next((s for s in parse_sections(buf) if s[0] == 10), None)
    if sec is None:
        raise SystemExit("no code section")
    _, hdr, payload, size, end = sec

    o = payload
    count, o = read_leb(buf, o)
    for idx in wanted:
        if not 0 <= idx < count:
            raise SystemExit("code index %d out of range 0..%d" % (idx, count - 1))

    parts = [write_leb(count)]
    done = 0
    for i in range(count):
        sz, cs = read_leb(buf, o)
        size_width = cs - o          # original byte width of this body's size LEB
        body = wanted.get(i)
        if body is None:
            parts.append(write_leb_padded(sz, size_width))
            parts.append(buf[cs:cs + sz])
        else:
            if len(body) > sz:
                raise SystemExit(
                    "code[%d]: stub (%d bytes) larger than body (%d) - cannot pad"
                    % (i, len(body), sz))
            # pad with nop so the body keeps the exact size of the one it replaces
            stub = body[:-1] + b"\x01" * (sz - len(body)) + body[-1:]
            parts.append(write_leb_padded(sz, size_width))
            parts.append(stub)
            done += 1
        o = cs + sz

    payload_bytes = b"".join(parts)
    out = buf[:hdr] + b"\x0a" + write_leb(len(payload_bytes)) + payload_bytes + buf[end:]
    open(dst, "wb").write(out)
    print("stubbed %d/%d bodies; %d -> %d bytes" % (done, count, len(buf), len(out)))


if __name__ == "__main__":
    sys.exit(main())
