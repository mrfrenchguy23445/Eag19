#!/usr/bin/env python3
"""Aggregate a Chrome .cpuprofile and name-mapped wasm hotspots via classes.wasm.teadbg.

usage: python scripts/profile-deobfuscate.py PROFILE.cpuprofile TEADBG FILE [--top N]

V8's cpuprofile gives every wasm frame a columnNumber that is the byte offset in
the wasm module; decode-teadbg.py maps such addresses to Java methods using the
teadbg line table (teavm_line / teavm_mtd / teavm_cls / teavm_str sections).
"""
import bisect
import json
import sys


def uleb(b, p):
    r = 0
    s = 0
    while True:
        x = b[p]
        p += 1
        r |= (x & 0x7F) << s
        if not x & 0x80:
            return r, p
        s += 7


def sleb(b, p):
    r = 0
    s = 0
    while True:
        x = b[p]
        p += 1
        r |= (x & 0x7F) << s
        s += 7
        if not x & 0x80:
            if x & 0x40:
                r -= 1 << s
            return r, p


def section(name, data, out):
    """Parse a teadbg section payload; keys look like 'teavm_str','teavm_pkg','teavm_cls','teavm_mtd','teavm_line'."""
    out[name] = data


def load_teadbg(path):
    b = open(path, "rb").read()
    assert b[:4] == b"tdbg", "bad teadbg magic"
    p = 5
    sec = {}
    while p < len(b):
        n, p = uleb(b, p)
        name = b[p:p + n].decode()
        p += n
        dl, p = uleb(b, p)
        sec[name] = b[p:p + dl]
        p += dl
    return sec


def strings(d):
    out = []
    p = 0
    while p < len(d):
        n, p = uleb(d, p)
        out.append(d[p:p + n].decode("utf8", "replace"))
        p += n
    return out


def pairs(d):
    out = []
    p = 0
    while p < len(d):
        a, p = uleb(d, p)
        b, p = uleb(d, p)
        out.append((a, b))
    return out


def lines(d):
    """(startAddress, endAddress, methodIndex) sequence table from teavm_line."""
    seqs = []
    address = 0
    seq_start = None
    cur_method = None
    line = 1
    p = 0
    depth = 0
    while p < len(d):
        c = d[p]
        p += 1
        if c == 0:
            m, p = uleb(d, p)
            if cur_method is None:
                cur_method = m
                seq_start = address
            else:
                depth += 1
        elif c == 1:
            if depth > 0:
                depth -= 1
            else:
                if cur_method is not None:
                    seqs.append((seq_start, address, cur_method))
                cur_method = None
                seq_start = None
        elif c == 2:
            d2, p = sleb(d, p)
            line += d2
        elif c == 3:
            # moveToFile: payload is a file index; a byte 2 after it (line delta marker)
            _file, p = uleb(d, p)
            line = 1
            if p < len(d) and d[p] == 2:
                p += 1
                dd, p = sleb(d, p)
                line += dd
        elif c == 4:
            adv, p = uleb(d, p)
            address += adv
        elif c >= 10:
            v = c - 10
            address += v % 32
            line += (v // 32) - 3
        else:
            raise SystemExit(f"unknown line command {c} at {p-1}")
    if cur_method is not None:
        seqs.append((seq_start, address, cur_method))
    return seqs


class NameMap:
    def __init__(self, teadbg_path):
        sec = load_teadbg(teadbg_path)
        self.strs = strings(sec["teavm_str"])
        pkgs = pairs(sec["teavm_pkg"])
        self.clss = pairs(sec["teavm_cls"])
        self.mtds = pairs(sec["teavm_mtd"])
        self.seqs = lines(sec["teavm_line"])
        self.seqs.sort(key=lambda s: s[0])
        self.starts = [s[0] for s in self.seqs]
        self._pkg_cache = {0: ""}
        self._pkg = pkgs

    def pkg_name(self, i):
        if i in self._pkg_cache:
            return self._pkg_cache[i]
        parent, nidx = self._pkg[i - 1]
        name = self.strs[nidx]
        pn = self.pkg_name(parent)
        full = (pn + "." if pn else "") + name
        self._pkg_cache[i] = full
        return full

    def cls_name(self, i):
        pi, ni = self.clss[i]
        pn = self.pkg_name(pi)
        return (pn + "." if pn else "") + self.strs[ni]

    def method_at(self, addr):
        i = bisect.bisect_right(self.starts, addr) - 1
        if i < 0:
            return None
        st, en, m = self.seqs[i]
        if not (st <= addr < en):
            return None
        ci, ni = self.mtds[m]
        return self.cls_name(ci), self.strs[ni], st, en


def main():
    prof_path = sys.argv[1]
    teadbg_path = sys.argv[2] if len(sys.argv) > 2 else "target_teavm_wasm_gc/javascript/classes.wasm.teadbg"
    top = int(sys.argv[4]) if len(sys.argv) > 4 else 50
    prof = json.load(open(prof_path, encoding="utf8"))

    nodes = {n["id"]: n for n in prof["nodes"]}
    samples = prof.get("samples", [])
    deltas = prof.get("timeDeltas", [])
    total_ms = sum(deltas) / 1000.0

    self_us = {}
    for sid, dt in zip(samples, deltas):
        self_us[sid] = self_us.get(sid, 0) + dt

    nm = NameMap(teadbg_path)
    by_method = {}
    by_node_kind = {}
    misses = 0
    for sid, us in self_us.items():
        n = nodes.get(sid)
        if not n:
            continue
        cf = n.get("callFrame", {})
        fname = cf.get("functionName", "?")
        if fname.startswith("wasm-function[") and cf.get("columnNumber", -1) >= 0:
            addr = cf["columnNumber"]
            info = nm.method_at(addr)
            if info:
                cls, method, st, en = info
                key = f"{cls}.{method}"
            else:
                misses += 1
                key = fname + f" (no name for addr {addr})"
        elif fname.startswith("("):
            key = fname
        elif cf.get("url", "").endswith(".js"):
            key = "[js] " + (fname or cf["url"].split("/")[-1])
        else:
            key = fname or "[native]"
        by_method[key] = by_method.get(key, 0) + us

    def render(k):
        us = by_method[k]
        return f"{us/1000.0:9.1f} ms  {100*us/total_us:5.1f}%  {k}"

    total_us = sum(by_method.values())
    print(f"total sampled: {total_us/1000.0:.1f} ms over {total_ms:.0f} ms profile, "
          f"{len(by_method)} distinct functions")
    print()
    print("=== top self-time ===")
    for k, _ in sorted(by_method.items(), key=lambda kv: -kv[1])[:top]:
        print(render(k))
    print(f"\nunnamed wasm frames (addr not in teadbg): {misses}")


if __name__ == "__main__":
    main()
