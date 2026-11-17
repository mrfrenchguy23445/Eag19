# Eag19

A browser port of Minecraft **1.19.4** that runs as WebAssembly, built on
EaglercraftX's platform layer and TeaVM's WASM-GC backend.

The client boots from `index.html` with no plugins, talks to vanilla-protocol
1.19.4 servers over WebSocket, and supports singleplayer, the creative menu,
resource packs (including drag-and-drop import) and the vanilla chunk-status
loading screen.

## Status

| Area | State |
| --- | --- |
| Singleplayer world generation | Works; chunk status screen completes without stalling |
| Multiplayer | Enabled; uses the Eaglercraft WebSocket handshake |
| Item/block textures | Rendered with the cutout render type |
| Sounds and music | Gameplay, UI and background music |
| Resource packs | Browser upload and drag-and-drop import into the virtual filesystem |

WebGL pointer lock is used when the browser grants it; if it is denied the
client falls back to a virtual grab so the mouse still controls the camera.

## Requirements

- JDK 17 (`JAVA_HOME` must point at it)
- A modern desktop browser with WebAssembly GC support (Chrome 119+ or Edge)

The Gradle wrapper resolves its dependencies from `gradle/local-libs`; no
network access is required after the wrapper distribution is cached.

## Build

```sh
# Linux/macOS
./gradlew :target_teavm_wasm_gc:generateWasmGC

# Windows
gradlew.bat :target_teavm_wasm_gc:generateWasmGC
```

A full WASM-GC build takes about 45 minutes on a desktop CPU and needs roughly
10 GiB of build budget. The output lands in
`target_teavm_wasm_gc/javascript/` as `classes.wasm`, `eagruntime.js` and the
EPK asset archives.

## Run the dev client

Serve the output directory over HTTP (the loader needs real HTTP, not
`file://`):

```sh
python -m http.server 8097 --directory target_teavm_wasm_gc/javascript
```

Then open <http://localhost:8097/>. Optional query flags used during
development:

- `?keepPixels=1` disables framebuffer clearing (screenshot debugging)
- `?autofocus=1` focuses the canvas without a click
- `?glspy=1` logs WebGL calls

## Multiplayer

The client uses the Eaglercraft WebSocket handshake (v5, game protocol `762`)
and cannot connect to a Java TCP port directly. You need an EaglerXServer
listener in front of a Paper 1.19.4 backend. See [SERVER.md](SERVER.md) for a
step-by-step Velocity + Paper setup, `wss://` configuration and troubleshooting.

For a handshake-only local check, run `node scripts/mock-eagler-server.mjs 8080`
and add `ws://localhost:8080`; the client should reach "Joining world..." and
wait there because the mock sends no play packets.

## Layout

| Path | Contents |
| --- | --- |
| `src/main/java/net/minecraft` | Minecraft 1.19.4 game code (see *Game source* below) |
| `src/main/java/net/lax1dude` | EaglercraftX platform and protocol code |
| `src/main/java/com/mojang/{blaze3d,realmsclient,math}` | Additional decompiled client packages |
| `src/wasm-gc-teavm` | Browser platform implementation (JS + Java) |
| `target_teavm_wasm_gc` | WASM-GC target, loader and web output |
| `target_teavm_javascript` | JavaScript target |
| `target_lwjgl_desktop` | Desktop/LWJGL target |
| `src/lwjgl` | Desktop platform implementation |
| `patcher` | Deterministic source-patch tooling (see below) |
| `scripts` | Build and verification helpers |

## Game source

Decompiled Minecraft code is copyright Mojang Studios and is not ours to
publish. This repository is therefore structured around a patcher: the public
source tree ships the build system, the browser platform, vendored libraries and
the patcher itself, while the game-derived tree is reconstructed locally from a
Minecraft installation the user owns.

The patcher takes three inputs:

1. a decompiled Minecraft 1.19.4 Java tree,
2. `eag19-source-patch.zip` — a hash-pinned bundle of Eag19's changes,
3. `eag19-project-skeleton.zip` — everything that is not Mojang-derived.

It verifies all three hashes and writes a ready-to-build Gradle project. See
[`patcher/README.md`](patcher/README.md) for the commands, the bundle format and
the release checklist.

If you are working in a checkout that already contains the game source (for
example a maintainer's private tree), you can build directly with the commands
above.

## Licence

Eag19's own code is released under the MIT licence; see [LICENSE](LICENSE).
Third-party components keep their own licences and notice files. Minecraft and
its assets remain copyright Mojang Studios and are not part of this repository.
