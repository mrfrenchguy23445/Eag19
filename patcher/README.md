# Patcher

This folder rebuilds the full Eag19 workspace without storing Mojang source in Git.

You give it three things:

1. A decompiled Minecraft 1.19.4 tree. You make this from the official client JAR.
2. A patch bundle. It contains only our changes to that tree.
3. A skeleton archive. It contains everything that is not Minecraft source: Gradle files, browser code, scripts, docs, and vendored libraries.

It gives you a normal Gradle project you can build.

## How it works

```text
official 1.19.4 client.jar + mappings
                |
                v
      decompile_base.py
                |
                v
          base Java tree  ------+
                                |
Eag19 source tree ---------> export_source_patches.py ---> patch bundle
                                |
Git-tracked project files ---> export_skeleton.py ------> skeleton archive
                                |
                                v
                       prepare_project.py
                                |
                                v
                       complete Gradle project
```

`prepare_project.py` checks every input hash before it writes anything. It rejects bad
paths, duplicate archive members, symlinks, and tampered manifests. If the final Java
tree does not match the bundle manifest, it deletes the output and fails.

## Requirements

- Python 3.10 or newer
- JDK 17
- A legally obtained Minecraft Java Edition 1.19.4 client

The Python scripts use only the standard library.

## Rebuild the project

### 1. Make the base tree

Download `client.jar` and `client.txt` from the 1.19.4 entry in
<https://piston-meta.mojang.com/mc/game/version_manifest_v2.json>.

```sh
python3 patcher/base-source/decompile_base.py \
  --client-jar client.jar \
  --expected-client-jar-sha256 <client.jar sha256> \
  --mappings client.txt \
  --expected-mappings-sha256 <client.txt sha256> \
  --output /path/to/1.19.4-java
```

On Windows, replace the trailing `\` with `^` or put the command on one line.

The script pins SpecialSource and Vineflower, checks their hashes, remaps the JAR,
decompiles it, and keeps only the packages the patch bundle touches. The command prints
the base manifest hash. Save it for release notes.

If you already have a base tree made with this exact recipe, skip this step.

### 2. Get the two archives

Take `eag19-source-patch.zip` and `eag19-project-skeleton.zip` from the same release.
Do not mix releases. Their hashes must be copied into the next command.

```sh
sha256sum eag19-source-patch.zip eag19-project-skeleton.zip
```

On Windows:

```bat
Get-FileHash -Algorithm SHA256 eag19-source-patch.zip
Get-FileHash -Algorithm SHA256 eag19-project-skeleton.zip
```

### 3. Put them together

```sh
python3 patcher/prepare_project.py \
  --base-source /path/to/1.19.4-java \
  --bundle eag19-source-patch.zip \
  --expected-bundle-sha256 <bundle sha256> \
  --skeleton eag19-project-skeleton.zip \
  --expected-skeleton-sha256 <skeleton sha256> \
  --output /path/to/eag19-project
```

The output directory must not exist. When it finishes, it contains the whole project
and a `patcher-receipt.json` with every input hash and operation count.

### 4. Build and run

```sh
cd /path/to/eag19-project
./gradlew :target_teavm_wasm_gc:generateWasmGC
node scripts/serve-wasm-dev.mjs
```

Open <http://localhost:8097>. Use Chrome or Edge 137+ because the client needs JSPI.

## Make a new patch bundle

Maintainers keep a clean base tree and the working Eag19 tree side by side.

```sh
python3 patcher/source-patches/export_source_patches.py export \
  --base /path/to/1.19.4-java \
  --final /path/to/eag19/src/main/java \
  --output /tmp/source-patch \
  --archive /tmp/eag19-source-patch.zip
```

Check it before publishing:

```sh
python3 patcher/source-patches/export_source_patches.py verify \
  --bundle /tmp/eag19-source-patch.zip \
  --base /path/to/1.19.4-java \
  --expected-bundle-sha256 <sha256>

python3 patcher/source-patches/export_source_patches.py apply \
  --bundle /tmp/eag19-source-patch.zip \
  --base /path/to/1.19.4-java \
  --output /tmp/reconstructed-java \
  --expected-bundle-sha256 <sha256>
```

Exports are deterministic. The same two trees always produce the same ZIP hash.

## Make a new skeleton

The skeleton only reads Git-tracked files. Clean or commit your work first.

```sh
python3 patcher/project-skeleton/export_skeleton.py \
  --output eag19-project-skeleton.zip
```

`--print-manifest` prints the per-file hashes without writing the ZIP.
`project-skeleton/include.txt` controls what goes in.

## Tests

```sh
python3 patcher/source-patches/test_export_source_patches.py
python3 patcher/project-skeleton/test_export_skeleton.py
```

## Do not commit these

- `client.jar` or `client.txt`
- any decompiled Minecraft tree
- `eag19-source-patch.zip`
- `eag19-project-skeleton.zip`
- `patcher-receipt.json`

Patch bundles contain game-derived code. Publish them only where you are allowed to.

## Layout

- `base-source/decompile_base.py` — makes the base tree
- `source-patches/export_source_patches.py` — makes, verifies, and applies bundles
- `project-skeleton/export_skeleton.py` — makes the non-Minecraft skeleton
- `prepare_project.py` — combines all three inputs into a buildable project

Minecraft and its assets belong to Mojang. This repository does not contain them.
