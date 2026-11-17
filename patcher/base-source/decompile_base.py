#!/usr/bin/env python3
"""Build the vanilla Minecraft 1.19.4 Java tree used as the patcher base.

The patcher never ships decompiled Minecraft source. This helper recreates the
exact base tree from inputs you own:

* the official ``client.jar`` for 1.19.4,
* Mojang's official ``client.txt`` ProGuard mappings for the same version,
* pinned SpecialSource and Vineflower releases from Maven Central.

The jar is remapped to Mojang's official class, method and field names, then
decompiled with pinned settings. Only the packages the Eag19 bundle touches are
kept: ``net/minecraft`` and ``com/mojang/{blaze3d,realmsclient,math}``.

Everything is byte-for-byte reproducible for a given set of inputs, which is
what lets the source-patch bundle bind to the base tree's manifest hash.

Example::

    python3 patcher/base-source/decompile_base.py \
      --client-jar client.jar \
      --expected-client-jar-sha256 <sha256> \
      --mappings client.txt \
      --expected-mappings-sha256 <sha256> \
      --output /tmp/1.19.4-java

Tool pins are checked unconditionally; pass ``--specialsource-jar`` and
``--vineflower-jar`` only to point at local copies of the pinned releases.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "source-patches"))

import export_source_patches as patches  # noqa: E402

DEFAULT_SPECIALSOURCE_SHA256 = (
    "afabcba81cd474c5b21abdfe1bbc3888f70a32e782c03bffe13510078fa45a26"
)
DEFAULT_VINEFLOWER_SHA256 = (
    "1dfcfe974395734fa467ce620661c7623d05ba83670de0529b1fbd63ff548b9d"
)
DEFAULT_SPECIALSOURCE_URL = (
    "https://repo1.maven.org/maven2/net/md-5/SpecialSource/1.11.6/"
    "SpecialSource-1.11.6-shaded.jar"
)
DEFAULT_VINEFLOWER_URL = (
    "https://repo1.maven.org/maven2/org/vineflower/vineflower/1.12.0/"
    "vineflower-1.12.0.jar"
)

KEEP_PREFIXES = (
    "net/minecraft",
    "com/mojang/blaze3d",
    "com/mojang/realmsclient",
    "com/mojang/math",
)

# Kept identical between releases so the generated tree, and therefore the
# bundle's base manifest, stays reproducible.
VINEFLOWER_ARGS = ("-dgs=1", "-hdc=0", "-asc=1", "-rsy=1", "-log=WARN")
SPECIALSOURCE_ARGS = ("--kill-lvt",)

CHUNK_SIZE = 1024 * 1024


class DecompileError(RuntimeError):
    pass


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(CHUNK_SIZE), b""):
            digest.update(chunk)
    return digest.hexdigest()


def require_sha256(value: str, *, label: str) -> str:
    value = value.strip().lower()
    if len(value) != 64 or any(ch not in "0123456789abcdef" for ch in value):
        raise DecompileError(f"{label} must be a SHA-256 hex digest")
    return value


def verify_file(path: Path, expected: str, *, label: str) -> None:
    if not path.is_file():
        raise DecompileError(f"{label} does not exist: {path}")
    actual = sha256_file(path)
    if actual != expected:
        raise DecompileError(f"{label} SHA-256 mismatch: expected {expected}, got {actual}")


def java_executable(java_home: str | None) -> str:
    if java_home:
        candidate = Path(java_home) / "bin" / ("java.exe" if os.name == "nt" else "java")
        if not candidate.is_file():
            raise DecompileError(f"java not found under --java-home: {candidate}")
        return str(candidate)
    found = shutil.which("java")
    if not found:
        raise DecompileError("java is not on PATH; set --java-home or install a JDK")
    return found


def run(command: list[str], *, label: str, log: Path) -> None:
    log.parent.mkdir(parents=True, exist_ok=True)
    with log.open("wb") as stream:
        result = subprocess.run(command, stdout=stream, stderr=subprocess.STDOUT)
    if result.returncode != 0:
        tail = log.read_text(encoding="utf-8", errors="replace").splitlines()[-25:]
        raise DecompileError(
            f"{label} failed with exit code {result.returncode}:\n" + "\n".join(tail)
        )


def keep(rel: str) -> bool:
    return any(rel == prefix or rel.startswith(prefix + "/") for prefix in KEEP_PREFIXES)


def collect_tree(decompiled: Path, destination: Path) -> int:
    if not decompiled.is_dir():
        raise DecompileError(f"decompiler produced no output directory: {decompiled}")
    count = 0
    for current, dirs, files in os.walk(decompiled, followlinks=False):
        dirs[:] = sorted(dirs)
        for name in sorted(files):
            source = Path(current) / name
            if source.is_symlink():
                raise DecompileError(f"decompiler produced a symlink: {source}")
            rel = source.relative_to(decompiled).as_posix()
            if not rel.endswith(".java") or not keep(rel):
                continue
            target = destination / Path(*rel.split("/"))
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(source, target)
            patch_rel = patches.normalize_rel(rel, field="decompiled path")
            if patch_rel != rel:
                raise DecompileError(f"decompiled path is not canonical: {rel}")
            count += 1
    if count == 0:
        raise DecompileError("no Minecraft packages were found in the decompiler output")
    return count


def manifest_digest(entries: dict[str, dict[str, object]]) -> str:
    return patches.sha256_bytes(
        patches.canonical_json(patches.manifest_object("vanilla-source", entries))
    )


def decompile(args: argparse.Namespace) -> dict[str, object]:
    output = Path(args.output)
    if output.exists() or output.is_symlink():
        raise DecompileError(f"output must be absent (refusing overwrite): {output}")
    output.parent.mkdir(parents=True, exist_ok=True)

    client_jar = Path(args.client_jar)
    mappings = Path(args.mappings)
    specialsource = Path(args.specialsource_jar)
    vineflower = Path(args.vineflower_jar)

    verify_file(
        client_jar,
        require_sha256(args.expected_client_jar_sha256, label="client jar"),
        label="client jar",
    )
    verify_file(
        mappings,
        require_sha256(args.expected_mappings_sha256, label="mappings"),
        label="mappings",
    )
    verify_file(specialsource, DEFAULT_SPECIALSOURCE_SHA256, label="SpecialSource")
    verify_file(vineflower, DEFAULT_VINEFLOWER_SHA256, label="Vineflower")

    java = java_executable(args.java_home)
    staging = Path(tempfile.mkdtemp(prefix=f".{output.name}.staging-", dir=str(output.parent)))
    try:
        named = staging / "client-named.jar"
        run(
            [
                java,
                f"-Xmx{args.remap_heap}",
                "-jar",
                str(specialsource),
                "--in-jar",
                str(client_jar),
                "--out-jar",
                str(named),
                "--srg-in",
                str(mappings),
                *SPECIALSOURCE_ARGS,
            ],
            label="SpecialSource remap",
            log=staging / "remap.log",
        )
        if not named.is_file() or named.stat().st_size == 0:
            raise DecompileError("SpecialSource did not write a remapped jar")

        decompiled = staging / "decompiled"
        run(
            [
                java,
                f"-Xmx{args.decompile_heap}",
                "-jar",
                str(vineflower),
                *VINEFLOWER_ARGS,
                str(named),
                str(decompiled),
            ],
            label="Vineflower decompile",
            log=staging / "decompile.log",
        )

        base = staging / "base"
        count = collect_tree(decompiled, base)
        entries = patches.scan_source(base)
        digest = manifest_digest(entries)
        if output.exists() or output.is_symlink():
            raise DecompileError(f"output appeared while decompiling: {output}")
        os.replace(base, output)
        return {
            "status": "decompiled",
            "output": str(output),
            "base_file_count": count,
            "base_manifest_sha256": digest,
            "specialsource_sha256": DEFAULT_SPECIALSOURCE_SHA256,
            "vineflower_sha256": DEFAULT_VINEFLOWER_SHA256,
        }
    finally:
        if staging.exists():
            shutil.rmtree(staging, ignore_errors=True)


def cli() -> int:
    here = Path(__file__).resolve().parent
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--client-jar", required=True, type=Path)
    parser.add_argument("--expected-client-jar-sha256", required=True)
    parser.add_argument("--mappings", required=True, type=Path)
    parser.add_argument("--expected-mappings-sha256", required=True)
    parser.add_argument("--specialsource-jar", type=Path, default=here / "SpecialSource-1.11.6-shaded.jar")
    parser.add_argument("--vineflower-jar", type=Path, default=here / "vineflower-1.12.0.jar")
    parser.add_argument("--java-home")
    parser.add_argument("--remap-heap", default="2G")
    parser.add_argument("--decompile-heap", default="6G")
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()
    try:
        result = decompile(args)
    except (DecompileError, patches.BundleError) as exc:
        print(f"error: {exc}", file=sys.stderr)
        return 1
    print(json.dumps(result, indent=2, sort_keys=True))
    return 0


if __name__ == "__main__":
    sys.exit(cli())
