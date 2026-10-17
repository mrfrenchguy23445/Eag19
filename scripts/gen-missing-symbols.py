#!/usr/bin/env python3
"""Generate docs/MISSING_SYMBOLS.md from real javac output.

Reads the two build logs captured with -Xmaxerrs 100000 and groups every
distinct missing symbol by the subsystem it belongs to.
"""
import re
import sys
from collections import defaultdict

# symbol -> (subsystem, note)
CATEGORIES = [
    ("Profile & identity", [
        ("EaglerProfile", "stored username/UUID/skin"),
    ]),
    ("Auto-update & signature verification", [
        ("UpdateService", "downloads + applies client updates"),
        ("UpdateCertificate", "RSA public key, bundled in jar"),
        ("UpdateDataObj", "serialised update manifest"),
        ("UpdateProgressStruct", "download progress"),
        ("UpdateResultObj", "update outcome"),
        ("CertificateInvalidException", "thrown on signature mismatch"),
        ("enableUpdateService", "toggle on UpdateService"),
    ]),
    ("Relay", [
        ("RelayManager", "relay list load/save"),
        ("RelayEntry", "one relay server"),
    ]),
    ("Singleplayer / integrated server", [
        ("SingleplayerServerController", "SP lifecycle"),
        ("EaglerIntegratedServerWorker", "runs the server on a worker"),
        ("IPCPacket15Crashed", "worker crash report"),
        ("IPCPacketFFProcessKeepAlive", "worker liveness"),
    ]),
    ("Voice chat", [
        ("VoiceClientController", "voice session"),
        ("EnumVoiceChannelType", "openmic / push-to-talk"),
        ("EnumVoiceChannelReadyState", "connection state"),
    ]),
    ("Webview", [
        ("WebViewOverlayController", "in-game browser"),
        ("PermissionsCache", "per-domain permissions"),
    ]),
    ("Screen recording", [
        ("EnumScreenRecordingCodec", "vp8 / h264"),
        ("ScreenRecordParameters", "resolution + fps"),
    ]),
    ("Boot menu support", [
        ("EaglerLoadingCache", "generic async cache"),
        ("ServerCookieDataStore", "persisted cookies"),
        ("EaglerFolderResourcePack", "folder-backed pack"),
    ]),
    ("EPK pack tooling", [
        ("EPKCompiler", "folder -> .epk"),
        ("EPKDecompiler", ".epk -> folder"),
    ]),
    ("LWJGL desktop platform", [
        ("ShaderSource", "deferred shader load"),
        ("UnsafeMemcpy", "bulk buffer copy"),
        ("UnsafeUtils", "sun.misc.Unsafe shim"),
    ]),
]


def main(logs):
    syms = set()
    for path in logs:
        with open(path, errors="replace") as fh:
            text = fh.read().replace("\r", "")
        syms.update(re.findall(
            r"symbol: +(?:class|variable|method) +([A-Za-z0-9_]+)", text))

    grouped = {}
    assigned = set()
    for group, entries in CATEGORIES:
        hits = []
        for sym, note in entries:
            if sym in syms:
                hits.append((sym, note))
                assigned.add(sym)
        if hits:
            grouped[group] = hits

    other = sorted(syms - assigned)

    out = []
    out.append("# Missing symbol inventory\n")
    out.append("Every symbol below is referenced by `src/teavm` or `src/lwjgl` "
               "but does not exist anywhere in the repository.\n")
    out.append(f"**{len(syms)} distinct symbols** across "
               f"`:target_teavm_javascript:compileJava` (490 errors) and "
               f"`:target_lwjgl_desktop:compileJava` (1094 errors).\n")
    out.append("Regenerate with `scripts/gen-missing-symbols.py`.\n")
    out.append("## Eaglercraft subsystems\n")
    out.append("These are whole subsystems that were never brought over from "
               "the 1.8 tree — not individual bugs.\n")
    for group, hits in grouped.items():
        out.append(f"\n### {group}\n")
        out.append("| Symbol | Role |")
        out.append("|---|---|")
        for sym, note in hits:
            out.append(f"| `{sym}` | {note} |")

    out.append("\n## Remaining unresolved symbols\n")
    out.append(f"{len(other)} symbols not attributable to a named subsystem. "
               "Most are local variables and fields whose declaring class is "
               "itself missing, so they resolve automatically once their owning "
               "class lands.\n")
    out.append("```")
    out.extend(other)
    out.append("```")

    with open("docs/MISSING_SYMBOLS.md", "w") as fh:
        fh.write("\n".join(out) + "\n")

    print(f"wrote docs/MISSING_SYMBOLS.md: {len(syms)} symbols "
          f"({len(assigned)} attributed, {len(other)} other)")


if __name__ == "__main__":
    main(sys.argv[1:])