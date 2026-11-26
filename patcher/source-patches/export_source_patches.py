#!/usr/bin/env python3

from __future__ import annotations

import argparse
import base64
import binascii
import difflib
import hashlib
import json
import os
import re
import shutil
import stat
import sys
import tempfile
import unicodedata
import zipfile
from pathlib import Path
from typing import Iterator

FORMAT = "eag19-java-source-patch-bundle-v1"
MANIFEST_FORMAT = "eag19-java-source-manifest-v1"
DELTA_FORMAT = "eag19-java-source-delta-v1"
TOOL_VERSION = "1"
CHUNK_SIZE = 1024 * 1024
WINDOWS_ABSOLUTE = re.compile(r"^[A-Za-z]:[\\/]")
SHA256_RE = re.compile(r"[0-9a-f]{64}")
ROOT_LABEL_RE = re.compile(r"[A-Za-z0-9][A-Za-z0-9._/-]{0,99}")

MAX_ARCHIVE_MEMBERS = 50_000
MAX_MEMBER_UNCOMPRESSED = 128 * 1024 * 1024
MAX_ARCHIVE_UNCOMPRESSED = 512 * 1024 * 1024

BUNDLE_FILES = {"bundle.json", "base-manifest.json", "final-manifest.json", "operations.json"}
BUNDLE_KEYS = {
    "format", "tool_version", "source_kind", "base_root", "final_root",
    "base_file_count", "final_file_count", "counts", "base_manifest_sha256",
    "final_manifest_sha256", "operations_sha256",
}
MANIFEST_KEYS = {"format", "root", "file_count", "records"}
MANIFEST_RECORD_KEYS = {"path", "sha256", "size"}
OPERATIONS_KEYS = {"format", "operations", "counts"}
OPERATION_KEYS = {
    "index", "op", "path", "mode", "pre_sha256", "post_sha256",
    "pre_size", "post_size", "payload", "payload_kind", "payload_sha256",
    "payload_size",
}
DELTA_KEYS = {"format", "before_lines", "after_lines", "hunks"}
DELTA_HUNK_KEYS = {"start", "deleted", "inserted_b64", "inserted_lines"}


class BundleError(RuntimeError):
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
    try:
        with path.open("rb") as stream:
            for chunk in iter(lambda: stream.read(CHUNK_SIZE), b""):
                digest.update(chunk)
    except OSError as exc:
        raise BundleError(f"cannot hash {path}: {exc}") from exc
    return digest.hexdigest()


def file_info(path: Path) -> tuple[str, int]:
    try:
        info = path.stat()
    except OSError as exc:
        raise BundleError(f"cannot stat {path}: {exc}") from exc
    if not stat.S_ISREG(info.st_mode):
        raise BundleError(f"expected a regular file, got {path}")
    return sha256_file(path), info.st_size


def _is_int(value: object) -> bool:
    return isinstance(value, int) and not isinstance(value, bool)


def _check_keys(value: object, allowed: set[str], *, label: str) -> None:
    if not isinstance(value, dict):
        raise BundleError(f"{label} must be an object")
    unknown = sorted(set(value) - allowed)
    if unknown:
        raise BundleError(f"{label} has unknown keys: {', '.join(unknown)}")


def _validate_root_label(value: object, *, label: str) -> str:
    if not isinstance(value, str) or not ROOT_LABEL_RE.fullmatch(value) or ".." in value.split("/"):
        raise BundleError(f"{label} has an invalid root label")
    return value


def _collision_key(path: str) -> str:
    return unicodedata.normalize("NFKC", path).casefold()


def _register_paths(seen: dict[str, str], path: str, *, label: str) -> None:
    parts = path.split("/")
    for end in range(1, len(parts) + 1):
        candidate = "/".join(parts[:end])
        key = _collision_key(candidate)
        previous = seen.get(key)
        if previous is not None and previous != candidate:
            raise BundleError(
                f"{label} paths collide after case/Unicode normalization: "
                f"{previous!r} vs {candidate!r}"
            )
        seen[key] = candidate


def _reject_symlink_ancestors(path: Path, *, label: str) -> None:
    path = Path(path)
    absolute = path if path.is_absolute() else Path.cwd() / path
    current = Path(absolute.anchor)
    for component in absolute.parts[1:]:
        current /= component
        try:
            if current.is_symlink():
                raise BundleError(f"symlink is not allowed in {label}: {current}")
        except OSError as exc:
            raise BundleError(f"cannot inspect {label}: {current}: {exc}") from exc


def _reject_symlink(path: Path, *, label: str) -> None:
    if path.is_symlink():
        raise BundleError(f"symlink is not allowed in {label}: {path}")


def normalize_rel(raw: object, *, field: str = "path") -> str:
    if not isinstance(raw, str) or not raw:
        raise BundleError(f"{field} must be a non-empty string")
    if "\x00" in raw:
        raise BundleError(f"{field} contains NUL")
    candidate = raw.replace("\\", "/")
    if candidate.startswith("//"):
        raise BundleError(f"{field} is absolute: {raw!r}")
    if candidate.startswith("/") or WINDOWS_ABSOLUTE.match(candidate):
        raise BundleError(f"{field} is absolute: {raw!r}")
    parts: list[str] = []
    for component in candidate.split("/"):
        if component in ("", "."):
            continue
        if component == "..":
            raise BundleError(f"{field} contains path traversal: {raw!r}")
        parts.append(component)
    if not parts:
        raise BundleError(f"{field} normalizes to an empty path: {raw!r}")
    normalized = "/".join(parts)
    if normalized.startswith("/") or normalized == ".." or normalized.startswith("../"):
        raise BundleError(f"{field} escapes its root: {raw!r}")
    return normalized


def scan_source(root: Path) -> dict[str, dict[str, object]]:


    root = Path(root)
    _reject_symlink_ancestors(root, label="source root")
    _reject_symlink(root, label="source root")
    if not root.is_dir():
        raise BundleError(f"source root is not a directory: {root}")

    entries: dict[str, dict[str, object]] = {}
    collisions: dict[str, str] = {}
    for current, dirs, files in os.walk(root, topdown=True, followlinks=False):
        current_path = Path(current)
        for name in sorted(dirs):
            directory = current_path / name
            _reject_symlink(directory, label="source tree")
            if not directory.is_dir():
                raise BundleError(f"source tree entry is not a directory: {directory}")
            rel = normalize_rel(directory.relative_to(root).as_posix(), field="source directory")
            _register_paths(collisions, rel, label="source")
        dirs.sort()
        for name in sorted(files):
            path = current_path / name
            _reject_symlink(path, label="source tree")
            if not path.is_file():
                raise BundleError(f"source tree entry is not a regular file: {path}")
            rel = normalize_rel(path.relative_to(root).as_posix(), field="source path")
            if not rel.endswith(".java"):
                raise BundleError(f"source root contains a non-Java file: {rel}")
            if rel in entries:
                raise BundleError(f"duplicate normalized source path: {rel}")
            _register_paths(collisions, rel, label="source")
            digest, size = file_info(path)
            entries[rel] = {"path": rel, "sha256": digest, "size": size}
    return dict(sorted(entries.items()))


def manifest_object(root_label: str, entries: dict[str, dict[str, object]]) -> dict[str, object]:
    records = [entries[path] for path in sorted(entries)]
    return {
        "format": MANIFEST_FORMAT,
        "root": root_label,
        "file_count": len(records),
        "records": records,
    }


def write_bytes(path: Path, data: bytes) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("wb") as stream:
        stream.write(data)


def write_canonical_json(path: Path, value: object) -> None:
    write_bytes(path, canonical_json(value))


def read_json(path: Path) -> object:
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except (OSError, UnicodeError, json.JSONDecodeError) as exc:
        raise BundleError(f"invalid JSON {path}: {exc}") from exc


def _safe_bundle_file(root: Path, raw: object, *, field: str) -> Path:
    rel = normalize_rel(raw, field=field)
    path = root / Path(*rel.split("/"))
    try:
        path.relative_to(root)
    except ValueError as exc:
        raise BundleError(f"{field} escapes bundle: {raw!r}") from exc
    _reject_symlink_ancestors(path, label="bundle")
    _reject_symlink(path, label="bundle")
    return path


def _bundle_tree_files(root: Path) -> set[str]:
    result: set[str] = set()
    collisions: dict[str, str] = {}
    _reject_symlink_ancestors(root, label="bundle root")
    _reject_symlink(root, label="bundle root")
    if not root.is_dir():
        raise BundleError(f"bundle is not a directory: {root}")
    for current, dirs, files in os.walk(root, topdown=True, followlinks=False):
        current_path = Path(current)
        for name in sorted(dirs):
            directory = current_path / name
            _reject_symlink(directory, label="bundle")
            if not directory.is_dir():
                raise BundleError(f"bundle entry is not a directory: {directory}")
            rel = normalize_rel(directory.relative_to(root).as_posix(), field="bundle directory")
            _register_paths(collisions, rel, label="bundle")
        dirs.sort()
        for name in sorted(files):
            path = current_path / name
            _reject_symlink(path, label="bundle")
            if not path.is_file():
                raise BundleError(f"bundle entry is not regular: {path}")
            rel = normalize_rel(path.relative_to(root).as_posix(), field="bundle path")
            if rel in result:
                raise BundleError(f"duplicate normalized bundle path: {rel}")
            _register_paths(collisions, rel, label="bundle")
            result.add(rel)
    return result


def _unpack_zip(archive: Path, destination: Path) -> None:
    try:
        with zipfile.ZipFile(archive) as bundle:
            infos = bundle.infolist()
            if len(infos) > MAX_ARCHIVE_MEMBERS:
                raise BundleError(f"archive has too many members: {len(infos)}")
            names: set[str] = set()
            collisions: dict[str, str] = {}
            total = 0
            for info in infos:
                raw = info.filename
                is_directory = raw.endswith("/") or info.is_dir()
                rel = normalize_rel(raw.rstrip("/"), field="archive path")
                if rel in names:
                    raise BundleError(f"duplicate normalized archive path: {rel}")
                _register_paths(collisions, rel, label="archive")
                names.add(rel)
                mode = (info.external_attr >> 16) & 0o170000
                if mode == stat.S_IFLNK:
                    raise BundleError(f"symlink archive member is not allowed: {raw!r}")
                expected_type = stat.S_IFDIR if is_directory else stat.S_IFREG
                if mode not in {0, expected_type}:
                    raise BundleError(f"unsupported archive member type for {raw!r}")
                if info.file_size < 0 or info.file_size > MAX_MEMBER_UNCOMPRESSED:
                    raise BundleError(f"archive member is too large: {raw!r}")
                if info.compress_size < 0:
                    raise BundleError(f"archive member has invalid compressed size: {raw!r}")
                total += info.file_size
                if total > MAX_ARCHIVE_UNCOMPRESSED:
                    raise BundleError("archive expanded size exceeds the configured limit")
                target = destination / Path(*rel.split("/"))
                if is_directory:
                    target.mkdir(parents=True, exist_ok=True)
                    continue
                target.parent.mkdir(parents=True, exist_ok=True)
                with bundle.open(info, "r") as source, target.open("wb") as sink:
                    copied = 0
                    while True:
                        chunk = source.read(CHUNK_SIZE)
                        if not chunk:
                            break
                        copied += len(chunk)
                        if copied > info.file_size:
                            raise BundleError(f"archive member expanded beyond its declared size: {raw!r}")
                        sink.write(chunk)
                    if copied != info.file_size:
                        raise BundleError(f"archive member size changed while reading: {raw!r}")
    except (OSError, zipfile.BadZipFile, RuntimeError) as exc:
        raise BundleError(f"cannot read bundle archive {archive}: {exc}") from exc


def materialize_bundle(bundle: Path) -> tuple[Path, object]:


    bundle = Path(bundle)
    _reject_symlink_ancestors(bundle, label="bundle")
    if bundle.is_dir():
        return bundle, None
    if not bundle.is_file():
        raise BundleError(f"bundle does not exist: {bundle}")
    temporary = tempfile.mkdtemp(prefix="eag19-source-bundle-")
    try:
        _unpack_zip(bundle, Path(temporary))
    except Exception:
        shutil.rmtree(temporary, ignore_errors=True)
        raise
    return Path(temporary), temporary


def _manifest_map(value: object, *, label: str) -> dict[str, dict[str, object]]:
    _check_keys(value, MANIFEST_KEYS, label=label)
    if value.get("format") != MANIFEST_FORMAT:
        raise BundleError(f"{label} has the wrong manifest format")
    records = value.get("records")
    if not isinstance(records, list) or not _is_int(value.get("file_count")) or value.get("file_count") != len(records):
        raise BundleError(f"{label} has an invalid records/file_count pair")
    result: dict[str, dict[str, object]] = {}
    collisions: dict[str, str] = {}
    previous: str | None = None
    for record in records:
        _check_keys(record, MANIFEST_RECORD_KEYS, label=f"{label} record")
        raw_path = record.get("path")
        path = normalize_rel(raw_path, field=f"{label} path")
        if path != raw_path:
            raise BundleError(f"{label} path is not canonical: {raw_path!r}")
        if previous is not None and path <= previous:
            raise BundleError(f"{label} records are not strictly sorted")
        previous = path
        digest = record.get("sha256")
        size = record.get("size")
        if not isinstance(digest, str) or not SHA256_RE.fullmatch(digest):
            raise BundleError(f"{label} has an invalid SHA-256 for {path}")
        if not _is_int(size) or size < 0:
            raise BundleError(f"{label} has an invalid size for {path}")
        if path in result:
            raise BundleError(f"duplicate normalized {label} path: {path}")
        _register_paths(collisions, path, label=label)
        result[path] = {"path": path, "sha256": digest, "size": size}
    if not isinstance(value.get("root"), str) or not value["root"]:
        raise BundleError(f"{label} has an invalid root")
    return result


def _manifest_digest(value: object) -> str:
    return sha256_bytes(canonical_json(value))


def _validate_counts(value: object, *, label: str) -> dict[str, int]:
    if not isinstance(value, dict) or set(value) != {"add", "modify", "delete"}:
        raise BundleError(f"{label} must contain exactly add/modify/delete")
    result: dict[str, int] = {}
    for key in ("add", "modify", "delete"):
        if not _is_int(value.get(key)) or value[key] < 0:
            raise BundleError(f"{label} has invalid {key}")
        result[key] = value[key]
    return result


def _assert_tree_manifest(root: Path, expected: dict[str, dict[str, object]], *, label: str) -> None:
    actual = scan_source(root)
    if actual != expected:
        missing = sorted(set(expected) - set(actual))
        extra = sorted(set(actual) - set(expected))
        changed = sorted(path for path in set(actual) & set(expected) if actual[path] != expected[path])
        details: list[str] = []
        if missing:
            details.append(f"missing={missing[:3]}")
        if extra:
            details.append(f"extra={extra[:3]}")
        if changed:
            details.append(f"changed={changed[:3]}")
        raise BundleError(f"{label} manifest mismatch ({'; '.join(details)})")


def _delta_object(before: bytes, after: bytes) -> dict[str, object]:


    before_lines = before.splitlines(keepends=True)
    after_lines = after.splitlines(keepends=True)
    hunks: list[dict[str, object]] = []
    matcher = difflib.SequenceMatcher(a=before_lines, b=after_lines, autojunk=False)
    for tag, i1, i2, j1, j2 in matcher.get_opcodes():
        if tag == "equal":
            continue
        inserted = b"".join(after_lines[j1:j2])
        hunks.append({
            "start": i1,
            "deleted": [sha256_bytes(line) for line in before_lines[i1:i2]],
            "inserted_b64": base64.b64encode(inserted).decode("ascii"),
            "inserted_lines": j2 - j1,
        })
    return {
        "format": DELTA_FORMAT,
        "before_lines": len(before_lines),
        "after_lines": len(after_lines),
        "hunks": hunks,
    }


def _parse_delta(value: object, *, label: str) -> dict[str, object]:
    _check_keys(value, DELTA_KEYS, label=label)
    if value.get("format") != DELTA_FORMAT:
        raise BundleError(f"{label} has an unsupported delta format")
    before_lines = value.get("before_lines")
    after_lines = value.get("after_lines")
    hunks = value.get("hunks")
    if not _is_int(before_lines) or before_lines < 0 or not _is_int(after_lines) or after_lines < 0:
        raise BundleError(f"{label} has invalid line counts")
    if not isinstance(hunks, list):
        raise BundleError(f"{label} hunks must be a list")
    parsed: list[dict[str, object]] = []
    previous_end = -1
    for number, hunk in enumerate(hunks, start=1):
        _check_keys(hunk, DELTA_HUNK_KEYS, label=f"{label} hunk {number}")
        start = hunk.get("start")
        deleted = hunk.get("deleted")
        inserted_lines = hunk.get("inserted_lines")
        encoded = hunk.get("inserted_b64")
        if not _is_int(start) or start < 0:
            raise BundleError(f"{label} hunk {number} has an invalid start")
        if not isinstance(deleted, list) or not all(isinstance(item, str) and SHA256_RE.fullmatch(item) for item in deleted):
            raise BundleError(f"{label} hunk {number} has invalid deleted-line hashes")
        if not _is_int(inserted_lines) or inserted_lines < 0:
            raise BundleError(f"{label} hunk {number} has an invalid inserted line count")
        if not isinstance(encoded, str):
            raise BundleError(f"{label} hunk {number} has invalid insert data")
        try:
            inserted = base64.b64decode(encoded.encode("ascii"), validate=True)
        except (ValueError, UnicodeError, binascii.Error) as exc:
            raise BundleError(f"{label} hunk {number} has invalid base64 insert data") from exc
        if len(inserted.splitlines(keepends=True)) != inserted_lines:
            raise BundleError(f"{label} hunk {number} insert line count does not match data")
        if start <= previous_end:
            raise BundleError(f"{label} hunks overlap or are not strictly sorted")
        if not deleted and not inserted:
            raise BundleError(f"{label} hunk {number} is empty")
        previous_end = start + len(deleted) - 1
        parsed.append({
            "start": start,
            "deleted": deleted,
            "inserted": inserted,
            "inserted_lines": inserted_lines,
        })
    return {
        "before_lines": before_lines,
        "after_lines": after_lines,
        "hunks": parsed,
    }


def _apply_delta(before: bytes, payload: bytes, *, label: str) -> bytes:
    try:
        value = json.loads(payload.decode("utf-8"))
    except (UnicodeError, json.JSONDecodeError) as exc:
        raise BundleError(f"{label} is not valid UTF-8 JSON") from exc
    if canonical_json(value) != payload:
        raise BundleError(f"{label} is not canonical JSON")
    delta = _parse_delta(value, label=label)
    before_lines = before.splitlines(keepends=True)
    if len(before_lines) != delta["before_lines"]:
        raise BundleError(f"{label} preimage line count mismatch")
    output: list[bytes] = []
    cursor = 0
    for number, hunk in enumerate(delta["hunks"], start=1):
        start = int(hunk["start"])
        deleted = hunk["deleted"]
        if start < cursor or start + len(deleted) > len(before_lines):
            raise BundleError(f"{label} hunk {number} lies outside the preimage")
        for offset, digest in enumerate(deleted):
            if sha256_bytes(before_lines[start + offset]) != digest:
                raise BundleError(f"{label} hunk {number} deleted-line hash mismatch")
        output.extend(before_lines[cursor:start])
        output.extend(hunk["inserted"].splitlines(keepends=True))
        cursor = start + len(deleted)
    output.extend(before_lines[cursor:])
    result = b"".join(output)
    if len(result.splitlines(keepends=True)) != delta["after_lines"]:
        raise BundleError(f"{label} produced an unexpected postimage line count")
    return result


def _load_verified_bundle(bundle_root: Path) -> tuple[dict, dict, dict, list]:
    files = _bundle_tree_files(bundle_root)
    missing = sorted(BUNDLE_FILES - files)
    if missing:
        raise BundleError(f"bundle is missing required files: {', '.join(missing)}")
    metadata = read_json(bundle_root / "bundle.json")
    _check_keys(metadata, BUNDLE_KEYS, label="bundle.json")
    if metadata.get("format") != FORMAT:
        raise BundleError("bundle.json has the wrong format")
    if metadata.get("tool_version") != TOOL_VERSION:
        raise BundleError("unsupported source-patch tool version")
    if metadata.get("source_kind") != "java-source":
        raise BundleError("bundle.json has an invalid source_kind")
    base_label = _validate_root_label(metadata.get("base_root"), label="bundle base_root")
    final_label = _validate_root_label(metadata.get("final_root"), label="bundle final_root")
    if base_label == final_label:
        raise BundleError("bundle roots must have different labels")
    for key in ("base_file_count", "final_file_count"):
        if not _is_int(metadata.get(key)) or metadata[key] < 0:
            raise BundleError(f"bundle.json has an invalid {key}")
    _validate_counts(metadata.get("counts"), label="bundle.json counts")
    for key in ("base_manifest_sha256", "final_manifest_sha256", "operations_sha256"):
        if not isinstance(metadata.get(key), str) or not SHA256_RE.fullmatch(metadata[key]):
            raise BundleError(f"bundle.json has an invalid {key}")

    base_obj = read_json(bundle_root / "base-manifest.json")
    final_obj = read_json(bundle_root / "final-manifest.json")
    if not isinstance(base_obj, dict) or base_obj.get("root") != base_label:
        raise BundleError("base manifest has an unexpected root")
    if not isinstance(final_obj, dict) or final_obj.get("root") != final_label:
        raise BundleError("final manifest has an unexpected root")
    base = _manifest_map(base_obj, label="base manifest")
    final = _manifest_map(final_obj, label="final manifest")

    operations_obj = read_json(bundle_root / "operations.json")
    _check_keys(operations_obj, OPERATIONS_KEYS, label="operations.json")
    if operations_obj.get("format") != FORMAT:
        raise BundleError("operations.json has the wrong format")
    operations = operations_obj.get("operations")
    if not isinstance(operations, list):
        raise BundleError("operations.json operations must be a list")
    counts = _validate_counts(operations_obj.get("counts"), label="operations.json counts")

    expected_counts = {"add": 0, "modify": 0, "delete": 0}
    seen: set[str] = set()
    expected_payloads: set[str] = set()
    previous_path: str | None = None
    for index, operation in enumerate(operations, start=1):
        if not isinstance(operation, dict):
            raise BundleError(f"operation {index} is not an object")
        _check_keys(operation, OPERATION_KEYS, label=f"operation {index}")
        if operation.get("index") != index:
            raise BundleError(f"operation indexes must be contiguous (expected {index})")
        kind = operation.get("op")
        if kind not in {"add", "modify", "delete"}:
            raise BundleError(f"operation {index} has unsupported type {kind!r}")
        expected_counts[kind] += 1
        raw_path = operation.get("path")
        path = normalize_rel(raw_path, field=f"operation {index} path")
        if path != raw_path or path in seen:
            raise BundleError(f"operation {index} has a duplicate/non-canonical path: {raw_path!r}")
        if previous_path is not None and path <= previous_path:
            raise BundleError(f"operation paths are not strictly sorted at index {index}")
        previous_path = path
        seen.add(path)
        if operation.get("mode") != "100644":
            raise BundleError(f"operation {index} must use regular-file mode 100644")
        pre = operation.get("pre_sha256")
        post = operation.get("post_sha256")
        for label, digest in (("pre_sha256", pre), ("post_sha256", post)):
            if digest is not None and (not isinstance(digest, str) or not SHA256_RE.fullmatch(digest)):
                raise BundleError(f"operation {index} has invalid {label}")
        pre_size = operation.get("pre_size")
        post_size = operation.get("post_size")
        if pre is None and pre_size is not None or post is None and post_size is not None:
            raise BundleError(f"operation {index} has an inconsistent null image size")
        if pre_size is not None and (not _is_int(pre_size) or pre_size < 0):
            raise BundleError(f"operation {index} has invalid pre_size")
        if post_size is not None and (not _is_int(post_size) or post_size < 0):
            raise BundleError(f"operation {index} has invalid post_size")
        if kind == "add" and (pre is not None or pre_size is not None):
            raise BundleError(f"add operation {index} must have null preimage fields")
        if kind == "delete" and (post is not None or post_size is not None):
            raise BundleError(f"delete operation {index} must have null postimage fields")
        if kind in {"add", "modify"}:
            payload_raw = operation.get("payload")
            payload = normalize_rel(payload_raw, field=f"operation {index} payload")
            if payload != payload_raw or not payload.startswith("payload/"):
                raise BundleError(f"operation {index} has an invalid payload path")
            expected_kind = "full-v1" if kind == "add" else "line-delta-v1"
            if operation.get("payload_kind") != expected_kind:
                raise BundleError(f"operation {index} has an invalid payload kind")
            payload_digest = operation.get("payload_sha256")
            payload_size = operation.get("payload_size")
            if not isinstance(payload_digest, str) or not SHA256_RE.fullmatch(payload_digest):
                raise BundleError(f"operation {index} has an invalid payload_sha256")
            if not _is_int(payload_size) or payload_size < 0:
                raise BundleError(f"operation {index} has an invalid payload_size")
            expected_payloads.add(payload)
            payload_file = _safe_bundle_file(bundle_root, payload, field=f"operation {index} payload")
            if not payload_file.is_file():
                raise BundleError(f"operation {index} payload is missing: {payload}")
            digest, size = file_info(payload_file)
            if digest != payload_digest or size != payload_size:
                raise BundleError(f"operation {index} payload hash/size mismatch: {path}")
            if kind == "modify":
                delta = read_json(payload_file)
                parsed = _parse_delta(delta, label=f"operation {index} delta")
                if canonical_json(delta) != payload_file.read_bytes():
                    raise BundleError(f"operation {index} delta is not canonical JSON")
                if parsed["before_lines"] < 0 or parsed["after_lines"] < 0:
                    raise BundleError(f"operation {index} delta has invalid line counts")
        elif any(operation.get(key) is not None for key in ("payload", "payload_kind", "payload_sha256", "payload_size")):
            raise BundleError(f"delete operation {index} cannot contain payload fields")
        if kind == "add":
            if path in base or path not in final or final[path]["sha256"] != post or final[path]["size"] != post_size:
                raise BundleError(f"add operation {index} does not match base/final manifests")
        elif kind == "modify":
            if path not in base or path not in final or base[path]["sha256"] != pre or final[path]["sha256"] != post or base[path]["size"] != pre_size or final[path]["size"] != post_size:
                raise BundleError(f"modify operation {index} does not match base/final manifests")
        elif kind == "delete":
            if path not in base or path in final or base[path]["sha256"] != pre or base[path]["size"] != pre_size:
                raise BundleError(f"delete operation {index} does not match base/final manifests")

    actual_payloads = {path for path in files if path.startswith("payload/")}
    if actual_payloads != expected_payloads:
        extra = sorted(actual_payloads - expected_payloads)
        missing_payloads = sorted(expected_payloads - actual_payloads)
        raise BundleError(f"payload set mismatch (extra={extra[:3]}, missing={missing_payloads[:3]})")
    unexpected = sorted(files - actual_payloads - BUNDLE_FILES)
    if unexpected:
        raise BundleError(f"bundle contains unexpected root files: {unexpected[:3]}")
    if counts != expected_counts:
        raise BundleError("operations.json counts do not match operations")
    delta_paths = (set(base) ^ set(final)) | {
        path for path in set(base) & set(final) if base[path] != final[path]
    }
    if seen != delta_paths:
        raise BundleError("operations do not cover the complete base/final manifest delta")
    counts_expected = {
        "add": len(set(final) - set(base)),
        "modify": sum(1 for path in set(base) & set(final) if base[path] != final[path]),
        "delete": len(set(base) - set(final)),
    }
    if counts != counts_expected or metadata.get("counts") != counts_expected:
        raise BundleError(f"operation counts do not match manifest delta: {counts} != {counts_expected}")
    if metadata.get("base_manifest_sha256") != _manifest_digest(base_obj):
        raise BundleError("bundle base manifest digest mismatch")
    if metadata.get("final_manifest_sha256") != _manifest_digest(final_obj):
        raise BundleError("bundle final manifest digest mismatch")
    if metadata.get("operations_sha256") != sha256_file(bundle_root / "operations.json"):
        raise BundleError("bundle operations digest mismatch")
    if metadata.get("base_file_count") != len(base) or metadata.get("final_file_count") != len(final):
        raise BundleError("bundle manifest file counts are inconsistent")
    return metadata, base, final, operations


def _expanded_bundle_sha256(bundle_root: Path) -> str:
    records: list[tuple[str, str, int]] = []
    for rel in sorted(_bundle_tree_files(bundle_root)):
        path = bundle_root / Path(*rel.split("/"))
        digest, size = file_info(path)
        records.append((rel, digest, size))
    return sha256_bytes(canonical_json(records))


def _assert_expected_bundle_sha256(bundle: Path, expected: str | None) -> str:
    if not isinstance(expected, str) or not SHA256_RE.fullmatch(expected):
        raise BundleError("an externally supplied expected bundle SHA-256 is required")
    bundle = Path(bundle)
    _reject_symlink_ancestors(bundle, label="bundle")
    if bundle.is_file():
        actual = sha256_file(bundle)
    elif bundle.is_dir():
        actual = _expanded_bundle_sha256(bundle)
    else:
        raise BundleError(f"bundle does not exist: {bundle}")
    if actual != expected:
        raise BundleError(f"bundle SHA-256 mismatch: expected {expected}, got {actual}")
    return actual


def verify_bundle(bundle: Path, base_root: Path, expected_bundle_sha256: str | None = None) -> dict[str, object]:
    bundle_digest = None
    if expected_bundle_sha256 is not None:
        bundle_digest = _assert_expected_bundle_sha256(Path(bundle), expected_bundle_sha256)
    bundle_root, temporary = materialize_bundle(bundle)
    try:
        metadata, base, final, operations = _load_verified_bundle(bundle_root)
        _assert_tree_manifest(base_root, base, label="base source")
        return {
            "bundle_format": metadata["format"],
            "base_file_count": len(base),
            "final_file_count": len(final),
            "operation_count": len(operations),
            "counts": metadata["counts"],
            "base_manifest_sha256": metadata["base_manifest_sha256"],
            "final_manifest_sha256": metadata["final_manifest_sha256"],
            "bundle_sha256": bundle_digest,
        }
    finally:
        if temporary is not None:
            shutil.rmtree(temporary, ignore_errors=True)


def _ensure_output_absent(output: Path) -> None:
    _reject_symlink_ancestors(output, label="output")
    if output.exists() or output.is_symlink():
        raise BundleError(f"output must be absent (refusing overwrite): {output}")
    parent = output.parent
    _reject_symlink(parent, label="output parent")
    if not parent.is_dir():
        raise BundleError(f"output parent is not a directory: {parent}")


def _copy_java_tree(source: Path, destination: Path) -> None:
    entries = scan_source(source)
    for rel in entries:
        source_path = source / Path(*rel.split("/"))
        target_path = destination / Path(*rel.split("/"))
        target_path.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(source_path, target_path)
        os.chmod(target_path, 0o644)


def _remove_empty_directories(root: Path) -> None:
    for path in sorted(root.rglob("*"), key=lambda item: len(item.parts), reverse=True):
        if path.is_dir() and not path.is_symlink():
            try:
                path.rmdir()
            except OSError:
                pass


def apply_bundle(
    bundle: Path,
    base_root: Path,
    output: Path,
    expected_bundle_sha256: str | None = None,
) -> dict[str, object]:


    output = Path(output)
    bundle_digest = _assert_expected_bundle_sha256(Path(bundle), expected_bundle_sha256)
    _ensure_output_absent(output)
    bundle_root, temporary = materialize_bundle(bundle)
    stage: Path | None = None
    try:
        metadata, base, final, operations = _load_verified_bundle(bundle_root)
        _assert_tree_manifest(base_root, base, label="base source")
        try:
            stage = Path(tempfile.mkdtemp(prefix=f".{output.name}.staging-", dir=str(output.parent)))
            _copy_java_tree(base_root, stage)
            for operation in operations:
                path = operation["path"]
                target = stage / Path(*path.split("/"))
                _reject_symlink(target, label="staging source")
                kind = operation["op"]
                pre = operation["pre_sha256"]
                post = operation["post_sha256"]
                if kind in {"modify", "delete"}:
                    if not target.is_file() or sha256_file(target) != pre:
                        raise BundleError(f"preimage mismatch for {kind} operation: {path}")
                if kind == "delete":
                    target.unlink()
                    continue
                if kind == "add" and target.exists():
                    raise BundleError(f"add operation target already exists: {path}")
                payload = _safe_bundle_file(bundle_root, operation["payload"], field="operation payload")
                target.parent.mkdir(parents=True, exist_ok=True)
                temporary_file = target.with_name(f".{target.name}.patching")
                if kind == "modify":
                    before = target.read_bytes()
                    after = _apply_delta(before, payload.read_bytes(), label=f"operation {operation['index']} delta")
                    with temporary_file.open("wb") as destination:
                        destination.write(after)
                else:
                    with payload.open("rb") as source, temporary_file.open("wb") as destination:
                        shutil.copyfileobj(source, destination, CHUNK_SIZE)
                os.chmod(temporary_file, 0o644)
                os.replace(temporary_file, target)
                if sha256_file(target) != post or target.stat().st_size != operation["post_size"]:
                    raise BundleError(f"postimage mismatch after {kind} operation: {path}")
            _remove_empty_directories(stage)
            _assert_tree_manifest(stage, final, label="reconstructed source")
            _ensure_output_absent(output)
            os.replace(stage, output)
            stage = None
        finally:
            if stage is not None and stage.exists():
                shutil.rmtree(stage, ignore_errors=True)
        return {
            "status": "applied",
            "output": str(output),
            "base_manifest_sha256": metadata["base_manifest_sha256"],
            "final_manifest_sha256": metadata["final_manifest_sha256"],
            "bundle_sha256": bundle_digest,
            "counts": metadata["counts"],
        }
    finally:
        if temporary is not None:
            shutil.rmtree(temporary, ignore_errors=True)


def export_bundle(
    base_root: Path,
    final_root: Path,
    output: Path,
    archive: Path | None = None,
    *,
    base_label: str = "vanilla-source",
    final_label: str = "game-source",
) -> dict[str, object]:


    base_label = _validate_root_label(base_label, label="base label")
    final_label = _validate_root_label(final_label, label="final label")
    if base_label == final_label:
        raise BundleError("base and final labels must differ")
    output = Path(output)
    _ensure_output_absent(output)
    if archive is not None:
        archive = Path(archive)
        if archive.exists() or archive.is_symlink():
            raise BundleError(f"archive must be absent (refusing overwrite): {archive}")
        _reject_symlink_ancestors(archive.parent, label="archive parent")
        _reject_symlink(archive.parent, label="archive parent")
        if not archive.parent.is_dir():
            raise BundleError(f"archive parent is not a directory: {archive.parent}")

    base_entries = scan_source(Path(base_root))
    final_entries = scan_source(Path(final_root))
    base_obj = manifest_object(base_label, base_entries)
    final_obj = manifest_object(final_label, final_entries)

    modifications = sorted(
        path for path in set(base_entries) & set(final_entries)
        if base_entries[path]["sha256"] != final_entries[path]["sha256"]
    )
    additions = sorted(set(final_entries) - set(base_entries))
    deletions = sorted(set(base_entries) - set(final_entries))
    delta_paths = [("add", path) for path in additions] + [("modify", path) for path in modifications] + [("delete", path) for path in deletions]
    delta_paths.sort(key=lambda item: item[1])

    operations: list[dict[str, object]] = []
    payload_sources: list[tuple[str, Path | bytes]] = []
    for index, (kind, path) in enumerate(delta_paths, start=1):
        before = base_entries.get(path)
        after = final_entries.get(path)
        operation: dict[str, object] = {
            "index": index,
            "op": kind,
            "path": path,
            "mode": "100644",
            "pre_sha256": before["sha256"] if before else None,
            "post_sha256": after["sha256"] if after else None,
            "pre_size": before["size"] if before else None,
            "post_size": after["size"] if after else None,
        }
        if kind in {"add", "modify"}:
            payload = f"payload/{index:04d}-{kind}/{path}{'.delta' if kind == 'modify' else ''}"
            operation["payload"] = payload
            if kind == "add":
                operation["payload_kind"] = "full-v1"
                source = Path(final_root) / Path(*path.split("/"))
                digest, size = file_info(source)
                operation["payload_sha256"] = digest
                operation["payload_size"] = size
                payload_sources.append((payload, source))
            else:
                operation["payload_kind"] = "line-delta-v1"
                before_bytes = (Path(base_root) / Path(*path.split("/"))).read_bytes()
                after_bytes = (Path(final_root) / Path(*path.split("/"))).read_bytes()
                delta_bytes = canonical_json(_delta_object(before_bytes, after_bytes))
                operation["payload_sha256"] = sha256_bytes(delta_bytes)
                operation["payload_size"] = len(delta_bytes)
                payload_sources.append((payload, delta_bytes))
        operations.append(operation)

    counts = {"add": len(additions), "modify": len(modifications), "delete": len(deletions)}
    operations_obj = {"format": FORMAT, "operations": operations, "counts": counts}
    metadata = {
        "format": FORMAT,
        "tool_version": TOOL_VERSION,
        "source_kind": "java-source",
        "base_root": base_label,
        "final_root": final_label,
        "base_file_count": len(base_entries),
        "final_file_count": len(final_entries),
        "counts": counts,
        "base_manifest_sha256": _manifest_digest(base_obj),
        "final_manifest_sha256": _manifest_digest(final_obj),
        "operations_sha256": sha256_bytes(canonical_json(operations_obj)),
    }

    stage = Path(tempfile.mkdtemp(prefix=f".{output.name}.staging-", dir=str(output.parent)))
    archive_temporary: Path | None = None
    archive_published = False
    output_published = False
    try:
        write_canonical_json(stage / "bundle.json", metadata)
        write_canonical_json(stage / "base-manifest.json", base_obj)
        write_canonical_json(stage / "final-manifest.json", final_obj)
        write_canonical_json(stage / "operations.json", operations_obj)
        for payload, source in payload_sources:
            destination = _safe_bundle_file(stage, payload, field="payload path")
            destination.parent.mkdir(parents=True, exist_ok=True)
            if isinstance(source, bytes):
                destination.write_bytes(source)
            else:
                shutil.copyfile(source, destination)
            os.chmod(destination, 0o644)
        _load_verified_bundle(stage)
        if archive is not None:
            archive_temporary = archive.with_name(f".{archive.name}.staging-{os.getpid()}")
            if archive_temporary.exists() or archive_temporary.is_symlink():
                raise BundleError(f"temporary archive path already exists: {archive_temporary}")
            write_deterministic_zip(stage, archive_temporary)
            os.replace(archive_temporary, archive)
            archive_published = True
        _ensure_output_absent(output)
        os.replace(stage, output)
        output_published = True
    except Exception:
        if archive_temporary is not None and archive_temporary.exists():
            archive_temporary.unlink()
        if archive_published and archive is not None and archive.exists():
            archive.unlink()
        if output_published and output.exists():
            shutil.rmtree(output, ignore_errors=True)
        if stage.exists():
            shutil.rmtree(stage, ignore_errors=True)
        raise
    return {
        "status": "exported",
        "bundle": str(output),
        "archive": str(archive) if archive is not None else None,
        "base_file_count": len(base_entries),
        "final_file_count": len(final_entries),
        "operation_count": len(operations),
        "counts": counts,
        "base_manifest_sha256": metadata["base_manifest_sha256"],
        "final_manifest_sha256": metadata["final_manifest_sha256"],
        "bundle_sha256": sha256_file(archive) if archive is not None else None,
    }


def write_deterministic_zip(bundle_root: Path, archive: Path) -> None:
    files = sorted(_bundle_tree_files(bundle_root))
    temporary = archive.with_name(f".{archive.name}.writing")
    if temporary.exists() or temporary.is_symlink():
        raise BundleError(f"temporary archive path already exists: {temporary}")
    try:
        with zipfile.ZipFile(temporary, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9, allowZip64=True) as output:
            for rel in files:
                source = bundle_root / Path(*rel.split("/"))
                info = zipfile.ZipInfo(rel, date_time=(1980, 1, 1, 0, 0, 0))
                info.compress_type = zipfile.ZIP_DEFLATED
                info.create_system = 3
                info.external_attr = 0o100644 << 16
                info.flag_bits = 0x800
                with source.open("rb") as stream:
                    output.writestr(info, stream.read())
        os.replace(temporary, archive)
    finally:
        if temporary.exists():
            temporary.unlink()


def cli() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    subparsers = parser.add_subparsers(dest="command", required=True)

    export_parser = subparsers.add_parser("export", help="export typed operations")
    export_parser.add_argument("--base", required=True, type=Path)
    export_parser.add_argument("--final", required=True, type=Path)
    export_parser.add_argument("--output", required=True, type=Path)
    export_parser.add_argument("--archive", type=Path)
    export_parser.add_argument("--base-label", default="vanilla-source")
    export_parser.add_argument("--final-label", default="game-source")

    verify_parser = subparsers.add_parser("verify", help="verify a bundle and its base tree")
    verify_parser.add_argument("--bundle", required=True, type=Path)
    verify_parser.add_argument("--base", required=True, type=Path)
    verify_parser.add_argument("--expected-bundle-sha256", type=str)

    apply_parser = subparsers.add_parser("apply", help="verify and reconstruct into a fresh output")
    apply_parser.add_argument("--bundle", required=True, type=Path)
    apply_parser.add_argument("--base", required=True, type=Path)
    apply_parser.add_argument("--output", required=True, type=Path)
    apply_parser.add_argument("--expected-bundle-sha256", required=True, type=str)

    hash_parser = subparsers.add_parser("hash", help="print a source tree manifest digest")
    hash_parser.add_argument("--source", required=True, type=Path)
    hash_parser.add_argument("--label", default="source")

    args = parser.parse_args()
    try:
        if args.command == "export":
            result = export_bundle(
                args.base,
                args.final,
                args.output,
                args.archive,
                base_label=args.base_label,
                final_label=args.final_label,
            )
        elif args.command == "verify":
            result = verify_bundle(args.bundle, args.base, args.expected_bundle_sha256)
        elif args.command == "apply":
            result = apply_bundle(args.bundle, args.base, args.output, args.expected_bundle_sha256)
        else:
            entries = scan_source(args.source)
            manifest = manifest_object(args.label, entries)
            result = {
                "file_count": len(entries),
                "manifest_sha256": _manifest_digest(manifest),
                "label": args.label,
            }
    except BundleError as exc:
        print(f"error: {exc}", file=sys.stderr)
        return 1
    print(json.dumps(result, indent=2, sort_keys=True))
    return 0


if __name__ == "__main__":
    sys.exit(cli())
