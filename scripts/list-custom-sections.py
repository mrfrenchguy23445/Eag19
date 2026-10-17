#!/usr/bin/env python3
"""List every custom section (id 0) name + size in a wasm module."""
import sys


def uleb(b, p):
    r = 0
    s = 0
    while True:
        x = b[p]
        p += 1
        r |= (x & 0x7F) << s
        if not (x & 0x80):
            return r, p
        s += 7


def main(path):
    b = open(path, "rb").read()
    assert b[:4] == b"\0asm", "not wasm"
    p = 8
    while p < len(b):
        sid = b[p]
        p += 1
        size, p = uleb(b, p)
        end = p + size
        if sid == 0:
            nlen, q = uleb(b, p)
            name = b[q:q + nlen].decode("utf8", "replace")
            print(f"{name:<24} id=0 size={size} at=0x{p:x}")
        p = end


if __name__ == "__main__":
    main(sys.argv[1])
