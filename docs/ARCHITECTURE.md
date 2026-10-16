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
- The camera pass runs on its own worker, one frame behind (`OcclusionPipeline`), so the render thread only copies the section list and never waits. Running it on the render thread cost half the frame rate at render distance 32 (98.8 to 47.1 FPS on Can's PC, 2026-10-12). A late result is only used while the eye stays within its reach (a margin from the last frame's movement that every test box grows by); when the eye moved past it, for example after a slow frame, the sections the result hid are tested again on the render thread with a margin grown to the eye (up to 8 blocks, `Frame.widen`) instead of drawing every section, which used to turn one slow frame into several (12 % of the elytra frames drew everything; the 0.1% low rose from 34.8 to 38.9 FPS on Can's PC, 2026-10-14); the raster covers 25 % more than the screen and a box crossing its edge counts as visible, because visibility from one eye position does not change when the camera turns.
- Shadow pass (with Helion): the same test per cascade in light space with an orthographic raster, on its own worker (`Aervium Shadow Culling`, one `OcclusionPipeline` per cascade). A finished result is matched to the current caster list by section position (`ShadowCascadeCulling`): while the light direction is unchanged, a caster it hid is dropped unless the caster changed or an occluder that left the list or changed covers one of its raster tiles (`TileMask`), so the result stays exact while the camera moves and the cascade gains and loses sections at its edges. On the benchmark route it still hides no caster: Helion's caster lists hold only sections with geometry near the light frustum (31 to 351 per cascade, almost all of them surface), and the worker draws 200 to 1800 occluder boxes without any caster lying behind them (diagnostic run 2026-10-14). A caster the light cannot see is lossless to drop, because the shadow map stores only the nearest depth and the casters in front of it are in the same cascade. Running this pass on the render thread cost a quarter of the frame rate at render distance 32 (100.7 to 76.3 FPS average, 1% low 58.7 to 28.1 on Can's PC, 2026-10-14).
- Both tests are conservative (see `DepthRaster`), so nothing visible is ever dropped. Sections still fading in never occlude.
- `-Daervium.cullingAudit=true` checks this in game: rays through the real blocks to hidden sections must never get through (`CullingAudit`). The benchmark log shows the share of sections the camera pass hid and of shadow casters the shadow pass hid per scene.

### Face Culling
The GPU throws away a triangle that faces away from the camera only after its vertices went through the vertex shader. Aervium groups the quads of every solid and cutout section mesh by the side they face while the mesh is built (`FacingSort`) and, in the camera pass, leaves out the groups whose planes all lie in front of the eye, so a section seen from one side sends only up to three of its six face directions to the GPU. Only faces the GPU would cull are left out, with half a block of margin for view bobbing; shadow passes and any other view keep every face.

### Terrain Draw Paths
Aervium builds the terrain draw list on one of two paths. Both keep vanilla's visible list, occlusion culling and face culling rules, so they draw the same faces.

- Baseline path (every GPU): `SectionDrawLists` walks the visible sections on the render thread and writes the indirect commands and section infos as plain numbers (`directDraws`): no record per draw or per section, pooled lists, one copy into mapped memory. GPUs without `drawIndirectCount` and the path without multi-draw use it for everything, and every GPU uses it for translucent layers (vanilla orders them per section) and Helion's shadow cascades. The path follows the device feature, not the GPU class: an integrated GPU with `drawIndirectCount` takes the advanced path (not measured yet, because the test PC has a discrete GPU).
- Advanced path (`gpuCulling`, GPUs with `drawIndirectCount`): in the camera pass the render thread only checks each visible section's occlusion stamp and GPU entry and appends its index and fade to a mapped list. A GPU section table holds each section's solid and cutout draws and is updated only for sections whose mesh changed, through ordered copies in the frame's command buffer. A compute shader (`terrain_draws.csh`, one thread per visible section) applies face culling against the camera, writes the section instance data and appends each draw to its draw group with an atomic counter; the terrain layers draw the groups with `vkCmdDrawIndexedIndirectCount`. Draw order inside a group follows the atomic counter instead of the visible list, which only matters for coplanar faces of different sections.
- The compute pass needs a storage buffer usage and a device feature renderpearl does not expose, so `VulkanConstStorageMixin` and `VulkanFeatureSetsMixin` add them; the dispatch sits between renderpearl's own full memory barriers.

Not built, with the reason (2026-10-16): meshlets need mesh shaders, which renderpearl pipelines cannot use, and a section's face groups already act as clusters the compute pass culls one by one; async compute and transfer queues exist in renderpearl (`computeQueue`, `transferQueue`) but nothing uses them, and the work they could take is small: the whole culling stage (table uploads, count reset, dispatch and its barriers) measured 19 to 41 µs per frame on the GPU on average and at most 0.36 ms (GPU timestamps, Can's PC, with and without Helion, 2026-10-16), and chunk uploads cost 0.03 to 0.19 ms per frame on average (benchmark log), so moving them to another queue would add semaphores and queue ownership transfers for no measurable frame time.

### Compact Terrain Vertices
Vanilla stores every terrain vertex in 28 bytes (three floats for the position, color, two floats for the texture, two shorts for light). The module `compactVertices` (on by default since the measurements of 2026-10-15 below; off by itself when the installed Helion cannot read the format) stores solid and cutout terrain in 16 bytes, 43 % less vertex memory and upload:

| Attribute | Format | Content |
|---|---|---|
| `PositionLight` | RGBA16_UINT | x, y, z in 1/2048 block steps over -8 to +24 blocks of the section; block light in the low and sky light in the high byte of w |
| `Color` | RGBA8_UNORM | unchanged |
| `PackedUV0` | RG16_UINT | atlas coordinates times 65536 |

1. The pipelines decide the format: `ChunkSectionLayer` takes its vertex size from the vanilla solid and cutout pipelines, so the module gives exactly those four pipelines (direct and multi-draw) the compact format, the define `AERVIUM_COMPACT_VERTICES=1` and the vertex shader `aervium:terrain/compact` while `RenderPipelines` is set up. The chunk buffers and draw offsets follow on their own.
2. Chunks are still built in the vanilla format, so face sorting and NeoForge geometry renderers see vanilla data; right before the upload `CompactVertexEncoder` rewrites the mesh in place.
3. Translucent terrain keeps the vanilla format, because vanilla sorts translucent quads from their float positions.
4. Helion reads the define, not Aervium code: its geometry pipelines inherit the format with `toBuilder()`, its shadow pipelines copy it from the vanilla pipeline, and its terrain shaders decode under `#ifdef AERVIUM_COMPACT_VERTICES`. Helion turns its geometry buffer and shadows off for a format version it does not know.
5. Exactness: model coordinates on the 1/16 grid and texture coordinates of a power-of-two atlas are exact; other positions move by at most 1/4096 block, other texture coordinates round toward the inside of their quad, so no face samples the neighbouring sprite. Vanilla models stay within one block of their block, far inside the range; a vertex outside it is moved to the edge and logged once.

Measured on Can's PC with Helion and Arkea, 1920x1009 window, render distance 32 (2026-10-15, overall average / 1% low / 0.1% low): four runs with the module off 106.1 / 48.1 / 29.7, 104.3 / 46.1 / 27.1, 104.6 / 44.4 / 27.8 and 104.8 / 45.5 / 26.4; two runs on 110.6 / 47.8 / 31.2 and 109.8 / 46.6 / 27.9; peak video memory underground 2375 MB off, 1607 MB on. Per scene the lows of the on runs stay inside the spread of the off runs (elytra 1% low 47.6 to 50.9 on, 48.3 to 55.2 off; underground 35.5 to 37.1 on, 30.9 to 33.2 off). With the two pairs of 2026-10-14 (fullscreen) the overall lows went up in three of four pairs, so the module is on by default.

Helion 0.1 cannot read the format. `MixinModules` therefore keeps the module off when Helion is installed without `com/aryston/helion/integration/vanilla/CompactTerrainVertices` in its jar, and logs why.

### Parallel Feature Generation
Vanilla generates noise, surface and biomes of new chunks on the shared background pool, but runs every other step, including features (about a fifth of the generation CPU), one task after another on its `worldgen` executor; with 16 threads that single thread decides how fast new chunks appear. The module `parallelFeatures` moves the feature step to its own job system. A feature step may write only into the 3 x 3 chunks around its own (`blockStateWriteRadius(1)`), so each step holds those nine chunks in `RegionLocks` while it runs, and steps whose areas overlap wait for each other. The other steps keep vanilla's order: structure starts and references and biomes only write the chunk they run for, and that chunk is never inside the area of a running feature step, because a feature step needs its neighbours at the terrain status, which comes after them. Measured with the worldgen check on Can's PC (dedicated server, 2026-10-15): 625 chunks 6115 and 6414 ms off, 4752 and 4924 ms on; 2401 chunks 15258 ms off, 11045 and 11190 ms on; terrain hash identical, no errors. In singleplayer it cost frames: the integrated server shares the processor with the render thread and the chunk builders, and two windowed benchmark pairs (1920x1009, 2026-10-15) gave elytra 108.5 and 101.5 FPS (1% low 41.2 and 38.0) on against 124.1 and 124.2 (52.2 and 55.2) off, overall 1% low 42.6 and 43.1 against 48.1 and 46.1. The mixin is therefore listed under `server` in the mixin configuration and only applies on dedicated servers.

### Packet Compression
The module `networkCompression` gives every connection's `CompressionEncoder` a zlib level 4 deflater instead of level 6. On 1024 real chunks (45 MB of chunk data from the benchmark world) level 6 took 834 ms and gave 14.0 %, level 4 399 ms and 14.6 %, level 1 251 ms and 16.8 % (Can's PC, 2026-10-15). Deflating straight from the packet buffer instead of vanilla's copy into a byte array gave 1 to 2 %, within noise, so it was not kept. Vanilla already holds back flushes during the server tick (`suspendFlushing`) and flushes once per tick, so flush batching has nothing left to gain. Singleplayer connections are in memory and never compress.

### Modules
Optimizations that change game classes are mixins grouped into modules (`OptimizationModule`). `MixinModules` reads `config/aervium-modules.properties` before any mixin applies and writes every switch back; renderer modules also stay off with Sodium or Iris. Features that run later (dynamic frame rate) use the normal client config.

## Benchmark

`-Daervium.benchmark=true` (works with `launcher/launch.ps1 -Define`) starts the benchmark from the first screen; `-Daervium.benchmark.quit=true` closes the game afterwards.

1. Deletes the previous `aervium-benchmark` world and creates a fresh one: seed 8675309, spectator, peaceful, cheats on.
2. Freezes time at noon and the weather, turns mob spawning off, finds the spawn, the nearest village and the redstone platform height on the server thread.
3. Force-loads the village crowd area, summons the 40 villagers as soon as its entities are loaded and releases the area, so no scene starts with a summon (the redstone clocks stay built at their scene, because they lie within the simulation distance of the first routes and would tick during them).
4. Warms up for 400 ticks at the start of the first route.
5. Runs every scene: 200 ticks of settling (camera already moving, world set up), then 600 measured ticks. The log names the longest frame while settling: the jump to a new scene costs 100 to 500 ms on Can's PC (JFR 2026-10-14: vanilla moving the view area and releasing old meshes, Helion rescanning its shadow caster sections, chunk uploads), which is why settling is not measured.

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
5. Every optimization is a module: mixins of a switched-off module are never applied (`MixinModules`), and runtime features read their switch from the config. A module that changes what the player sees or is not yet measured on Can's PC lands on a branch until it is measured; every module on `main` starts on.
6. Caches are content addressed: anything that changes the result (game version, sources, includes, defines, GPU, driver) changes the key or invalidates the data, so a stale cache can never be used.

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
| `RenderPipelineBuilderCompactMixin` | `RenderPipeline.Builder.build` for the vanilla solid and cutout terrain pipelines | The chunk buffers take their vertex size from these static pipelines; NeoForge pipeline modifiers only swap pipelines at draw time. |
| `SectionCompilerCompactMixin`, `CompileTaskCompactMixin`, `MeshDataCompactMixin` | `SectionCompiler.getOrBeginLayer`, the upload call in `CompileTask.doTask`, `MeshData` | No hook between the mesh build and the upload, and the builder format of a layer is fixed. |
| `ChunkSectionsToRenderCompactMixin` | the wireframe override in `ChunkSectionsToRender.renderGroup` | The debug override pipelines are chosen inside the method. |
| `CompressionEncoderLevelMixin` | `new Deflater()` in the `CompressionEncoder` constructor | The compression level of a connection has no setting or event. |
| `ChunkStatusTasksFeaturesMixin` | `ChunkStatusTasks.generateFeatures` | The feature step is a static task of the generation pyramid and always runs on vanilla's single `worldgen` executor. |
| `LevelRendererDirectDrawMixin` | `writeChunkSectionCommands` and `writeChunkSectionsInstanced` calls in `LevelRenderer.prepareChunkRendersIndirect` | Both writes only accept lists of records, one per draw and per section. |
| `SectionDirtyStateMirrorMixin`, `SectionUpdateTrackerMirrorMixin`, `LevelExtractorDirtySectionsMixin` | `SectionUpdateTracker.SectionDirtyState` changes, the tracker constructor, the `getDirtyState` call in `LevelExtractor.extract` | The dirty states live in scattered objects behind a node wrapper and the scan over every visible section has no hook. |
| `ChunkSectionsToRenderIndirectCountMixin` | `RenderPass.drawIndexedIndirect` calls in `ChunkSectionsToRender.DrawIndirect.render` | Renderpearl has no counted indirect draw. |
| `VulkanFeatureSetsMixin`, `VulkanConstStorageMixin` | `VulkanFeatureSets.optionalFeatureSets`, `VulkanConst.bufferUsageToVk` | Renderpearl enables only the device features vanilla uses and has no storage buffer usage. |
| `VulkanDeviceAccessor`, `VulkanCommandEncoderAccessor`, `VulkanRenderPassAccessor`, `FrontendRenderPassAccessor` | enabled features, the encoder's command buffer, a render pass's command buffer and descriptor push, the frontend pass's backend | The compute pass and the counted draw record raw Vulkan commands into renderpearl's command buffers. |
| `RenderSectionBlockEntityMixin`, `LevelExtractorBlockEntitiesMixin` | `setSectionMesh` and `reset` of `RenderSection`, the `getSectionMesh` call in `LevelExtractor.extractVisibleBlockEntities` | Block entity extraction walks every visible section and has no hook. |

## Phases

| Phase | Content | State |
|---|---|---|
| 0 Foundation and measurement | Project, adapter layer, telemetry (frame times, lows, MSPT, GC, RAM, VRAM, timing scopes), overlay, repeatable benchmark, unit tests | Done in 0.1 (unreleased) |
| 1 Core and stutter killers | Done: SPIR-V disk cache, persistent Vulkan pipeline cache, warm-up of built-in pipelines vanilla compiles mid-game, dynamic frame rate (background and pause), lighter chunk section locks, switchable modules, startup report. Moved to Phase 2: zero-allocation frame arenas (FFM) and job priorities, because chunk meshing is their first real user. Heap analysis on Can's PC (2026-10-15, render distance 32, Helion and Arkea, during the benchmark village scene after a full collection): 1469 MB live, 756 MB of it chunk data (block and biome containers, light arrays) that the client and the integrated server each keep; everything FerriteCore deduplicates on older versions (block state tables, neighbour tables, property maps, baked quads, model parts) adds up to 45 MB (3 %), because 26.3 already stores state neighbours in compact tables, so no memory module pays off now. Startup on the same PC: 25.6 s to the title screen; the main thread waits most of that time (42,000 park events in a JFR recording from the first second) while NeoForge scans and transforms classes, and the busiest CPU work is the Unicode font (about 1 s) and block shape caches, spread over threads; shader and pipeline caches already save the shader compiles. A ModernFix-like loader change is not in reach of a mod that loads after these steps | Done |
| 2 Streaming and meshing | Started: chunk builds on their own worker pool; compact terrain vertices (`compactVertices`, on by default: 5 % higher average, 1% low +3 %, 0.1% low +6 % and 20 to 32 % less video memory on Can's PC, 2026-10-15). Profile 2026-10-14: the mesh workers are 4 to 24 % busy and the render thread waits for them at most 17 ms per scene, so meshing does not limit frames yet; biome tint blending is 19 % of build time and the first target once it does. Measured again on 2026-10-15 (benchmark log, compact vertices on): chunk uploads take 0.04 to 0.19 ms per frame on average and 4.0 ms at most, the render thread waits for chunk builders at most 10.6 ms per 30 s scene and the 8 mesh workers are at most 21 % busy even in the elytra flight (883 builds per second); vanilla already builds the nearest section first and uploads through a staging buffer, and the light worker spends 15.6 to 62.5 ms CPU per 1200 parity ticks. Predictive streaming, budgeted uploads, job priorities, frame arenas and a faster light engine therefore have nothing to gain on this PC now; they come back when a profile shows the meshing, upload or light path in the frame time | Done |
| 3 Culling and draw | Done: software occlusion culling for the camera, Helion's shadow cascades, entities, block entities and particles. Done: optional leaf culling (off by default), face culling, culling results that survive slow frames and shadow results matched by section. Batched GUI and text drawing and multi-draw indirect need no module: in 26.3 vanilla sorts every GUI element by layer, pipeline and texture, merges them into one vertex buffer with one render pass and caches item icons in an atlas (`GuiRenderer`, `GuiItemAtlas`), and draws terrain with multi-draw indirect whenever the GPU supports it (`Minecraft.multiDrawIndirect`, on by default); Aervium's culling hooks both terrain paths | Done |
| 4 World and TPS | Done: point of interest search (`poiSearch`), entity collision search (`entityCollisions`) and idle hopper search (`hopperSearch`) skips, checked by the parity, worldgen and audit runs; parallel feature generation on dedicated servers (`parallelFeatures`, 23 to 27 % faster world generation; it cost frames in singleplayer); faster packet compression (`networkCompression`); flush batching is vanilla (`suspendFlushing`). Server profile on Can's PC (2026-10-15, dedicated server, 600 mobs on 289 force-loaded chunks of a normal world, JFR): mean tick 7.9 ms, longest 33.8 ms; block collisions 13 % of the server thread, mob brains 6 %, entity section lookups 5 %, chunk lookups for block reads 8 %, no single method above 8 %; with every scene of the benchmark at most 11.2 ms average MSPT, the tick stays far inside 50 ms and needs no further module now | Done |
| 5 Advanced Vulkan | GPU-driven culling, meshlets, async compute and transfer queues, two render paths: baseline for integrated GPUs and advanced for strong GPUs Started 2026-10-15 with a profile without Helion (render distance 32, 1920x1009 window): building the chunk draw list took 20.5 % of the render thread, mostly cache misses on the scattered objects of every visible section; draw records now keep their numbers in one array and are checked by a mesh version on the section, which brought it to 10.8 % (face culling results unchanged); vanilla's block entity scan read the mesh of every visible section to find the few with block entities, and `blockEntitySections` brought the whole level extraction from 16.3 % to 10.3 % (block entity scan 7 % to 2.1 %). Clean benchmark on 2026-10-15 after a restart (1920x1009 window, render distance 32, average / 1% low / 0.1% low): without Helion Phase 4 196.5 / 67.1 / 35.8 and Phase 5 210.2 / 72.0 / 44.2; with Helion two pairs 110.5 / 35.9 / 21.0 against 114.0 / 43.0 / 28.0 and 139.1 / 64.1 / 42.4 against 141.0 / 65.5 / 40.2 (the first pair ran while the PC was still busy after the restart; with Helion the frame is GPU bound and the gain is within noise); Sodium 0.9.3 on the same route 273.8 / 112.4 / 66.0, so without Helion Aervium is still 23 % behind on average and 36 % on the 1% low. 2026-10-16, same PC and window, profiles with a JFR frame event: the draw list built one record per draw and per section and grew its lists every frame (12 % of the render thread); `directDraws` writes the commands and section infos as plain numbers (two pairs against `b04e807`: 290.0 / 108.8 / 59.0 and 293.8 / 107.5 / 56.1 against 274.6 / 99.1 / 56.1 and 268.4 / 92.6 / 49.0). In the cave scene the dirty section lookup was 17 % and the render thread retesting hidden sections for a late camera result 11 % (16 % of those frames widen); `dirtySections` and reveal steps measured on the worker (`measureReveal`) raised the cave scene from 375 / 98 / 60 and 367 / 97 / 56 to 392 / 107 / 68 and 418 / 110 / 65 (culling audit: 1.13 million rays, 0 reached a hidden target). Moving the record header into its array raised the redstone 1% low by 3 to 4 %. Sodium 0.9.3 directly before Aervium on the same route: 295.4 / 125.6 / 70.4 against 319.5 / 129.0 / 68.0. Tried and not kept: leaving faces turned away from the sun out of Helion's shadow casters showed light streaks in the morning shadows (lesson L-102). GPU-driven culling, the advanced path of the two terrain draw paths (see Terrain Draw Paths): without Helion four runs with `gpuCulling` 327.7 / 130.2 / 71.6, 323.8 / 134.0 / 70.6, 332.3 / 136.7 / 75.6 and 326.1 / 131.8 / 73.1 against three without 323.9 / 128.4 / 65.2, 322.4 / 126.6 / 70.7 and 325.3 / 129.2 / 71.1 (cave scene 1% low 115 to 128 against 110 to 116; one run of each side was disturbed and is left out); with Helion 144.8 / 66.2 / 42.4 and 142.7 / 64.3 / 41.7 against 143.4 / 64.3 / 41.2 and 144.2 / 64.6 / 40.9, so it is on by default; the Helion visual test shows the same terrain as the CPU path within run-to-run noise. Meshlets and async queues were not built (reasons under Terrain Draw Paths) | Done |
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
