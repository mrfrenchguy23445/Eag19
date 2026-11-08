# Eag19 Optimization Mod Port Plan

Port context: vanilla 1.19.4 source under official-style names, TeaVM **wasm-gc**,
cooperative **single-threaded** executor (browser main thread). Every Java allocation
is real GC memory; no real threading exists, so all mod patches must be
allocation/CPU-oriented, not concurrency-oriented.

## Already landed (previous builds)
- Batch 1 worldgen: PlacedFeature stream pipeline → flat lists; 8 placement modifiers
  converted; primeHeightmaps swap-removal; Heightmap.update scratch mutable;
  Aquifer barrier memo fields; NoiseChunk indexed loops; applyBiomeDecoration loops.
- Batch 2 worldgen: ProtoChunk heightmapsAfter cache (per-block EnumSet→array),
  TreeFeature union/copy removal, MaterialRuleList indexed loop.
- Reliability: worldgen stall, save crash, particle Iterator.remove() purge.
- FPS defaults + one-time perfAutoTune migration; title screen tint removed (stale
  overlay fill deleted from TitleScreen.render).

## Verified wasm-gc boot (Nov 8)
- `classes.wasm` (unstubbed) boots to the title screen (`EditProfileScreen` behind
  LoadingOverlay) with **zero console errors**. Sweep on the same artifact:
  `status=clean masked=0`.
- Dev loader: `target_teavm_wasm_gc/javascript/index.html` (static, port 8097 via
  `scripts/serve-wasm-dev.mjs`, opts namespaced `_eaglercraftX_wasm_dev`).
- **Contract gotchas, both boot-fatal and easy to re-trip:**
  - rootElement must contain ONLY the empty `<div id="_eaglercraftX_game_root">`:
    the JS runtime re-positions `rootElement.lastChild` and a whitespace text
    node there crashes `initializeContext` with
    `Cannot set properties of undefined (setting 'position')`.
  - `window.eaglercraftXOpts.useJOrbisAudioDecoder = true` in dev: the stock
    `assets.epk` (Jun 6, built from desktopRuntime/resources) has no
    `assets/eagler/audioctx_test_ogg.dat`, and wasm-gc PlatformAudio calls
    `getRequiredResourceBytes` (hard-fail) for it. With the embedded JOrbis
    decoder enabled the probe is skipped entirely and OGG decoding runs in Java
    (the intended path for this target). `silence_loop.wav` absence is harmless
    (null-guarded). Long-term fix if wanted: synthesize ogg/wav32f/wav16 probe
    files into the EPK sources and rebuild the EPK, not the wasm.
- Boot log milestones: `Sound engine started`, `Reading 2 EPK files`,
  SimpleReloadInstance prepare/apply, `LightOverlay ... overlay finished,
  removing overlay`, then steady `EditProfileScreen` frames.

## Wave-2 (landed Nov 8, commit 956fa31)
1. `LayerLightEngine` chunk cache widened 2 → 4 slots (Lithium-style): the BFS
   regularly ping-pongs between two chunks at column/tree edges and a 2-slot
   MRU evicts the other hot chunk on every miss (50% thrash). Pure MRU widening,
   semantics unchanged.
2. Starlight Phase C step 1 (measure first): `ThreadedLevelLightEngine.runUpdate`
   now records a nanosecond "light bill" — task runnable time vs graph BFS time —
   and logs one `Diag: LightEngine: N batches in last X ms wall: tasks=.. ms,
   BFS=.. ms` line per 5 s window of *activity* (idle wall time is skipped).
   Next step pending this data: only port the full Starlight engine if the BFS
   share is actually large.
- Validation owed after next wasm build: sweep clean + import/classinfo gates +
  boot test + read the LightEngine Diag lines during worldgen and record the
  measured light share here.

## Phase A — FerriteCore port (next build)
Goal: kill block-state lookup overhead — the hottest remaining vanilla path
(setValue/getValue run constantly in feature placement, updateShapeAtEdge, model
building). Guava ArrayTable.get does two linear key-list scans per setValue.

1. `Property` — lazy value→index cache (equals-keyed; MUST be equals so autoboxed
   Integer values such as BlockStateProperties.DISTANCE resolve exactly).
2. `StateHolder` — flat parallel arrays replacing:
   - `ImmutableMap` hash lookups in getValue/hasProperty/getOptionalValue
   - `ArrayTable` neighbour table in setValue/trySetValue
   → `Property[] fastProperties`, `Comparable<?>[] fastValues`,
     `S[][] fastNeighbours` (value-indexed rows), built in populateNeighbours
     (boot-time, once per state). getValues()/codec keep the ImmutableMap so
     serialization, toString, and saves are untouched.
   Invariant: populateNeighbours IS called once per state by StateDefinition ctor
   before any setValue — verified.
3. (optional, memory-only) Tag flattening in TagLoader — defer unless memory shows up.

Validation: boot, worldgen profile timings (ChunkGenProfiler), features-stage ms down.

Post-mortem (Nov 6): the combo-builder rewrite (vanilla flatMap -> iterative) had a
fatal phantom-prefix bug (crashed boot before title screen). Fix keeps iterative
form but rebuilds the product list each step; net win over vanilla is only the
dropped stream pipeline nodes (allocation counts otherwise similar). If a real win
is wanted here later, replace with a one-pass odometer that allocates each Pair
combo list exactly once — boot-time only, low priority.

## Phase C — Starlight port (SCOPED, landed Nov 6 in this build)
Full Starlight engine rewrite was out of scope for one build (its instrumentation-first
step is still owed). What landed instead is the Starlight-style cut on the measured
hot path (`ProtoChunk.setBlockState → lightEngine.checkBlock` during FEATURES):

- **ThreadedLevelLightEngine.checkBlock coalescing.** During the FEATURES stage every
  qualifying write (air→solid transition is the common case) enqueued its own task
  through the sorter mailbox: Pair + two lambdas + Util.name wrapper + queue submit +
  pop + ask-future + Unit ack — all for one BlockPos that the engine ultimately folds
  into a section-level re-sweep on the next super.runUpdates. Now positions are
  accumulated into a per-light-section LongArrayFIFOQueue and ONE POST_UPDATE task per
  section burst replays them through the unchanged `super.checkBlock` path. Semantics
  are identical (light BFS converges; ordering between sections is irrelevant); the
  per-write mailbox traffic is gone.
- Behavioral notes:
  - A failing flush task loses that burst's positions, same as a vanilla task dying
    mid-run; the sorter's "always ack" fix keeps the queue draining regardless.
  - `flushingLightCheckSections` prevents duplicate flush tasks per section; entries
    are removed at flush start, so an exception mid-flush cannot wedge a section.
- Still owed if profiling later shows a large light share: LayerLightEngine
  per-section array caches (the true Starlight data layout) — its own build.

## Phase D — Sodium-inspired client meshing (CHERRY-PICKS ONLY, landed Nov 6)
Optioned "only if profiling says chunk rebuilding is a big FPS driver"; the dedupe
was zero-risk, so it went in with this build:

1. **RebuildTask.compile**: the second full `RenderChunkRegion.getBlockState` per
   block (4096 fetches per rebuild, after the block-entity branch) was deleted —
   the state fetched a few lines earlier is reused for the fluid phase
   (`renderLiquid(var17, ...)` matches vanilla's `var18` argument exactly).
2. **BlockCollisions.isAir fast path** (Phase B #2) doubles as the entity-movement
   side of the collision iteration cuts.
3. Per-state render-cull precomputation and mesh-build quad fixes remain NOT ported
   — Eaglercraft's JS renderer already replaces most of that scope; profile first.

## Phase B — Lithium selection (landed Nov 6, same build as A)
Portable logic patches only (skip all threading modules — single-threaded port):

| Module | Port? | Why |
|---|---|---|
| collections/hashing small-set rewrites | yes, selective | wasm-gc pays for every boxed key |
| entity collision iteration cuts | yes | per-frame physics cost on main thread |
| AI pathing/brain hot loops | maybe | matters on mob-heavy seeds |
| chunk serialization | skip | TDeflater already fixed; save path is local |
| threading/scheduler | skip | n/a in this port |
| math (sin/cos) | skip | vanilla Mth already table-based |

Concrete candidates to research while implementing:
1. World/entity section iteration paths (EntitySectionStorage.getEntities loops).
2. VoxelShape occasional-box merging costs in entity movement.
3. NeighborUpdater / tickScheduler allocation patterns.

## Phase C — Starlight light engine (own build, riskiest)
The biggest remaining chunk-gen lever. Current engine hits
`ProtoChunk.setBlockState → lightEngine.checkBlock` for qualifying block writes —
on our worldgen hot path. Port strategy (single-threaded adaptation):
1. Instrument first: measure current light cost share during NOISE/FEATURES.
2. Port Starlight's engine event listeners replacing LightEngine defaults —
   **drop the SWMR/threading layers** (cooperative executor makes them no-ops).
3. Watch for: our already-patched heightmap/light interplay in ProtoChunk.

## Phase D — Sodium-inspired client meshing (optional, scoped)
Not a Sodium port — Eaglercraft's JS renderer already replaces most of it.
Cherry-pick: per-state render-cull precomputation, chunk mesh build quad-level
fixes in the render-chunk builder. Only if profiling says chunk rebuilding is a
big FPS driver after phases A–C.

## Build cadence (superseded Nov 6: user directive "port ALL the mods at once")
Phases A+B+C-scoped+D-cherry-picks all land in ONE build:
  → stub pipeline (scripts/rebuild-stubs.sh — Linux/WSL path, needs node + python3 +
     wasm-tools; restore /tmp/opencode/wasm-tools-1.231.0-x86_64-linux if cleaned) →
  sweep must be clean → serve on 8097 (entry point: index.html — autokey.html no
  longer exists) → user boot test.
- Offline bundle regeneration (makeMainWasmClientBundle) after each validated build.
- Nov 8: sweep is CLEAN on the unstubbed build — the stub pipeline is NOT part of
  the loop anymore; only re-run it if a sweep ever goes non-clean again.

## Environment notes (recurring gotchas)
- gradle daemons can wedge during long EPK pack tasks: `./gradlew --stop` + kill
  stray java procs, then retry.
- Teadbg sidecar is only regenerated when stubs run; a clean (unstubbed) build
  leaves a stale .teadbg — crash trace decode scripts need a matching sidecar.
- Cloudflare publish: `npx wrangler` v4.157.0 ready; awaiting user's
  `CLOUDFLARE_API_TOKEN` or `wrangler login`.
- Desktop runtime (target_lwjgl_desktop) is UNCOMPILABLE in this workspace: the
  src/lwjgl platform layer targets a newer upstream branch (needs voice/relay/
  LAN/SP-singleplayer/webview/recording packages this branch's src/main lacks).
  desktopRuntime jars (paulscode etc.) were restored Nov 6 from the Eagler-
  Versions/eaglercraft-1.14-workspace GitHub mirror after the EaglercraftX-1.8
  workspace archive got DMCA'd. 6 platform files excluded via
  target_lwjgl_desktop/build.gradle.kts, still ~10 more classes short. Validation
  therefore happens through the wasm build + browser boot test only.
- `makeMainWasmClientBundle` re-executes generateWasmGC when the epk changes —
  always re-run the stub pipeline after ANY wasm-producing task, and regenerate
  the bundle only AFTER the stubbed module is installed (bundle regen is slow:
  ~30m).
- Boot crash `Cannot set property <X> as it does not exist in
  Block{minecraft:air}` (Nov 6, FerriteCore A build): the decoded owner is
  MISLEADING — blocks mid-`Blocks.<clinit>` are unregistered, and
  `DefaultedMappedRegistry.getKey` falls back to the registry defaultKey, which
  for BLOCK is `minecraft:air`. Do not take "air" literally; the real owner is
  whichever block's static init died. Root cause that time: the FerriteCore
  iterative combo expansion in `StateDefinition` appended into the same list and
  kept partial-prefix phantom states, so `stateDefinition.any()` returned an
  empty-values ghost and every `registerDefaultState(any().setValue(...))`
  (first victim: SnowyDirtBlock) threw. Fixed by rebuilding the product into a
  fresh list per property step (same order as vanilla flatMap: first property
  varies slowest).
- Crash overlay frames are TeaVM-decoded garbage; exception message + type are
  reliable. Reproduce yourself via a fresh `index.html` tab (fast boot and
  deterministic), console tail via browser console tool, then reason from
  message + source, not from the frame list.

## EPK toolchain + eagler asset/lang mount (Nov 8)

- The stock assets.epk has NO `assets/eagler/*` entries. Skins/GLSL/audioctx/
  icudt/eagtek were sourced from the official EaglercraftX 1.8.8 WASM offline
  download (cdn.eaglercraft.net/objects/dl/1.8.8/eaglercraft1-8-wasm-
  62da5f39.zip): that HTML embeds one base64 data-URI holding an `EAG$WASM`
  EPW container; `scripts/epk-dump-offline.mjs` extracts it, `scripts/epw-
  split.py` (WSL python, xz) splits the EPW, `scripts/epk-extract-direct.mjs`
  pulls `assets/eagler/**` (410 entries), `scripts/make-folder-epk.mjs` packs
  them into `javascript/eagler.epk` (mounted LAST in dev index.html so eagler
  entries win duplicates).
- Raw-key title/profile screen (`menu.editprofile`, `eagler.profile.*`):
  ClientPackSource exposes the `eagler` resource namespace, and
  ClientLanguage.loadFrom loads `lang/en_us.json` from EVERY namespace, so
  dropping `assets/eagler/lang/en_us.json` into eagler.epk merges the eagler
  strings as pure data - no Java change, no wasm rebuild. The strings are
  lifted from upstream 1.8 en_US.lang (`eaglercraft.*` keys) by
  `scripts/make-eagler-lang.mjs` (repo copy: scripts/eagler-lang-en_us.json
  overlay), with port-renamed aliases menu.editprofile / eagler.profile.title
  / eagler.profile.username added manually.
- Custom EPK writers must emit, in order: `EAGPKG$$` magic, len-prefixed
  `ver2.0`, filename(u8-len), comment(u16-len), date(floatBE, 8 bytes),
  numFiles(u32), comp char; then gzip(comp 'G') stream; terminator `:::YEE:>`.
  FILE entries: tag + name + u32(len=4crc+data+1':') + u32 crc + data + ':',
  each entry ends with '>'. Verify with scripts/epk-list.mjs (crcBad=0).
- Dev server data pack: `javascript/data.epk` from
  src/wasm-gc-teavm/resources/minecraft-1.19.4-vanilla-data.zip via
  scripts/make-data-epk.mjs (SERVER_DATA pack type; without it CreateWorld
  crashes "Missing key ... world_preset: minecraft:normal"). Confirmed working
  Nov 8: world generated end-to-end, "chunks/s 52-69", zero console errors.
- EditProfileScreen is the first-boot screen (no panorama by design; the
  panorama renders on the TitleScreen after confirming the profile).
