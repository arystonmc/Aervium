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
`JobSystem` is a work-stealing pool with two cores left to the game threads. Jobs never touch Minecraft state; they get copies and hand results back through `MainThreadQueue`, which runs them within a time budget. Phase 1 adds job priorities and dependencies, Phase 2 moves chunk meshing onto it.

### Shader and Pipeline Caches
Vanilla compiles every GLSL shader to SPIR-V with shaderc at each start and resource reload, and creates its Vulkan pipelines without a `VkPipelineCache`.

- `SpirvCache` keys each compilation by its full input (`ShaderCacheKey`) and keeps the SPIR-V in `<game>/aervium/cache/spirv/`. Hits skip shaderc; misses compile as vanilla and write the result on a worker.
- `VulkanPipelineCache` hands one pipeline cache to every pipeline creation, loads it from `<game>/aervium/cache/vulkan-pipelines.bin` when its header matches the GPU and driver, and saves it on shutdown.
- `PipelineWarmup` compiles the built-in pipelines vanilla would only compile on the render thread at first use and inserts them through the budgeted main-thread queue.
- The startup report in the log (`Aervium startup: ...`) shows the time to the first screen, shader hits and misses and pipeline creation time; `PipelineCacheMixin` logs every pipeline compiled late on the render thread.

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
| `LevelRendererSectionSliceMixin`, `CompiledSectionMeshMixin`, `UberGpuBufferMixin` | `LevelRenderer.extractSectionDrawGroups`, `CompiledSectionMesh`, `UberGpuBuffer.freeAllocation` and `close` | The terrain buffer slices of a mesh are only reachable through a map lookup and the draw list loop has no hook. |

## Phases

| Phase | Content | State |
|---|---|---|
| 0 Foundation and measurement | Project, adapter layer, telemetry (frame times, lows, MSPT, GC, RAM, VRAM, timing scopes), overlay, repeatable benchmark, unit tests | Done in 0.1 (unreleased) |
| 1 Core and stutter killers | Done: SPIR-V disk cache, persistent Vulkan pipeline cache, warm-up of built-in pipelines vanilla compiles mid-game, dynamic frame rate (background and pause), lighter chunk section locks, switchable modules, startup report. Moved to Phase 2: zero-allocation frame arenas (FFM) and job priorities, because chunk meshing is their first real user. Still open: more memory reductions after heap analysis, faster resource loading | In progress |
| 2 Streaming and meshing | Zero-allocation frame arenas (FFM) and job priorities and dependencies for the mesh jobs, Predictive chunk streaming along the movement direction, parallel meshing on the job system, budgeted GPU uploads, compact vertex formats, faster light engine (ScalableLux-like) | Planned |
| 3 Culling and draw | Software occlusion culling (caves), entity and block entity culling, leaf and face culling, batched GUI and text drawing (ImmediatelyFast-like), multi-draw indirect | Planned |
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
