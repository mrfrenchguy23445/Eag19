#!/usr/bin/env python3

from __future__ import annotations

import sys
import tempfile
import unittest
import zipfile
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

import export_source_patches as sp


def write_tree(root: Path, files: dict[str, bytes]) -> None:
    for rel, data in files.items():
        path = root / Path(*rel.split("/"))
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(data)


BASE = {
    "net/minecraft/Alpha.java": b"package net.minecraft;\r\n\r\npublic class Alpha {\r\n   int a = 1;\r\n}\r\n",
    "net/minecraft/Beta.java": b"package net.minecraft;\r\n\r\npublic class Beta {\r\n}\r\n",
    "net/minecraft/Gone.java": b"package net.minecraft;\r\n\r\npublic class Gone {\r\n}\r\n",
    "com/mojang/Other.java": b"package com.mojang;\n\npublic class Other {\n}\n",
}

FINAL = {
    "net/minecraft/Alpha.java": (
        b"package net.minecraft;\r\n\r\npublic class Alpha {\r\n"
        b"   int a = 1;\r\n   int b = 2;\r\n   int c = 3;\r\n}\r\n"
    ),
    "net/minecraft/Beta.java": BASE["net/minecraft/Beta.java"],
    "net/minecraft/Added.java": b"package net.minecraft;\r\n\r\npublic class Added {\r\n}\r\n",
    "com/mojang/Other.java": b"package com.mojang;\r\n\r\npublic class Other {\r\n}\r\n",
}


class BundleTests(unittest.TestCase):
    def setUp(self) -> None:
        self.temp = tempfile.TemporaryDirectory(prefix="eag19-patch-test-")
        self.root = Path(self.temp.name)
        self.base = self.root / "base"
        self.final = self.root / "final"
        write_tree(self.base, BASE)
        write_tree(self.final, FINAL)

    def tearDown(self) -> None:
        self.temp.cleanup()

    def export(self, name: str = "bundle") -> tuple[Path, Path]:
        out = self.root / name
        archive = self.root / f"{name}.zip"
        sp.export_bundle(self.base, self.final, out, archive)
        return out, archive

    def test_round_trip(self) -> None:
        out, archive = self.export()
        digest = sp.sha256_file(archive)
        self.assertTrue(out.is_dir())

        result = sp.verify_bundle(archive, self.base, digest)
        self.assertEqual(result["counts"], {"add": 1, "modify": 2, "delete": 1})

        target = self.root / "reconstructed"
        sp.apply_bundle(archive, self.base, target, digest)
        for rel, data in FINAL.items():
            self.assertEqual((target / Path(*rel.split("/"))).read_bytes(), data, rel)
        self.assertFalse((target / "net/minecraft/Gone.java").exists())

    def test_export_is_deterministic(self) -> None:
        _, first = self.export("first")
        _, second = self.export("second")
        self.assertEqual(sp.sha256_file(first), sp.sha256_file(second))

    def test_different_base_is_rejected(self) -> None:
        _, archive = self.export()
        digest = sp.sha256_file(archive)
        other = self.root / "other-base"
        write_tree(other, {**BASE, "net/minecraft/Alpha.java": b"changed\r\n"})
        with self.assertRaises(sp.BundleError):
            sp.apply_bundle(archive, other, self.root / "out", digest)

    def test_missing_base_file_is_rejected(self) -> None:
        _, archive = self.export()
        digest = sp.sha256_file(archive)
        other = self.root / "other-base"
        write_tree(other, {k: v for k, v in BASE.items() if not k.endswith("Gone.java")})
        with self.assertRaises(sp.BundleError):
            sp.apply_bundle(archive, other, self.root / "out", digest)

    def test_tampered_payload_is_rejected(self) -> None:
        out, _ = self.export()
        payload = next(path for path in out.rglob("*") if path.is_file() and path.name.endswith(".delta"))
        payload.write_bytes(payload.read_bytes() + b" ")
        with self.assertRaises(sp.BundleError):
            sp.apply_bundle(out, self.base, self.root / "out", sp._expanded_bundle_sha256(out))

    def test_bundle_sha_pin_is_required(self) -> None:
        out, _ = self.export()
        with self.assertRaises(sp.BundleError):
            sp.apply_bundle(out, self.base, self.root / "out", None)

    def test_zip_member_paths_are_validated(self) -> None:
        archive = self.root / "evil.zip"
        with zipfile.ZipFile(archive, "w") as zf:
            zf.writestr("../escape.txt", b"x")
        with self.assertRaises(sp.BundleError):
            sp.verify_bundle(archive, self.base)

    def test_non_java_tree_is_rejected(self) -> None:
        (self.final / "notes.txt").write_text("hello", encoding="utf-8")
        with self.assertRaises(sp.BundleError):
            sp.export_bundle(self.base, self.final, self.root / "bad")


if __name__ == "__main__":
    unittest.main()
