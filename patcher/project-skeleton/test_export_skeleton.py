#!/usr/bin/env python3

from __future__ import annotations

import json
import subprocess
import sys
import tempfile
import unittest
import zipfile
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

import export_skeleton as sk


class GlobTests(unittest.TestCase):
    def test_single_star_does_not_cross_directories(self) -> None:
        self.assertTrue(sk.matches("target_teavm_wasm_gc/build/web", ("*/build/**",)))
        self.assertFalse(sk.matches("gradle/local-libs/com/resentclient/build/plugin/x.jar", ("*/build/**",)))

    def test_double_star_matches_any_depth(self) -> None:
        self.assertTrue(sk.matches("sites-web/public/data/assets.epk.00", ("sites-web/public/data/**",)))
        self.assertTrue(sk.matches("a/b/c/__pycache__/x.pyc", ("**/__pycache__/**",)))
        self.assertFalse(sk.matches("sites-web/public/index.html", ("sites-web/public/data/**",)))

    def test_class_files_match_at_any_depth(self) -> None:
        self.assertTrue(sk.matches("build/classes/A.class", ("**/*.class",)))
        self.assertFalse(sk.matches("build/classes/A.java", ("**/*.class",)))


class SkeletonTests(unittest.TestCase):
    def setUp(self) -> None:
        self.temp = tempfile.TemporaryDirectory(prefix="eag19-skeleton-test-")
        self.root = Path(self.temp.name)
        self.repo = self.root / "repo"
        self.repo.mkdir()
        files = {
            "build.gradle.kts": b"plugins {}\n",
            "sites-web/public/index.html": b"<html></html>\n",
            "sites-web/public/data/assets.epk.00": b"epk",
            "src/main/java/net/minecraft/Alpha.java": b"class Alpha {}\n",
            "src/main/java/com/mojang/logging/LogUtils.java": b"class LogUtils {}\n",
            "build/scratch.class": b"class",
            ".idea/workspace.xml": b"<xml/>\n",
        }
        for rel, data in files.items():
            path = self.repo / Path(*rel.split("/"))
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(data)
        self.git("init")
        self.git("add", ".")
        self.git("-c", "user.name=Test", "-c", "user.email=test@example.com", "commit", "-m", "init")

    def tearDown(self) -> None:
        self.temp.cleanup()

    def git(self, *args: str) -> None:
        subprocess.run(["git", "-C", str(self.repo), *args], check=True, capture_output=True)

    def test_selection_respects_includes_and_excludes(self) -> None:
        prefixes = ["build.gradle.kts", "sites-web/public", "src/main/java/net/minecraft", "src/main/java/com/mojang/logging"]
        files = sk.select_files(self.repo, prefixes, sk.DEFAULT_EXCLUDES)
        self.assertEqual(
            files,
            [
                "build.gradle.kts",
                "sites-web/public/index.html",
                "src/main/java/com/mojang/logging/LogUtils.java",
            ],
        )

    def test_archive_is_deterministic_and_verified(self) -> None:
        prefixes = ["build.gradle.kts", "sites-web/public", "src/main/java/com/mojang/logging"]
        files = sk.select_files(self.repo, prefixes, sk.DEFAULT_EXCLUDES)
        first = self.root / "first.zip"
        second = self.root / "second.zip"
        sk.write_archive(self.repo, files, first)
        sk.write_archive(self.repo, files, second)
        self.assertEqual(sk.sha256_file(first), sk.sha256_file(second))

        with zipfile.ZipFile(first) as archive:
            self.assertEqual(
                sorted(archive.namelist()),
                ["SKELETON-MANIFEST.json", *files],
            )
            manifest = json.loads(archive.read("SKELETON-MANIFEST.json"))
            self.assertEqual(manifest["format"], sk.FORMAT)
            self.assertEqual(manifest["file_count"], len(files))
            self.assertEqual([record["path"] for record in manifest["records"]], files)

    def test_symlink_is_rejected(self) -> None:
        if not hasattr(Path, "symlink_to"):
            self.skipTest("symlinks unavailable")
        target = self.repo / "sites-web/public/link.java"
        try:
            target.symlink_to(self.repo / "build.gradle.kts")
        except (OSError, NotImplementedError):
            self.skipTest("symlinks unavailable")
        self.git("add", "sites-web/public/link.java")
        with self.assertRaises(sk.SkeletonError):
            sk.select_files(self.repo, ["sites-web/public"], sk.DEFAULT_EXCLUDES)


if __name__ == "__main__":
    unittest.main()
