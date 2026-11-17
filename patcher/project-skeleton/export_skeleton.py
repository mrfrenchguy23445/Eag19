#!/usr/bin/env python3
"""Build the deterministic project skeleton used by the Eag19 patcher.

The skeleton contains every tracked file that is not Minecraft-derived: build
scripts, the TeaVM targets, the browser platform code, vendored third-party
sources, and documentation. It deliberately excludes the game Java tree, which
the patch bundle reconstructs from a base tree supplied by the user.

Only files tracked by Git are packaged, so a dirty build output directory can
never leak into a release archive.

Examples::

    python3 patcher/project-skeleton/export_skeleton.py \
      --output /tmp/eag19-project-skeleton.zip

    python3 patcher/project-skeleton/export_skeleton.py --print-manifest
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import subprocess
import sys
import tempfile
import zipfile
from pathlib import Path

FORMAT = "eag19-project-skeleton-v1"
MANIFEST_NAME = "SKELETON-MANIFEST.json"
CHUNK_SIZE = 1024 * 1024

DEFAULT_EXCLUDES = (
    "sites-web/public/data/**",
    "sites-web/probe/**",
    "sites-web/.wrangler/**",
    ".idea/**",
    ".cloud-attachments/**",
    "src/main/java/net/minecraft/**",
    "src/main/java/com/mojang/blaze3d/**",
    "src/main/java/com/mojang/realmsclient/**",
    "src/main/java/com/mojang/math/**",
    "**/*.class",
    "build/**",
    "*/build/**",
    ".gradle/**",
    "*/.gradle/**",
    "**/__pycache__/**",
    "**/*.pyc",
)


class SkeletonError(RuntimeError):
    pass


def canonical_json(value: object) -> bytes:
    return (
        json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":"))
        + "\n"
    ).encode("utf-8")


def sha256_bytes(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(CHUNK_SIZE), b""):
            digest.update(chunk)
    return digest.hexdigest()


def read_prefixes(path: Path) -> list[str]:
    prefixes: list[str] = []
    for raw in path.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        prefixes.append(line.replace("\\", "/").strip("/"))
    return prefixes


def tracked_files(source: Path) -> list[str]:
    try:
        result = subprocess.run(
            ["git", "-C", str(source), "ls-files", "-z"],
            check=True,
            capture_output=True,
        )
    except (OSError, subprocess.CalledProcessError) as exc:
        raise SkeletonError(f"cannot list tracked files in {source}: {exc}") from exc
    return sorted(path for path in result.stdout.decode("utf-8").split("\0") if path)


def walk_files(source: Path) -> list[str]:
    files: list[str] = []
    for current, dirs, names in os.walk(source, followlinks=False):
        dirs[:] = sorted(d for d in dirs if d != ".git")
        for name in sorted(names):
            files.append((Path(current) / name).relative_to(source).as_posix())
    return files


_GLOB_CACHE: dict[str, re.Pattern[str]] = {}


def _glob_regex(pattern: str) -> re.Pattern[str]:
    """Translate a POSIX glob into a regex where ``*`` never crosses ``/``."""
    cached = _GLOB_CACHE.get(pattern)
    if cached is not None:
        return cached
    parts: list[str] = []
    index = 0
    length = len(pattern)
    while index < length:
        if pattern.startswith("**/", index):
            parts.append("(?:.*/)?")
            index += 3
        elif pattern.startswith("**", index):
            parts.append(".*")
            index += 2
        elif pattern[index] == "*":
            parts.append("[^/]*")
            index += 1
        elif pattern[index] == "?":
            parts.append("[^/]")
            index += 1
        else:
            parts.append(re.escape(pattern[index]))
            index += 1
    compiled = re.compile("".join(parts) + r"\Z")
    _GLOB_CACHE[pattern] = compiled
    return compiled


def matches(path: str, patterns: tuple[str, ...]) -> bool:
    return any(_glob_regex(pattern).match(path) for pattern in patterns)


def select_files(source: Path, prefixes: list[str], excludes: tuple[str, ...]) -> list[str]:
    available = tracked_files(source)
    selected: list[str] = []
    for rel in available:
        if not any(rel == prefix or rel.startswith(prefix + "/") for prefix in prefixes):
            continue
        if matches(rel, excludes):
            continue
        path = source / Path(*rel.split("/"))
        if path.is_symlink() or not path.is_file():
            raise SkeletonError(f"selected entry is not a regular file: {rel}")
        selected.append(rel)
    return selected


def manifest_for(source: Path, files: list[str]) -> dict[str, object]:
    records = []
    for rel in files:
        path = source / Path(*rel.split("/"))
        records.append({
            "path": rel,
            "sha256": sha256_file(path),
            "size": path.stat().st_size,
        })
    return {
        "format": FORMAT,
        "file_count": len(records),
        "records": records,
    }


def write_archive(source: Path, files: list[str], archive: Path) -> None:
    archive.parent.mkdir(parents=True, exist_ok=True)
    temporary = archive.with_name(f".{archive.name}.writing")
    if temporary.exists():
        temporary.unlink()
    manifest = manifest_for(source, files)
    try:
        with zipfile.ZipFile(
            temporary,
            "w",
            compression=zipfile.ZIP_DEFLATED,
            compresslevel=9,
            allowZip64=True,
        ) as output:
            info = zipfile.ZipInfo(MANIFEST_NAME, date_time=(1980, 1, 1, 0, 0, 0))
            info.compress_type = zipfile.ZIP_DEFLATED
            info.create_system = 3
            info.external_attr = 0o100644 << 16
            output.writestr(info, canonical_json(manifest))
            for rel in files:
                path = source / Path(*rel.split("/"))
                info = zipfile.ZipInfo(rel, date_time=(1980, 1, 1, 0, 0, 0))
                info.compress_type = zipfile.ZIP_DEFLATED
                info.create_system = 3
                info.external_attr = 0o100644 << 16
                with path.open("rb") as stream:
                    output.writestr(info, stream.read())
        os.replace(temporary, archive)
    finally:
        if temporary.exists():
            temporary.unlink()


def cli() -> int:
    here = Path(__file__).resolve().parent
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", type=Path, default=here.parents[1])
    parser.add_argument("--include", type=Path, default=here / "include.txt")
    parser.add_argument("--exclude", action="append", default=[])
    parser.add_argument("--output", type=Path)
    parser.add_argument("--print-manifest", action="store_true")
    args = parser.parse_args()

    try:
        source = args.source.resolve()
        prefixes = read_prefixes(args.include)
        excludes = DEFAULT_EXCLUDES + tuple(args.exclude)
        files = select_files(source, prefixes, excludes)
        if not files:
            raise SkeletonError("no files selected; check the include list")
        if args.print_manifest:
            print(json.dumps(manifest_for(source, files), indent=2, sort_keys=True))
            return 0
        if args.output is None:
            parser.error("--output is required unless --print-manifest is used")
        write_archive(source, files, args.output)
    except SkeletonError as exc:
        print(f"error: {exc}", file=sys.stderr)
        return 1
    print(json.dumps({
        "status": "exported",
        "archive": str(args.output),
        "file_count": len(files),
        "archive_sha256": sha256_file(args.output),
    }, indent=2, sort_keys=True))
    return 0


if __name__ == "__main__":
    sys.exit(cli())
