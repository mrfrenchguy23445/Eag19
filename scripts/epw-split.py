#!/usr/bin/env python3
"""Split an EAG$WASM EPW bundle: dump each assets EPK entry (xz-decompressed)
plus the string slices, so the assets can be repacked into a normal EPK.
usage: python3 epw-split.py <file.epw> <outdir>
"""
import lzma, os, struct, sys

path, outdir = sys.argv[1], sys.argv[2]
buf = open(path, "rb").read()
assert len(buf) >= 384
magic = buf[0:8]
assert magic == b"EAG$WASM", magic
fileLength, fileCRC32, vMaj, vMin, clientVer = struct.unpack_from("<IIHHI", buf, 8)
numEPKs, = struct.unpack_from("<I", buf, 96)
os.makedirs(outdir, exist_ok=True)

def sl(off):
    a, b = struct.unpack_from("<II", buf, off)
    return a, b

# slice offsets (epw_header layout)
sp    = sl(100)
spm   = sl(108)
pak   = sl(116)
pakm  = sl(124)
crash = sl(132)
crashm= sl(140)
fav   = sl(148)
favm  = sl(156)
meta  = sl(80)

def dump(name, off, ln):
    open(os.path.join(outdir, name), "wb").write(buf[off:off+ln])
    print(f"{name}: {ln} bytes @{off}")

dump("meta.txt", meta[0], meta[1])
dump("splash.png", sp[0], sp[1])
dump("pressAnyKey.png", pak[0], pak[1])
dump("crash.png", crash[0], crash[1])
dump("favicon.png", fav[0], fav[1])

base = 276
hdrLen = ((276 + 32 * numEPKs) + 127) & ~127
print(f"numEPKs={numEPKs} hdrLen={hdrLen} fileLen={fileLength} totalRead={len(buf)}")
for i in range(numEPKs):
    e = base + i * 32
    fpOfs, fpLen = struct.unpack_from("<II", buf, e)
    lpOfs, lpLen = struct.unpack_from("<II", buf, e + 8)
    fOfs, compLen, decLen = struct.unpack_from("<III", buf, e + 16)
    filePath = buf[fpOfs:fpOfs+fpLen].decode("utf-8", "replace")
    loadPath = buf[lpOfs:lpOfs+lpLen].decode("utf-8", "replace")
    comp = buf[fOfs:fOfs+compLen]
    dec = lzma.decompress(comp) if compLen else b""
    assert len(dec) == decLen, (len(dec), decLen)
    safe = f"epk{i}_fp={filePath.replace('/','_')}_lp={loadPath.replace('/','_') or 'root'}.epk"
    open(os.path.join(outdir, safe), "wb").write(dec)
    print(f"#{i} filePath={filePath!r} loadPath={loadPath!r} comp={compLen} dec={decLen} -> {safe}")
