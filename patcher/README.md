# Eag19 patcher

Eag19 is a WebAssembly port of Minecraft 1.19.4. The game is compiled from
decompiled Minecraft source, so the project **never stores that source in Git**.
Instead, this directory contains the tools that reconstruct a complete project
from inputs you supply:

| Input | What it is | Where it comes from |
| --- | --- | --- |
| Base Java tree | Decompiled Minecraft 1.19.4 `net/minecraft` and `com/mojang/{blaze3d,realmsclient,math}` packages | `base-source/decompile_base.py`, run against the official client JAR you own |
| `eag19-source-patch.zip` | Deterministic bundle of Eag19's changes to that tree | A maintainer publishes it as a release asset |
| `eag19-project-skeleton.zip` | Every non-Minecraft file: Gradle build, TeaVM targets, browser platform, vendored libraries, docs | `project-skeleton/export_skeleton.py` |

The result is an ordinary Gradle workspace that builds the WASM-GC client.
Nothing in this repository is Mojang-derived; the patch bundle is distributed
outside Git and pinned by SHA-256.

## Requirements

- Python 3.10 or newer (standard library only)
- JDK 17 for building the reconstructed project
- A JDK on `PATH` for the one-time decompile step
- A Minecraft Java Edition 1.19.4 installation, or a decompiled tree of it you
  are entitled to use

## Build the base tree

Download the official `client.jar` and `client.txt` from the 1.19.4 entry of
<https://piston-meta.mojang.com/mc/game/version_manifest_v2.json>. The helper
pins SpecialSource 1.11.6 and Vineflower 1.12.0, verifies all four SHA-256
digests, remaps the jar to Mojang's official names, decompiles it with fixed
flags, and keeps only the packages the bundle touches:

```sh
python3 patcher/base-source/decompile_base.py \
  --client-jar client.jar \
  --expected-client-jar-sha256 <client.jar sha256> \
  --mappings client.txt \
  --expected-mappings-sha256 <client.txt sha256> \
  --output /path/to/1.19.4-java
```

The pinned tool hashes and the exact decompiler flags are constants in the
script. Changing them changes the base manifest and invalidates every published
bundle, so treat them as part of the release format. The command prints the
resulting file count and base manifest hash; record both in the release notes.

If you already have a tree produced by this exact recipe, skip the step and
pass it to `prepare_project.py` as `--base-source`.

## Reconstruct a project

```sh
python3 patcher/prepare_project.py \
  --base-source /path/to/1.19.4-java \
  --bundle eag19-source-patch.zip \
  --expected-bundle-sha256 <bundle sha256> \
  --skeleton eag19-project-skeleton.zip \
  --expected-skeleton-sha256 <skeleton sha256> \
  --output /path/to/eag19-project
```

`--base-source` is the directory that contains `net/minecraft` and
`com/mojang/...`. The output directory must not exist. On success it contains
the complete project plus `patcher-receipt.json`, which records every input
hash, the reconstructed Java tree hash, and the operation counts.

The command fails closed. It will not overwrite an existing directory, will not
accept an archive without an externally supplied SHA-256, and will not promote
the result unless the patched Java tree matches the bundle's final manifest
exactly. Tampered payloads, unsafe archive paths, duplicate members and
unexpected files are all rejected.

## Produce a bundle (maintainers)

Bundles are exported from two Java trees you control: the pristine decompiled
base and the working Eag19 tree. New files are stored in full, modifications as
byte-span deltas, deletions as their prior identity. Manifests bind every
operation to both trees.

```sh
python3 patcher/source-patches/export_source_patches.py export \
  --base /path/to/1.19.4-java \
  --final /path/to/eag19/src/main/java \
  --output /tmp/source-patch \
  --archive /tmp/eag19-source-patch.zip
```

Verify and apply:

```sh
python3 patcher/source-patches/export_source_patches.py verify \
  --bundle eag19-source-patch.zip \
  --base /path/to/1.19.4-java \
  --expected-bundle-sha256 <sha256>

python3 patcher/source-patches/export_source_patches.py apply \
  --bundle eag19-source-patch.zip \
  --base /path/to/1.19.4-java \
  --output /tmp/reconstructed-java \
  --expected-bundle-sha256 <sha256>
```

Exports are deterministic: the same two trees always produce the same archive
hash. The `.zip` is the only thing you publish.

`test_export_source_patches.py` covers round-trips, determinism, tamper
rejection, base-tree mismatch and archive path validation:

```sh
python3 patcher/source-patches/test_export_source_patches.py
```

## Produce the skeleton (maintainers)

The skeleton is built from Git-tracked files only, so a dirty build directory
can never leak into a release. It excludes the Minecraft-derived Java tree,
generated web output (`sites-web/public/data`) and Gradle build directories.

```sh
python3 patcher/project-skeleton/export_skeleton.py \
  --output eag19-project-skeleton.zip
```

`project-skeleton/include.txt` lists the tracked prefixes that belong in the
archive. `--print-manifest` dumps the per-file hashes without writing a ZIP. The
archive carries `SKELETON-MANIFEST.json`, and `prepare_project.py` verifies every
member against it in addition to the archive-level SHA-256.

## Release checklist

1. Build the base tree with `base-source/decompile_base.py` from the exact
   client JAR and mappings you intend to support.
2. Export the bundle against that base, then reconstruct a project from the
   generated skeleton and bundle.
3. Build it (`./gradlew :target_teavm_wasm_gc:generateWasmGC`) and run the
   client from `target_teavm_wasm_gc/javascript` on port 8097.
4. Export the skeleton from a clean checkout.
5. Publish both ZIPs as release assets. Put their SHA-256 digests and the base
   manifest hash in the release notes; the source tree stays free of Mojang
   code.
6. Run both Python test suites and record the reconstructed Java manifest hash.

Never commit `client.jar`, `client.txt`, `eag19-source-patch.zip`,
`eag19-project-skeleton.zip`, `patcher-receipt.json`, or an
extracted/decompiled Minecraft tree.

## Layout

| Path | Contents |
| --- | --- |
| `base-source/decompile_base.py` | Remaps and decompiles the official client JAR with pinned tools |
| `source-patches/export_source_patches.py` | Deterministic bundle exporter, verifier and applier |
| `project-skeleton/export_skeleton.py` | Builds the non-Minecraft skeleton from tracked files |
| `prepare_project.py` | Combines base tree, bundle and skeleton into a Gradle project |

## Legal notes

- Minecraft and its assets are copyright Mojang Studios. This repository
  contains neither decompiled game source nor game assets.
- A patch bundle contains game-derived code. Publish it only where you are
  allowed to; keep it out of Git and out of the public source tree.
- The skeleton retains third-party licences for the vendored components it
  ships. Keep them with the files.
- Everyone running the patcher must supply their own legally obtained copy of
  Minecraft 1.19.4.
