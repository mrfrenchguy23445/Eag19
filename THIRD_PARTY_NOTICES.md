# Third-party notices

Eag19's own code is MIT; see [LICENSE](LICENSE). Third-party components keep their own licences.

The GPLv3 Via stack is **local-only**: its binaries, source archives, assets and
WASM-GC adaptations are ignored by Git and are not part of this repository. The
public tree keeps only the platform-neutral `EaglerProtocolBridge` seam.

## ViaVersion 4.10.2

- Licence: GPLv3 — local file `gradle/local-libs/com/viaversion/viaversion/4.10.2/LICENSE`
- Source: <https://github.com/ViaVersion/ViaVersion/tree/4.10.2>
- Maven source jar: <https://repo.viaversion.com/com/viaversion/viaversion/4.10.2/viaversion-4.10.2-sources.jar>
- Local binary: `gradle/local-libs/com/viaversion/viaversion/4.10.2/viaversion-4.10.2.jar`
- WASM-GC adaptations: the maintainer's local `src/wasm-gc-teavm/java/` overrides
  `InformativeException` and Via's shaded Gson; those files are gitignored.

## ViaBackwards 4.10.2

- Licence: GPLv3 — local file `gradle/local-libs/com/viaversion/viabackwards/4.10.2/LICENSE`
- Source: <https://github.com/ViaVersion/ViaBackwards/tree/4.10.2>
- Local source archive: `gradle/local-libs/com/viaversion/viabackwards/4.10.2/viabackwards-4.10.2-sources.zip`
- Local binary: `gradle/local-libs/com/viaversion/viabackwards/4.10.2/viabackwards-4.10.2.jar`

## SnakeYAML 2.0

- Licence: Apache 2.0
- Source: <https://github.com/asomov/snakeyaml/tree/v2.0>
- Local binary: `gradle/local-libs/org/yaml/snakeyaml/2.0/`

## Gson 2.10

- Licence: Apache 2.0
- Vendored, WASM-adjusted source: `src/main/java/com/google/gson/`

## Redistributing a WASM build

A built HTML/WASM file containing the Via binaries is a GPLv3 distribution. Ship
this notice, the two GPLv3 licence files, and the corresponding source (the
linked upstream tags plus the local WASM-GC adaptations). Do not label the whole
binary MIT. If you do not distribute the Via-enabled build, keep the libraries
local as this repository does.
