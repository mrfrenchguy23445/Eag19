# Singleplayer implementation and verification

The client starts `IntegratedServer` on the client thread, pumps server
work from the render loop at 20 ticks per second, and exchanges queued packet
objects with the server. Local login and gameplay do not bind a Netty socket.
World initialization renders chunk progress and yields to browser events while
waiting for scheduled work. Disconnecting stops the server and drains its normal
player/chunk saving path before releasing the world lock.

The WASM client installs `PersistentVirtualFileSystem` after the Eagler runtime
initializes and before Minecraft starts. Both `java.io` and `java.nio.file` then
use the configured `worldsDB` IndexedDB database, with files, empty directories,
and modification times stored under `eag19-vfs/`. A configured RAM-disk mode still
uses temporary storage. The resource-pack database is separate from world saves.

Open to LAN remains unavailable in the browser. Local players use the transport's
`local` address; TCP peer-IP authentication hints and the dedicated `ban-ip`
and `pardon-ip` commands are not part of the browser server.

## Build

Use Java 17 and run:

```sh
./gradlew :target_teavm_wasm_gc:generateWasmGC \
  :target_teavm_wasm_gc:compileMainWasmRuntime \
  :target_teavm_wasm_gc:compileMainEpk
```

Serve `target_teavm_wasm_gc/javascript` over HTTP and open it in a browser with
WebAssembly GC and JSPI support. The EPK packing task depends on extraction of the
bundled 1.19.4 server data, including world-generation registries.

## Focused regression checks

```sh
scripts/verify-singleplayer.sh
```

These JVM checks cover ordered, bidirectional local packet delivery, deferred
packet handling, disconnect propagation, positional chunk-sector operations,
sparse writes, file truncation, persistent directory metadata, reopening files,
append, moving world directories, and chunk-save barriers that await generation. The filesystem test recreates the provider
against the same test store to model a reload; it does not replace an actual
IndexedDB/browser test.

## Browser acceptance checks still required

A successful Java build and the focused checks are insufficient to claim a
finished browser singleplayer build. Verify the fresh, unmodified WASM output:

1. Finish profile setup and reach the title screen without a crash.
2. Create survival and creative worlds, including a fixed seed and a flat world.
3. Spawn, move across chunk boundaries, break/place blocks, use inventory and
   crafting, and exercise entity interactions and death/respawn.
4. Pause and resume; verify that local simulation pauses and settings apply.
5. Save and quit, reopen the world, then reload the page and reopen it again.
   Check player position, inventory, placed blocks, entities, and world time.
6. Verify world naming, edit, recreate, and deletion flows, plus returning to the
   menu after a failed load.

At the time these changes were made, the checked-in browser binary failed during
entity-data initialization before reaching the title screen. Browser acceptance
must be performed against a newly built artifact, without replacing failing
compiler-generated method bodies with placeholder stubs.
