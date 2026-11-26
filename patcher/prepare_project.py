#!/usr/bin/env python3

from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import shutil
import stat
import sys
import tempfile
import zipfile
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent / "source-patches"))

import export_source_patches as patches

SKELETON_FORMAT = "eag19-project-skeleton-v1"
SKELETON_MANIFEST = "SKELETON-MANIFEST.json"
SHA256_RE = re.compile(r"[0-9a-f]{64}")
MAX_MEMBER_UNCOMPRESSED = 128 * 1024 * 1024
MAX_ARCHIVE_UNCOMPRESSED = 1024 * 1024 * 1024
CHUNK_SIZE = 1024 * 1024


class PrepareError(RuntimeError):
    pass


def canonical_json(value: object) -> bytes:
    return (
        json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":"))
        + "\n"
    ).encode("utf-8")


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(CHUNK_SIZE), b""):
            digest.update(chunk)
    return digest.hexdigest()


def require_sha256(value: str | None, *, label: str) -> str:
    if not isinstance(value, str) or not SHA256_RE.fullmatch(value):
        raise PrepareError(f"{label} requires an externally supplied SHA-256")
    return value


def verify_archive(path: Path, expected: str, *, label: str) -> None:
    actual = sha256_file(path)
    if actual != expected:
        raise PrepareError(f"{label} SHA-256 mismatch: expected {expected}, got {actual}")


def load_skeleton_manifest(archive: zipfile.ZipFile) -> dict[str, dict[str, object]]:
    try:
        raw = archive.read(SKELETON_MANIFEST)
    except KeyError as exc:
        raise PrepareError("skeleton is missing its manifest") from exc
    try:
        value = json.loads(raw.decode("utf-8"))
    except (UnicodeError, json.JSONDecodeError) as exc:
        raise PrepareError(f"skeleton manifest is not valid JSON: {exc}") from exc
    if canonical_json(value) != raw:
        raise PrepareError("skeleton manifest is not canonical JSON")
    if not isinstance(value, dict) or value.get("format") != SKELETON_FORMAT:
        raise PrepareError("skeleton manifest has the wrong format")
    records = value.get("records")
    if not isinstance(records, list) or value.get("file_count") != len(records):
        raise PrepareError("skeleton manifest has an invalid record count")
    result: dict[str, dict[str, object]] = {}
    previous = None
    for record in records:
        if not isinstance(record, dict) or set(record) != {"path", "sha256", "size"}:
            raise PrepareError("skeleton manifest record is malformed")
        path = patches.normalize_rel(record.get("path"), field="skeleton path")
        if path != record.get("path"):
            raise PrepareError(f"skeleton path is not canonical: {record.get('path')!r}")
        if previous is not None and path <= previous:
            raise PrepareError("skeleton records are not strictly sorted")
        previous = path
        digest = record.get("sha256")
        size = record.get("size")
        if not isinstance(digest, str) or not SHA256_RE.fullmatch(digest):
            raise PrepareError(f"skeleton record has an invalid hash: {path}")
        if not isinstance(size, int) or isinstance(size, bool) or size < 0:
            raise PrepareError(f"skeleton record has an invalid size: {path}")
        result[path] = {"sha256": digest, "size": size}
    return result


def extract_skeleton(archive_path: Path, destination: Path) -> int:
    with zipfile.ZipFile(archive_path) as archive:
        infos = archive.infolist()
        if len(infos) > 20_000:
            raise PrepareError("skeleton archive has too many members")
        manifest = load_skeleton_manifest(archive)
        members: dict[str, zipfile.ZipInfo] = {}
        for info in infos:
            if info.filename == SKELETON_MANIFEST:
                continue
            if info.filename.endswith("/") or info.is_dir():
                continue
            rel = patches.normalize_rel(info.filename, field="skeleton archive path")
            if rel in members:
                raise PrepareError(f"duplicate skeleton member: {rel}")
            mode = (info.external_attr >> 16) & 0o170000
            if mode not in {0, stat.S_IFREG}:
                raise PrepareError(f"unsupported skeleton member type: {rel}")
            if info.file_size > MAX_MEMBER_UNCOMPRESSED:
                raise PrepareError(f"skeleton member is too large: {rel}")
            members[rel] = info
        if set(members) != set(manifest):
            missing = sorted(set(manifest) - set(members))[:3]
            extra = sorted(set(members) - set(manifest))[:3]
            raise PrepareError(f"skeleton member set mismatch (missing={missing}, extra={extra})")
        total = sum(int(record["size"]) for record in manifest.values())
        if total > MAX_ARCHIVE_UNCOMPRESSED:
            raise PrepareError("skeleton archive expands beyond the configured limit")

        for rel in sorted(members):
            info = members[rel]
            record = manifest[rel]
            target = destination / Path(*rel.split("/"))
            if target.exists() or target.is_symlink():
                raise PrepareError(f"skeleton destination already exists: {rel}")
            target.parent.mkdir(parents=True, exist_ok=True)
            digest = hashlib.sha256()
            written = 0
            with archive.open(info, "r") as source, target.open("wb") as sink:
                while True:
                    chunk = source.read(CHUNK_SIZE)
                    if not chunk:
                        break
                    written += len(chunk)
                    if written > int(record["size"]):
                        raise PrepareError(f"skeleton member expanded beyond its declared size: {rel}")
                    digest.update(chunk)
                    sink.write(chunk)
            if written != record["size"] or digest.hexdigest() != record["sha256"]:
                raise PrepareError(f"skeleton member failed verification: {rel}")
            os.chmod(target, 0o644)
    return len(members)


def prepare(args: argparse.Namespace) -> dict[str, object]:
    output = Path(args.output)
    if output.exists() or output.is_symlink():
        raise PrepareError(f"output must be absent (refusing overwrite): {output}")
    output.parent.mkdir(parents=True, exist_ok=True)

    bundle_digest = require_sha256(args.expected_bundle_sha256, label="bundle")
    skeleton_digest = require_sha256(args.expected_skeleton_sha256, label="skeleton")
    verify_archive(Path(args.skeleton), skeleton_digest, label="skeleton")

    staging = Path(tempfile.mkdtemp(prefix=f".{output.name}.staging-", dir=str(output.parent)))
    try:
        java_target_rel = patches.normalize_rel(args.java_target, field="java target")
        java_target = staging / Path(*java_target_rel.split("/"))
        java_target.parent.mkdir(parents=True, exist_ok=True)
        result = patches.apply_bundle(
            Path(args.bundle),
            Path(args.base_source),
            java_target,
            bundle_digest,
        )
        skeleton_files = extract_skeleton(Path(args.skeleton), staging)
        receipt = {
            "format": "eag19-project-receipt-v1",
            "bundle_sha256": bundle_digest,
            "skeleton_sha256": skeleton_digest,
            "base_manifest_sha256": result["base_manifest_sha256"],
            "final_manifest_sha256": result["final_manifest_sha256"],
            "counts": result["counts"],
            "skeleton_file_count": skeleton_files,
            "java_target": java_target_rel,
        }
        with (staging / "patcher-receipt.json").open("wb") as stream:
            stream.write(canonical_json(receipt))
        if output.exists() or output.is_symlink():
            raise PrepareError(f"output appeared while patching (refusing overwrite): {output}")
        os.replace(staging, output)
        staging = None
        return {"status": "prepared", "output": str(output), **receipt}
    finally:
        if staging is not None and staging.exists():
            shutil.rmtree(staging, ignore_errors=True)


def cli() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base-source", required=True, type=Path)
    parser.add_argument("--bundle", required=True, type=Path)
    parser.add_argument("--expected-bundle-sha256", required=True)
    parser.add_argument("--skeleton", required=True, type=Path)
    parser.add_argument("--expected-skeleton-sha256", required=True)
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--java-target", default="src/main/java")
    args = parser.parse_args()
    try:
        result = prepare(args)
    except (PrepareError, patches.BundleError) as exc:
        print(f"error: {exc}", file=sys.stderr)
        return 1
    print(json.dumps(result, indent=2, sort_keys=True))
    return 0


if __name__ == "__main__":
    sys.exit(cli())
