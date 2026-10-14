# Aervium Architecture

How Aervium is built, which rules every optimization must keep and in which order the engine grows. Read this before changing anything, and before starting a new phase.

## Goal

Aervium is the performance engine of the Aryston series for Minecraft 26.3 on NeoForge with the Vulkan backend. It replaces Sodium and its companion mods (FerriteCore, ImmediatelyFast, ModernFix, EntityCulling, More Culling and Leaf Culling, Lithium, C2ME, ScalableLux, Krypton, Dynamic FPS, Sodium Extra, Distant Horizons) with built-in modules that can be turned on and off, for an ecosystem of vanilla, Helion and Arkea. Render optimization lives in Aervium; Helion builds its visuals on Aervium's draw path.

## Rules

These rules decide every design question. A change that breaks one is not merged, whatever it gains.

1. **20 TPS always.** The server tick stays under 50 ms (MSPT). Server time is never traded for frame rate.
2. **Smoothness over peak FPS.** Success is a higher 1% low and 0.1% low and a steady frame time (low standard deviation, no spikes in the graph), measured with the benchmark. A change that raises the average but lowers a low is a regression.
3. **Vanilla behavior is identical.** Redstone timing, farms, mob behavior, world generation and saved data behave exactly as in vanilla. Optimizations change how fast something is computed, never what is computed.
4. **Clean room.** Sodium (PolyForm Shield) and Lithium (LGPL) are never read or copied. Known techniques are implemented from public descriptions (papers, talks, articles) only. Ports from permissive projects follow rule T1 and T2 of the workspace.
5. **Passive next to other renderers.** With Sodium or Iris installed every optimization stays off; measurement still works.
6. **Thin adapter.** Everything Minecraft-specific goes through the adapter layer, so a new Minecraft version only touches that layer.
7. **No allocation in the frame.** Work that runs every frame uses preallocated memory: rings and arrays on the heap now, off-heap frame arenas (FFM) from Phase 1.
8. **Measure first.** Every optimization starts with a benchmark run before and after, on the same settings, and lands only when the benchmark shows it (see Measuring).

## Layers

```
Minecraft and NeoForge
   │  events (RenderFrameEvent, ClientTickEvent, ServerTickEvent, GUI layers), mixins
   ▼
Adapter layer                 the only code that touches Minecraft or NeoForge
   integration/               both sides: server events, mod list checks
   client/                    client side: frame events, keys, overlay drawing, VRAM probe
   client/benchmark/          benchmark world, camera and environment
   mixin/                     accessors and hooks where no API exists
   config/                    NeoForge config spec
   ▼
Engine (game-independent, unit tested)
   core/telemetry             ring buffers, statistics, GC pauses, snapshots
   core/budget                frame budget, timed systems, timing scopes
   core/job                   work-stealing job system, main-thread queue
   core/culling               software depth raster, occluder boxes, occlusion passes
   world/                     server-side engine parts (tick recorder now, TPS work in Phase 4)
   debug/route, debug/benchmark  camera routes, benchmark scenes, commands, reports
```

Engine code never imports `net.minecraft` or `net.neoforged` classes (Gson and SLF4J are allowed). Common code never references client classes.

## Frame and Tick Flow

```
RenderFrameEvent.Pre   frame start: FrameTimeRecorder (overlay), BenchmarkDriver (when running), FRAME scope begins
GameRenderer.render    vanilla, Helion or later Aervium draw path
GUI layers             TelemetryOverlay draws text and the frame graph (OVERLAY scope)
RenderFrameEvent.Post  FRAME scope ends, MainThreadQueue drains job results within 0.5 ms

ClientTickEvent.Pre/Post   CLIENT_TICK scope; every 5 ticks ClientTelemetry copies raw data and hands it to a worker
ServerTickEvent.Pre/Post   TickTimeRecorder: one MSPT sample per tick, SERVER_TICK scope
GC notifications           GcPauseRecorder on the JMX thread
```

Raw data is copied on the thread that writes it, statistics are computed on a worker (`TELEMETRY` scope), and results come back through `MainThreadQueue`. The overlay itself only reads a preallocated array and cached strings.

## Engine Systems

### Telemetry
- `SampleRing` stores nanosecond samples; recording never allocates. Marks (`recorded()`) cut windows out of a ring without copying it.
- Frame time is the time between two `RenderFrameEvent.Pre`, so it includes everything a player waits for: game logic on the client thread, rendering, presentation and frame limiting.
- 1% low is the frame rate at the 99th percentile frame time and 0.1% low at the 99.9th (nearest rank). Standard deviation is the population standard deviation of frame times.
- MSPT is the time from `ServerTickEvent.Pre` to `Post`, recorded on the integrated and the dedicated server.
- GC pauses come from the collector notifications; concurrent cycles of ZGC and Shenandoah are not pauses.
- VRAM is the device-local usage the game's Vulkan memory allocator reports; it counts what Minecraft allocated, not other programs.

### Frame Budget
`FrameBudget` turns the target frame rate into a frame time and gives every `TimedSystem` its share. Server systems always get 50 ms. Today the budget colors the overlay; from Phase 1 every system that does deferrable work (chunk uploads, mesh builds, light updates) spends only its share per frame and carries the rest over.

### Job System
`JobSystem` is a work-stealing pool with two cores left to the game threads. Jobs never touch Minecraft state; they get copies and hand results back through `MainThreadQueue`, which runs them within a time budget. Chunk builds run on their own `JobSystem` (`MeshWorkers`) instead of vanilla's shared background pool, where they queued behind the integrated server's chunk generation and saving; vanilla's own queue still picks the nearest section first. Job priorities and dependencies come with Phase 2's own mesher.

### Shader and Pipeline Caches
Vanilla compiles every GLSL shader to SPIR-V with shaderc at each start and resource reload, and creates its Vulkan pipelines without a `VkPipelineCache`.

- `SpirvCache` keys each compilation by its full input (`ShaderCacheKey`) and keeps the SPIR-V in `<game>/aervium/cache/spirv/`. Hits skip shaderc; misses compile as vanilla and write the result on a worker.
- `VulkanPipelineCache` hands one pipeline cache to every pipeline creation, loads it from `<game>/aervium/cache/vulkan-pipelines.bin` when its header matches the GPU and driver, and saves it on shutdown.
- `PipelineWarmup` compiles the built-in pipelines vanilla would only compile on the render thread at first use and inserts them through the budgeted main-thread queue.
- The startup report in the log (`Aervium startup: ...`) shows the time to the first screen, shader hits and misses and pipeline creation time; `PipelineCacheMixin` logs every pipeline compiled late on the render thread.

### Occlusion Culling
Vanilla's cave culling only knows whether two faces of a section are connected by air; a cave behind a stone wall still gets drawn when air connects it to the camera somewhere. Aervium adds a software occlusion test, the technique console games used before GPUs could do it themselves.

- While a section mesh is built, its opaque blocks (the same ones vanilla's `VisGraph` sees) become up to 24 occluder boxes.
- Camera pass, every frame: the visible sections are sorted near to far; each is tested against a 256-pixel-wide depth raster and, if visible, draws its occluder boxes into it. Hidden sections are skipped in the draw list only; vanilla's visible list stays untouched, so section updates, translucency sorting and entities work as before.
- The camera pass runs on its own worker, one frame behind (`OcclusionPipeline`), so the render thread only copies the section list and never waits. Running it on the render thread cost half the frame rate at render distance 32 (98.8 to 47.1 FPS on Can's PC, 2026-10-12). A late result is only used while the eye stays within its reach (a margin from the last frame's movement that every test box grows by); the raster covers 25 % more than the screen and a box crossing its edge counts as visible, because visibility from one eye position does not change when the camera turns.
- Shadow pass (with Helion): the same test per cascade in light space with an orthographic raster, on its own worker (`Aervium Shadow Culling`, one `OcclusionPipeline` per cascade). The render thread only hashes each cascade (light direction, section list, meshes, fade state) and applies a finished result while the hash still matches, so the result is exact; while a cascade changes every frame its casters are all kept. A caster the light cannot see is lossless to drop, because the shadow map stores only the nearest depth and the casters in front of it are in the same cascade. Running this pass on the render thread cost a quarter of the frame rate at render distance 32 (100.7 to 76.3 FPS average, 1% low 58.7 to 28.1 on Can's PC, 2026-10-14).
- Both tests are conservative (see `DepthRaster`), so nothing visible is ever dropped. Sections still fading in never occlude.
- `-Daervium.cullingAudit=true` checks this in game: rays through the real blocks to hidden sections must never get through (`CullingAudit`). The benchmark log shows the share of sections the camera pass hid and of shadow casters the shadow pass hid per scene.

### Face Culling
The GPU throws away a triangle that faces away from the camera only after its vertices went through the vertex shader. Aervium groups the quads of every solid and cutout section mesh by the side they face while the mesh is built (`FacingSort`) and, in the camera pass, leaves out the groups whose planes all lie in front of the eye, so a section seen from one side sends only up to three of its six face directions to the GPU. Only faces the GPU would cull are left out, with half a block of margin for view bobbing; shadow passes and any other view keep every face.

### Modules
Optimizations that change game classes are mixins grouped into modules (`OptimizationModule`). `MixinModules` reads `config/aervium-modules.properties` before any mixin applies and writes every switch back; renderer modules also stay off with Sodium or Iris. Features that run later (dynamic frame rate) use the normal client config.

## Benchmark

`-Daervium.benchmark=true` (works with `launcher/launch.ps1 -Define`) starts the benchmark from the first screen; `-Daervium.benchmark.quit=true` closes the game afterwards.

1. Deletes the previous `aervium-benchmark` world and creates a fresh one: seed 8675309, spectator, peaceful, cheats on.
2. Freezes time at noon and the weather, turns mob spawning off, finds the spawn, the nearest village and the redstone platform height on the server thread.
3. Warms up for 400 ticks at the start of the first route.
4. Runs every scene: 200 ticks of settling (camera already moving, world set up), then 600 measured ticks.

| Scene | Route | Stresses |
|---|---|---|
| walk | east from the spawn at walking speed, following the ground | steady terrain rendering |
| elytra | south at 1.6 blocks per tick, 80 blocks above the spawn | chunk generation, meshing and uploads |
| village | orbit around the village center with 40 summoned villagers | entities, AI, block entities |
| underground | west at y -30 through stone and caves | occlusion, cave visibility |
| redstone | orbit around 96 observer clocks with sticky pistons and lamps | block updates, light updates, server ticks |

The report goes to `<game folder>/aervium/benchmarks/benchmark-<yyyyMMdd-HHmmss>.json` and `.txt` and is also printed to the log. It compares itself with the newest earlier report. Repeatability target: average FPS of every scene within ±2 % between two runs on the same settings; the frame limit, vsync, window size, render distance and installed mods are written into the report because they change the result.

## Vanilla Parity Check

`./gradlew runParity` starts a dedicated server in `runs/parity` with a fresh flat world, runs a fixed scenario and stops; `-PparityModules=off` writes every module as off before the run and `-PparityOff=a,b` turns off only the named modules.

1. Seed 1095062134, flat grass world, no structures, time, weather and mob spawning frozen, the server never pauses when empty.
2. After 40 ticks the level random and the random tick position generator get fixed seeds and `ParityScenario` builds an observer clock with a sticky piston, a hopper chain between chests, a burning furnace, a wheat farm, water and lava, saplings, sugar cane and torches in 16 force-loaded chunks.
3. Every tick a fixed sequence places light sources and blockers (glowstone, sea lanterns, stone, glass, slabs, stairs, leaves, tinted glass, air) in a 40 x 8 x 8 light lab, 4 per tick or `aervium.parity.lightChanges`, so block and sky light keep changing.
4. After every tick the harness waits until the light worker has finished every queued task for the scenario chunks. Vanilla lights on a worker thread, so without this a random tick that reads light (grass dying under a new block) would depend on thread timing.
5. Every 200 ticks for 1200 ticks the log prints three hashes of the 48 x 48 x 21 area: blocks (state, block entity data, scheduled ticks), light (block and sky light) and entities.

Two runs with the same settings must print the same hashes, and a run with every module off must print the same hashes as a run with every module on. Any optimization that touches world logic has to keep this true; a different hash shows the first checkpoint where the world diverged.

`-Daervium.parity.worldgen=true` with `-PparityWorld=normal` runs the worldgen check instead: ticks are frozen, 25 x 25 chunks (`aervium.parity.worldgenRadius`, 12) generate on the fixed seed, and `ChunkStatusTasksTerrainMixin` hashes every chunk right after its terrain step (noise, surface, carvers, biomes). The log prints one terrain hash, a save and load round trip of every section (mismatches must be 0), the generation time and the process CPU time. Features are left out on purpose: vanilla places overlapping features of neighbouring chunks in thread order, so two plain vanilla runs already differ there. CI runs this check with modules on and off as well.

`-Daervium.parity.mobs=N` with `-PparityWorld=normal` and a force radius spawns N cows, sheep, pigs, chickens and villagers on dry land over the force-loaded square and logs the mean and longest server tick, wall and server thread CPU; this is the server load benchmark (hashes are not comparable then). Cloud profile with 600 mobs on 289 chunks after `poiSearch`: mob movement and collisions about 15 %, villager brains about 21 %, entity lookups about 9 %. Two ideas were measured and not kept: skipping air before the block shape lookups in collisions, and flattening the brain's behavior maps into arrays; neither moved CPU per tick beyond run-to-run noise. `-Daervium.poiAudit=true` runs the vanilla point of interest search next to the indexed one for every column and logs the number of searches, records and mismatches; `-Daervium.collisionAudit=true` runs the vanilla entity search for every entity collision query the section counts skip and counts the ones where vanilla found an entity. `-Daervium.parity.hoppers=N` places N idle hoppers on dry land (with `-Daervium.hopperAudit=true` one in 64 also gets a full chest minecart on top and one in 64 a dropped item stack); `-Daervium.hopperAudit=true` keeps the vanilla hopper entity searches, counts the ones the section counts would skip and the ones among them where vanilla found an entity. CI runs the three audits with 300 mobs and 500 hoppers and requires 0 mismatches. Measured with dense minecarts next to most hoppers, `hopperSearch` skips few container searches and gives no gain; with plain idle hoppers it cuts server tick CPU by about 11 %.

The same run doubles as a light benchmark: the log prints the wall and CPU time the server light worker spent before the scenario and during it. `-Daervium.parity.lightChanges=400` makes block light heavy; `-PparityWorld=normal` with `-Daervium.parity.forceRadius=12` generates 625 chunks of normal terrain first (hashes are not comparable then, normal worlds have mobs). Cloud results on 4 cores: light was about 5 % of the chunk generation CPU (noise filling 57 %, surface 21 %, features 17 %), and 400 block changes per tick cost about 17 µs each. Bigger chunk and section caches inside the light engine changed neither number beyond noise, so they were not kept.

## Invariants

1. No engine class imports Minecraft or NeoForge; the adapter layer is the only place that does.
2. Nothing allocates per frame in recording or drawing code; copies and summaries happen at most every five ticks.
3. Every job result reaches Minecraft state only through `MainThreadQueue` on the main thread.
4. The benchmark restores every option it changes (pause on lost focus) and only deletes its own `aervium-benchmark` world.
. Every optimization is a module: mixins of a switched-off module are never applied (`MixinModules`), and runtime features read their switch from the config.
7. Caches are content addressed: anything that changes the result (game version, sources, includes, defines, GPU, driver) changes the key or invalidates the data, so a stale cache can never be used.

## Mixins

| Mixin | Target | Why no API |
|---|---|---|
| `FrontendGpuDeviceAccessor` | `FrontendGpuDevice.backend` | The Vulkan memory allocator and its heap budgets are not exposed by renderpearl. |
| `GlslCompilerMixin` | `GlslCompiler.compileToSpv` | No way to supply compiled shaders. |
| `VulkanRenderPipelineMixin` | `vkCreateGraphicsPipelines` in `VulkanRenderPipeline.compile` | Vanilla passes no pipeline cache and offers no hook. |
| `PipelineCacheMixin`, `PipelineCacheAccessor` | `PipelineCache.get`, its fields | Late compiles are invisible otherwise; the warm-up needs the device and shader source of the cache. |
| `RenderSystemMixin` | `RenderSystem.setCurrentPipelineCache` | No event after the shader manager installs its pipelines. |
| `FramerateLimitTrackerMixin` | `FramerateLimitTracker.getFramerateLimit` | The frame limit has no API. |
| `PalettedContainerMixin` | `PalettedContainer` threading check | The check is a private field of every container. |
| `DataLayerStorageMapMixin`, `BlockDataLayerStorageMapMixin`, `SkyDataLayerStorageMapMixin` | `DataLayerStorageMap` writes, `copy()` of the block and sky light maps | The visible light map is always a full clone and the storage has no hook for how it is copied. |
| `VisGraphMixin`, `VisibilitySetMixin`, `CompiledSectionMeshVisibilityAccessor` | `VisGraph.resolve`, `VisibilitySet`, `CompiledSectionMesh.visibilitySet` | The opaque blocks of a section are only known inside the mesh build. |
| `LevelRendererOcclusionMixin` | `LevelRenderer.prepareChunkRenders` and `prepareChunkRendersIndirect` | No event tells which view a terrain pass is for. |
| `LevelExtractorOcclusionMixin` | `LevelExtractor.extractVisibleEntities`, `isEntityVisible`, block entity extraction | Entity visibility is only the frustum check; no event can drop an entity or block entity from a frame. |
| `QuadParticleGroupOcclusionMixin`, `ParticleAccessor` | `QuadParticleGroup.extractRenderState`, `Particle.xo/yo/zo` | Particle extraction only checks the frustum and has no hook. |
| `LevelRendererMeshWorkersMixin` | `new SectionRenderDispatcher` in `LevelRenderer.invalidateCompiledGeometry` | The chunk build executor is hard-wired to the shared background pool. |
| `SectionStoragePoiIndexMixin`, `SectionStorageAccessor`, `PoiManagerColumnSearchMixin` | `SectionStorage` constructor and fields, `PoiManager.getInChunk` | The point of interest storage is a private map with no per-column view, and every POI search goes through `getInChunk`. |
| `EntitySectionCollidableMixin`, `PersistentEntitySectionManagerAccessor`, `ServerLevelEntityCollisionsMixin` | `EntitySection.add` and `remove`, `PersistentEntitySectionManager.sectionStorage`, `getEntityCollisions` overridden in `ServerLevel` | Sections have no per-kind counts and the collision query is an interface default method with no hook. |
| `EntitySectionHopperMixin`, `ServerLevelEntityManagerAccessor`, `EntityCapabilityAccessor`, `HopperBlockEntityItemSearchMixin`, `HopperEntityContainerSearchMixin` | `EntitySection.add` and `remove`, `ServerLevel.entityManager`, NeoForge `EntityCapability.providers`, `HopperBlockEntity.getItemsAtAndAbove`, NeoForge `VanillaInventoryCodeHooks.getEntityContainerOrHandler` | Sections have no per-kind counts, and whether an entity type has an item automation provider is only visible in the capability's provider map. |
| `CrashReportMixin` | `CrashReport.getDetails` | NeoForge crash callables get no access to the exception, and the section needs its stack. |
| `ChunkStatusTasksTerrainMixin` | `ChunkStatusTasks.buildTerrain` | The worldgen check needs each chunk right after its terrain step, before features from its neighbours land in it. Does nothing outside the check. |
| `ThreadedLevelLightEngineTimingMixin` | `ThreadedLevelLightEngine.runUpdate` | The server light worker has no timing of its own. |
| `SectionRenderDispatcherTimingMixin` | `SectionRenderDispatcher.lock` and `uploadTerrainBuffersToGpu` | Upload and builder lock time are invisible otherwise. |
| `ModelBlockRendererLeafCullingMixin` | `ModelBlockRenderer.shouldRenderFace` | Leaf face culling only depends on the block state, never on the leaves around it. |
| `HelionShadowCastersMixin` (pseudo, optional) | Helion `VanillaShadowCasterSource.collectSections` | Helion exposes no hook for its caster lists yet. |
| `LevelRendererSectionSliceMixin`, `RenderSectionMixin`, `CompiledSectionMeshMixin`, `UberGpuBufferMixin` | `LevelRenderer.extractSectionDrawGroups`, `CompiledSectionMesh`, `UberGpuBuffer.freeAllocation` and `close` | The terrain buffer slices of a mesh are only reachable through a map lookup and the draw list loop has no hook. |

## Phases

| Phase | Content | State |
|---|---|---|
| 0 Foundation and measurement | Project, adapter layer, telemetry (frame times, lows, MSPT, GC, RAM, VRAM, timing scopes), overlay, repeatable benchmark, unit tests | Done in 0.1 (unreleased) |
| 1 Core and stutter killers | Done: SPIR-V disk cache, persistent Vulkan pipeline cache, warm-up of built-in pipelines vanilla compiles mid-game, dynamic frame rate (background and pause), lighter chunk section locks, switchable modules, startup report. Moved to Phase 2: zero-allocation frame arenas (FFM) and job priorities, because chunk meshing is their first real user. Still open: more memory reductions after heap analysis, faster resource loading | In progress |
| 2 Streaming and meshing | Started: chunk builds on their own worker pool. Next: zero-allocation frame arenas (FFM) and job priorities and dependencies for the mesh jobs, predictive chunk streaming along the movement direction, parallel meshing on the job system, budgeted GPU uploads, compact vertex formats, faster light engine (ScalableLux-like) | Planned |
| 3 Culling and draw | Done: software occlusion culling for the camera, Helion's shadow cascades, entities, block entities and particles. Done: optional leaf culling (off by default). Next: face culling, batched GUI and text drawing (ImmediatelyFast-like), multi-draw indirect | In progress |
| 4 World and TPS | Server tick optimizations that keep vanilla behavior (Lithium-like, clean room), parallel chunk generation (C2ME-like), network compression and batching (Krypton-like) | Planned |
| 5 Advanced Vulkan | GPU-driven culling, meshlets, async compute and transfer queues, two render paths: baseline for integrated GPUs and advanced for strong GPUs | Planned |
| 6 Frame lock and LOD | Dynamic resolution to hold a target frame time, distant terrain LOD (Distant Horizons-like), extended settings (Sodium Extra-like) | Planned |

## Porting Checklist for a New Minecraft Version

1. Update `gradle.properties` and build; fix compile errors only in `integration/`, `client/`, `mixin/` and `config/`.
2. Check the mixin target of `FrontendGpuDeviceAccessor` and the VMA calls in `VulkanMemoryProbe`.
3. Check the gamerule names and command syntax used by `BenchmarkCommands` and the world creation in `BenchmarkWorld`.
4. Run the unit tests, then a benchmark, and compare it with the last report of the previous version.

## Measuring

Before and after an optimization:

1. Start the game with `-Define aervium.benchmark=true,aervium.benchmark.quit=true` (or run the two properties separately) with the same options and mods.
2. Run it twice without the change to check repeatability (the report shows the change against the previous run), then once with the change.
3. Keep the change only when the lows and MSPT improve or stay equal and no scene loses more than the run-to-run noise.
