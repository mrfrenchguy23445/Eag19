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

## Phase B — Lithium selection (same build)
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

## Build cadence
- Phases A+B together → ONE build ("all optimizations in one build" directive).
  → stub pipeline (scripts/rebuild-stubs.sh auto; restore
  /tmp/opencode/wasm-tools-1.231.0-x86_64-linux symlink if /tmp was cleaned) →
  sweep must be clean → serve on 8097 (entry point: index.html — autokey.html no
  longer exists) → user boot test.
- Phase C → its own build after A+B validate.
- Offline bundle regeneration (makeMainWasmClientBundle) after each validated build.

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
