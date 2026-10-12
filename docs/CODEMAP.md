# Aervium Code Map

Every class and source file of Aervium with its purpose. Find the right file here before opening any code. Keep this file in sync with the code in the same commit (rule D1). The big picture, the performance rules and the phase plan are in `docs/ARCHITECTURE.md`.

## Quick Lookup

| I want to change | Go to |
|---|---|
| Mod id, logger, startup | `Aervium` (both sides), `AerviumClient` (client) |
| Percentiles, 1% and 0.1% lows, standard deviation | `SampleStatistics`, `FrameTimeSummary` |
| Server tick time (MSPT) statistics and histogram | `TickSummary`, `TickTimeRecorder` |
| Ring buffers without per-frame allocation | `SampleRing`, `FrameTimeRecorder` |
| Garbage collector pauses | `GcPauseRecorder`, `GcPauseClassifier` |
| Collector in use and JVM flag advice | `GarbageCollector`, `GcAdvice` |
| Frame budget and timed systems | `FrameBudget`, `TimedSystem`, `SystemTimings` |
| Worker threads and results back on the main thread | `JobSystem`, `MainThreadQueue` |
| Overlay drawing and text | `TelemetryOverlay`, `OverlayLines`, `ClientTelemetry` |
| VRAM usage | `VulkanMemoryProbe`, `FrontendGpuDeviceAccessor` |
| Benchmark route and scenes | `BenchmarkRoute`, `BenchmarkScene`, `LinePath`, `OrbitPath` |
| Benchmark world, commands and camera | `BenchmarkWorld`, `BenchmarkCommands`, `BenchmarkCamera` |
| Benchmark flow and report | `BenchmarkDriver`, `SceneMeasurement`, `BenchmarkReport`, `BenchmarkText`, `BenchmarkFiles`, `BenchmarkComparison` |
| Shader and pipeline caches, startup report | `ShaderCaches`, `SpirvCache`, `VulkanPipelineCache`, `ShaderCacheKey`, `DiskCache` |
| Pipelines compiled in the middle of a frame | `PipelineWarmup`, `PipelineCacheMixin` |
| Dynamic frame rate | `FrameRatePolicy`, `FrameRateGovernor`, `FramerateLimitTrackerMixin` |
| Chunk memory | `PalettedContainerMixin` |
| Light engine snapshots | `ShardedLongMap`, `ShardedLongMapBuilder`, `DataLayerStorageMapMixin`, `BlockDataLayerStorageMapMixin`, `SkyDataLayerStorageMapMixin` |
| Chunk draw list (terrain buffer lookups per frame) | `LevelRendererSectionSliceMixin`, `CompiledSectionMeshMixin`, `UberGpuBufferMixin`, `SectionSliceHolder` |
| Switching modules on and off | `OptimizationModule`, `ModuleSettings`, `MixinModules` |
| Sodium and Iris detection | `CompatibilityGuard` |
| Settings | `AerviumConfig`, `lang/*.json` |
| Key bindings | `AerviumKeys` |
| Event wiring | `CommonEvents` (both sides), `ClientEvents` (client) |
| Mod name, version, loader versions, license, authors | `gradle.properties` |
| Mod list metadata and dependencies | `neoforge.mods.toml` |
| Mod logo | `branding/logo.png` (temporary placeholder) |
| Build setup, run configurations | `build.gradle` |
| Unit tests | `src/test/java/`, see Tests below |

## Overview

- Side: client and server. The common part records server tick times; the client part holds the overlay, the benchmark and every other measurement. Each side works without the other.
- Root package: `com.aryston.aervium`.
- Game-independent code lives in `core`, `world` and `debug` and is covered by unit tests. Code that touches Minecraft or NeoForge lives in `integration` (both sides), `client` (client side) and `mixin`.

## Classes

### `com.aryston.aervium`

#### Aervium
- Path: `src/main/java/com/aryston/aervium/Aervium.java`
- Role: Entry point annotated with `@Mod` for both sides; registers the common events.
- Members:
  - `MOD_ID`: the mod id.
  - `LOGGER`: shared logger for the whole mod.
- Depends on: `CommonEvents`.

### `com.aryston.aervium.core.telemetry`

#### SampleRing
- Path: `src/main/java/com/aryston/aervium/core/telemetry/SampleRing.java`
- Role: Fixed-capacity ring buffer of `long` samples (nanoseconds) for a single writer; recording never allocates.
- Members:
  - `record(long)`, `clear()`, `size()`, `recorded()`: write and count; `recorded` counts every sample ever written and serves as a mark.
  - `copyLatest(long[], int)`: copies the newest samples oldest first into an existing array without allocating.
  - `latest(int)`, `since(long mark)`: copies into a new array (for aggregation, not per frame).
- Depends on: nothing.

#### SampleStatistics
- Path: `src/main/java/com/aryston/aervium/core/telemetry/SampleStatistics.java`
- Role: Record with count, mean, population standard deviation, min, max, median, p95, p99 and p99.9 of a sample.
- Members:
  - `ofSorting(long[], int)`: sorts the counted prefix in place and computes the statistics.
  - `percentile(long[], int, double)`: nearest-rank percentile of a sorted array.
- Depends on: nothing.

#### FrameTimeSummary
- Path: `src/main/java/com/aryston/aervium/core/telemetry/FrameTimeSummary.java`
- Role: Frame time statistics for players: average FPS, 1% low (FPS at the 99th percentile frame time), 0.1% low (99.9th percentile), average, standard deviation, median, p99, p99.9 and maximum in milliseconds.
- Depends on: `SampleStatistics`, `Durations`.

#### TickSummary
- Path: `src/main/java/com/aryston/aervium/core/telemetry/TickSummary.java`
- Role: Server tick time statistics in milliseconds with the number and share of ticks over the 50 ms budget and a histogram (5, 10, 20, 30, 40, 50 ms and over).
- Depends on: `SampleStatistics`, `Durations`.

#### FrameTimeRecorder
- Path: `src/main/java/com/aryston/aervium/core/telemetry/FrameTimeRecorder.java`
- Role: Records the time between consecutive frame starts into a `SampleRing`; `reset()` forgets the gap to the next frame (world joins).
- Depends on: `SampleRing`.

#### GcPauseRecorder
- Path: `src/main/java/com/aryston/aervium/core/telemetry/GcPauseRecorder.java`
- Role: Listens to the garbage collector notifications of the JVM and records stop-the-world pause durations.
- Members:
  - `install()`: registers the listener on every collector bean.
  - `mark()`, `since(long)`: pauses after a mark; `total()`: every pause since start.
- Depends on: `GcPauseClassifier`, `SampleRing`, `GcPauseTotals`, `Durations`.
- Notes: Notifications arrive on a JMX thread, so every method is synchronized.

#### GcPauseClassifier
- Path: `src/main/java/com/aryston/aervium/core/telemetry/GcPauseClassifier.java`
- Role: Tells pauses from concurrent collector cycles by collector name and action (ZGC and Shenandoah report cycles separately).
- Depends on: nothing.

#### GarbageCollector
- Path: `src/main/java/com/aryston/aervium/core/telemetry/GarbageCollector.java`
- Role: Enum of the collectors (ZGC, Shenandoah, G1, Parallel, Serial), recognized from the collector bean names; `lowLatency()` marks the concurrent ones.
- Members: `current()`: the collector of this JVM; `fromBeanNames(Collection)`.
- Depends on: nothing.

#### GcAdvice
- Path: `src/main/java/com/aryston/aervium/core/telemetry/GcAdvice.java`
- Role: Advice on launcher JVM flags (low-pause collector, heap limit) for the startup log; a mod cannot change the collector of the running JVM.
- Members: `forThisJvm()`; `advise(GarbageCollector, long, long)`: empty when nothing should change.
- Depends on: `GarbageCollector`.

#### GcPauseTotals
- Path: `src/main/java/com/aryston/aervium/core/telemetry/GcPauseTotals.java`
- Role: Record of pause count, total and longest pause in milliseconds.
- Depends on: nothing.

#### HeapUsage
- Path: `src/main/java/com/aryston/aervium/core/telemetry/HeapUsage.java`
- Role: Record of used and maximum Java heap; `current()` reads the runtime.
- Depends on: nothing.

#### VramUsage
- Path: `src/main/java/com/aryston/aervium/core/telemetry/VramUsage.java`
- Role: Record of used video memory and its budget; `UNAVAILABLE` when no Vulkan allocator is reachable.
- Depends on: nothing.

#### TelemetrySnapshot
- Path: `src/main/java/com/aryston/aervium/core/telemetry/TelemetrySnapshot.java`
- Role: Everything the overlay shows at one moment: frame summary, optional server tick summary, GC pauses, heap, VRAM and the timing of every `TimedSystem` (`SystemTiming`).
- Depends on: `FrameTimeSummary`, `TickSummary`, `GcPauseTotals`, `HeapUsage`, `VramUsage`, `TimedSystem`.

#### Durations
- Path: `src/main/java/com/aryston/aervium/core/telemetry/Durations.java`
- Role: Nanosecond conversions and the 50 ms tick budget.
- Depends on: nothing.

#### ByteSizes
- Path: `src/main/java/com/aryston/aervium/core/telemetry/ByteSizes.java`
- Role: Bytes to megabytes.
- Depends on: nothing.

### `com.aryston.aervium.core.budget`

#### TimedSystem
- Path: `src/main/java/com/aryston/aervium/core/budget/TimedSystem.java`
- Role: Every system with its own timing scope (frame, client tick, server tick, overlay, telemetry), its label and its share of the frame or tick budget.
- Depends on: nothing.

#### FrameBudget
- Path: `src/main/java/com/aryston/aervium/core/budget/FrameBudget.java`
- Role: Record of the target frame rate; gives the frame time and the budget of each `TimedSystem` (tick systems always get 50 ms).
- Depends on: `TimedSystem`, `Durations`.

#### SystemTimings
- Path: `src/main/java/com/aryston/aervium/core/budget/SystemTimings.java`
- Role: Allocation-free timing scopes: `begin()` and `end(system, start)` keep a smoothed time and a peak per system; `sample(budget)` reads and resets the peaks and flags systems over budget.
- Members:
  - `SHARED`: the instance every scope writes to.
- Depends on: `TimedSystem`, `FrameBudget`, `TelemetrySnapshot.SystemTiming`.
- Notes: Each system is written by one thread; readers accept slightly stale values.

#### FrameRatePolicy
- Path: `src/main/java/com/aryston/aervium/core/budget/FrameRatePolicy.java`
- Role: Dynamic frame rate: keeps the vanilla limit while the game is active and caps it while the window is in the background (`UNFOCUSED`) or the single player game is paused (`PAUSED`); never raises a lower vanilla limit.
- Depends on: nothing.

### `com.aryston.aervium.core.cache`

#### ContentKey
- Path: `src/main/java/com/aryston/aervium/core/cache/ContentKey.java`
- Role: SHA-256 key builder over separated text parts.
- Depends on: nothing.

#### DiskCache
- Path: `src/main/java/com/aryston/aervium/core/cache/DiskCache.java`
- Role: One file per key in a folder; writes go through a uniquely named temporary file and an atomic move, so a crash never leaves half an entry. Keys address their content, so an existing entry is never rewritten, and a move blocked by another reader or writer (Windows reports it as access denied) is retried and treated as done once the entry exists.
- Depends on: nothing.

#### ShaderIncludes
- Path: `src/main/java/com/aryston/aervium/core/cache/ShaderIncludes.java`
- Role: Finds `#include` and `#moj_import` targets of a shader source, directly and transitively (cycles stop once every include is seen).
- Depends on: nothing.

#### ShaderCacheKey
- Path: `src/main/java/com/aryston/aervium/core/cache/ShaderCacheKey.java`
- Role: Cache key of one shader compilation: format version, game version, shader name and stage, source, every transitive include with its content, sorted defines and flags, and the device switches that change the compiled code.
- Depends on: `ContentKey`, `ShaderIncludes`.

#### SpirvBinary
- Path: `src/main/java/com/aryston/aervium/core/cache/SpirvBinary.java`
- Role: Rejects cache entries that are not whole SPIR-V words or lack the SPIR-V magic number.
- Depends on: nothing.

#### PipelineCacheHeader
- Path: `src/main/java/com/aryston/aervium/core/cache/PipelineCacheHeader.java`
- Role: Checks the Vulkan pipeline cache header (version one, vendor, device, pipeline cache UUID) before saved data is handed to the driver.
- Depends on: nothing.

#### CacheStatistics
- Path: `src/main/java/com/aryston/aervium/core/cache/CacheStatistics.java`
- Role: Thread-safe hit, miss and work time counters for the startup report.
- Depends on: nothing.

### `com.aryston.aervium.core.module`

#### OptimizationModule
- Path: `src/main/java/com/aryston/aervium/core/module/OptimizationModule.java`
- Role: Optimizations that are switched on or off before the game starts, with their key in `config/aervium-modules.properties` and whether they touch the renderer (off when Sodium or Iris is installed).
- Depends on: nothing.

#### ModuleSettings
- Path: `src/main/java/com/aryston/aervium/core/module/ModuleSettings.java`
- Role: Reads `key=true|false` lines (unknown lines are ignored, every module defaults to on) and writes every module back so players see all switches.
- Depends on: `OptimizationModule`.

### `com.aryston.aervium.core.collection`

#### ShardedLongMap
- Path: `src/main/java/com/aryston/aervium/core/collection/ShardedLongMap.java`
- Role: Read-only `Long2ObjectOpenHashMap` view over 128 immutable shards (by mixed key hash); answers `get`, `getOrDefault`, `containsKey`, `size` and `isEmpty`, throws on writes and iteration, `clone()` returns itself.
- Depends on: nothing (fastutil).

#### ShardedLongMapBuilder
- Path: `src/main/java/com/aryston/aervium/core/collection/ShardedLongMapBuilder.java`
- Role: Keeps a mutable mirror of a map in 128 shards and publishes `ShardedLongMap` snapshots that copy only the shards written since the last snapshot; unchanged shards are shared between snapshots.
- Members: `copyOf(Long2ObjectMap)`, `put(long, V)`, `remove(long)`, `publish()`.
- Depends on: `ShardedLongMap`.

### `com.aryston.aervium.core.job`

#### JobSystem
- Path: `src/main/java/com/aryston/aervium/core/job/JobSystem.java`
- Role: Work-stealing worker pool (`ForkJoinPool` in async mode) with named daemon threads below normal priority; `defaultWorkers` leaves two cores to the game threads and caps at eight.
- Depends on: nothing.

#### MainThreadQueue
- Path: `src/main/java/com/aryston/aervium/core/job/MainThreadQueue.java`
- Role: Results of jobs that must be applied on the main thread; `drain(budget)` runs them in order until the time budget is spent (at least one per call).
- Depends on: nothing.

### `com.aryston.aervium.world`

#### TickTimeRecorder
- Path: `src/main/java/com/aryston/aervium/world/TickTimeRecorder.java`
- Role: Records the duration of every server tick of the integrated or dedicated server into a ring buffer and the `SERVER_TICK` timing scope.
- Members:
  - `SERVER`: the instance the server events write to.
  - `mark()`, `since(long)`, `latest(int)`: copies for the overlay and the benchmark.
- Depends on: `SampleRing`, `SystemTimings`.
- Notes: Written on the server thread and read on the client thread, so the ring is accessed under a lock.

### `com.aryston.aervium.debug.route`

#### CameraPath
- Path: `src/main/java/com/aryston/aervium/debug/route/CameraPath.java`
- Role: Sealed interface of a camera route: `pose(tick)` and whether the height follows the ground.
- Depends on: `CameraPose`, `LinePath`, `OrbitPath`.

#### CameraPose
- Path: `src/main/java/com/aryston/aervium/debug/route/CameraPose.java`
- Role: Record of a feet position and the yaw and pitch of the camera.
- Depends on: nothing.

#### LinePath
- Path: `src/main/java/com/aryston/aervium/debug/route/LinePath.java`
- Role: Straight route at a constant speed in the direction of its yaw (Minecraft convention: yaw 0 looks south).
- Depends on: `CameraPose`.

#### OrbitPath
- Path: `src/main/java/com/aryston/aervium/debug/route/OrbitPath.java`
- Role: Circle around a center at a height, always looking at the center, one turn per `ticksPerTurn`.
- Depends on: `CameraPose`.

### `com.aryston.aervium.debug.benchmark`

#### BenchmarkScene
- Path: `src/main/java/com/aryston/aervium/debug/benchmark/BenchmarkScene.java`
- Role: The scenes in route order: walk, elytra, village, underground, redstone, with id and description.
- Depends on: nothing.

#### BenchmarkRoute
- Path: `src/main/java/com/aryston/aervium/debug/benchmark/BenchmarkRoute.java`
- Role: Timing (400 warm-up ticks, then 200 settle and 600 measured ticks per scene) and the camera path of every scene from the anchors.
- Depends on: `BenchmarkScene`, `BenchmarkAnchors`, `BenchmarkCommands`, `LinePath`, `OrbitPath`.

#### BenchmarkCommands
- Path: `src/main/java/com/aryston/aervium/debug/benchmark/BenchmarkCommands.java`
- Role: Server commands of the benchmark: frozen time and weather without mob spawning, a crowd of 40 villagers on the village surface, and a floating platform with 96 observer clocks driving sticky pistons and redstone lamps.
- Depends on: `BlockPoint`.
- Notes: The second observer row is placed last so each pair starts pulsing when it is built.

#### BlockPoint
- Path: `src/main/java/com/aryston/aervium/debug/benchmark/BlockPoint.java`
- Role: Game-independent block position with offset and command coordinates.
- Depends on: nothing.

#### BenchmarkAnchors
- Path: `src/main/java/com/aryston/aervium/debug/benchmark/BenchmarkAnchors.java`
- Role: Record of the spawn, the village center (spawn when none was found) and the redstone platform origin.
- Depends on: `BlockPoint`.

#### SceneMeasurement
- Path: `src/main/java/com/aryston/aervium/debug/benchmark/SceneMeasurement.java`
- Role: Marks of the frame, tick and GC recorders at the start of a measured scene, peak heap and VRAM during it, and the `SceneResult` at its end including frames lost from a full ring.
- Depends on: `SampleRing`, `FrameTimeSummary`, `TickSummary`, `GcPauseTotals`, `SceneResult`.

#### SceneResult
- Path: `src/main/java/com/aryston/aervium/debug/benchmark/SceneResult.java`
- Role: Record of one measured scene for the JSON report.
- Depends on: `FrameTimeSummary`, `TickSummary`, `GcPauseTotals`.

#### BenchmarkEnvironment
- Path: `src/main/java/com/aryston/aervium/debug/benchmark/BenchmarkEnvironment.java`
- Role: Record of versions, operating system, CPU threads, heap limit, GPU, window size, render and simulation distance, frame limit, vsync, seed and installed mods.
- Depends on: nothing.

#### BenchmarkReport
- Path: `src/main/java/com/aryston/aervium/debug/benchmark/BenchmarkReport.java`
- Role: Record of a whole run with format version, start time, environment, anchors, scenes and overall frame and tick summaries.
- Depends on: `BenchmarkEnvironment`, `BenchmarkAnchors`, `SceneResult`.

#### BenchmarkComparison
- Path: `src/main/java/com/aryston/aervium/debug/benchmark/BenchmarkComparison.java`
- Role: Percentage change of average FPS, both lows and MSPT per scene against an earlier report, and the largest FPS change (the repeatability check).
- Depends on: `BenchmarkReport`, `SceneResult`.

#### BenchmarkText
- Path: `src/main/java/com/aryston/aervium/debug/benchmark/BenchmarkText.java`
- Role: Readable report: environment, a table per scene and overall, the MSPT distribution, VRAM and GC details and the comparison.
- Depends on: `BenchmarkReport`, `BenchmarkComparison`.

#### BenchmarkFiles
- Path: `src/main/java/com/aryston/aervium/debug/benchmark/BenchmarkFiles.java`
- Role: Writes `benchmark-<start>.json` and `.txt` and reads the newest earlier JSON report of the same format version.
- Depends on: `BenchmarkReport`.

### `com.aryston.aervium.integration`

#### CommonEvents
- Path: `src/main/java/com/aryston/aervium/integration/CommonEvents.java`
- Role: Wires `ServerTickEvent.Pre` and `Post` and `ServerStoppedEvent` to `TickTimeRecorder` on both sides.
- Depends on: `TickTimeRecorder`.

#### LightSnapshotSource
- Path: `src/main/java/com/aryston/aervium/integration/light/LightSnapshotSource.java`
- Role: Interface `DataLayerStorageMapMixin` adds to the light storage maps: `aervium$snapshot()` returns the map for a new visible copy.
- Depends on: nothing.

#### CompatibilityGuard
- Path: `src/main/java/com/aryston/aervium/integration/CompatibilityGuard.java`
- Role: Lists installed renderer mods (Sodium, Iris) that turn Aervium's optimizations off; `REPLACED_RENDERERS` is shared with `MixinModules`.
- Depends on: nothing inside the mod.

#### MixinModules
- Path: `src/main/java/com/aryston/aervium/integration/MixinModules.java`
- Role: Mixin config plugin: reads and rewrites `config/aervium-modules.properties` before any mixin applies, skips the mixins of switched-off modules and skips renderer modules when Sodium or Iris is in the mod list.
- Depends on: `ModuleSettings`, `OptimizationModule`, `CompatibilityGuard`.
- Notes: Runs before mods are constructed, so it reads the mod list through `FMLLoader.getCurrent().getLoadingModList()`.

### `com.aryston.aervium.config`

#### AerviumConfig
- Path: `src/main/java/com/aryston/aervium/config/AerviumConfig.java`
- Role: Client config: overlay visibility (saved when F7 toggles it), the target frame rate of the frame budget, and dynamic frame rate (on by default, 30 FPS in the background, 60 FPS while paused).
- Depends on: `FrameBudget`, `FrameRatePolicy`.

### `com.aryston.aervium.client`

#### AerviumClient
- Path: `src/main/java/com/aryston/aervium/client/AerviumClient.java`
- Role: Client entry point (`@Mod` with `Dist.CLIENT`): registers the client config, the NeoForge config screen and the client events.
- Depends on: `AerviumConfig`, `ClientEvents`.

#### ClientEvents
- Path: `src/main/java/com/aryston/aervium/client/ClientEvents.java`
- Role: Client adapter: frame start and end (`RenderFrameEvent`), client tick scopes, the F7 key, the overlay layer, config loading (overlay, budget, dynamic frame rate), Sodium and Iris check, installing the shader caches and the pipeline warm-up (only without Sodium and Iris), the startup report, world joins and shutdown (saves the pipeline cache before the job system stops).
- Depends on: `ClientTelemetry`, `TelemetryOverlay`, `BenchmarkDriver`, `AerviumKeys`, `AerviumConfig`, `CompatibilityGuard`, `SystemTimings`, `ShaderCaches`, `PipelineWarmup`, `FrameRateGovernor`.

#### FrameRateGovernor
- Path: `src/main/java/com/aryston/aervium/client/FrameRateGovernor.java`
- Role: Applies the `FrameRatePolicy` to the vanilla frame limit from the window focus and pause state; suspended while the benchmark runs.
- Depends on: `FrameRatePolicy`.

#### ClientTelemetry
- Path: `src/main/java/com/aryston/aervium/client/ClientTelemetry.java`
- Role: Owns the overlay frame recorder (2048 frames), the GC recorder, the job system and the main-thread queue; every five client ticks it copies the raw data, summarizes it on a worker and publishes the snapshot on the main thread within 0.5 ms.
- Depends on: `FrameTimeRecorder`, `GcPauseRecorder`, `JobSystem`, `MainThreadQueue`, `TickTimeRecorder`, `SystemTimings`, `VulkanMemoryProbe`.

#### TelemetryOverlay
- Path: `src/main/java/com/aryston/aervium/client/TelemetryOverlay.java`
- Role: GUI layer in the top right corner: text lines and a 240 frame time graph (0 to 50 ms) with the budget line; bars are green, yellow over budget and red over twice the budget. Reads frames into a preallocated array, so drawing allocates nothing of its own.
- Depends on: `ClientTelemetry`, `OverlayLines`, `FrameBudget`, `SystemTimings`.

#### OverlayLines
- Path: `src/main/java/com/aryston/aervium/client/OverlayLines.java`
- Role: Turns a `TelemetrySnapshot` into colored text lines; rebuilt only when a snapshot arrives.
- Depends on: `TelemetrySnapshot`, `ByteSizes`.

#### AerviumKeys
- Path: `src/main/java/com/aryston/aervium/client/AerviumKeys.java`
- Role: Key binding category and the overlay toggle (F7).
- Depends on: `Aervium`.

#### VulkanMemoryProbe
- Path: `src/main/java/com/aryston/aervium/client/VulkanMemoryProbe.java`
- Role: Reads used and budgeted device-local memory from the game's Vulkan memory allocator (`vmaGetHeapBudgets`).
- Depends on: `FrontendGpuDeviceAccessor`, `VramUsage`.

### `com.aryston.aervium.client.chunk`

#### SectionSliceHolder
- Path: `src/main/java/com/aryston/aervium/client/chunk/SectionSliceHolder.java`
- Role: Interface `CompiledSectionMeshMixin` adds to every compiled chunk section mesh: one cached terrain buffer slice per layer, or `ABSENT` when the mesh has no buffer for that layer.
- Depends on: nothing.

#### SectionLayers
- Path: `src/main/java/com/aryston/aervium/client/chunk/SectionLayers.java`
- Role: One shared `ChunkSectionLayer.values()` array.
- Notes: Lives in its own class so the enum is first loaded at the first frame; a static field in a mixin would load it inside the static setup of `LevelRenderer`, before `RenderPipelines` exists, and every layer would get a null pipeline.

### `com.aryston.aervium.client.pipeline`

#### ShaderCaches
- Path: `src/main/java/com/aryston/aervium/client/pipeline/ShaderCaches.java`
- Role: Holds the SPIR-V cache (`<game>/aervium/cache/spirv/`) and the Vulkan pipeline cache (`<game>/aervium/cache/vulkan-pipelines.bin`) for the mixins, counts pipelines compiled late on the render thread (logs every one over 1 ms), writes the startup report (time to the first screen, shader hits and misses, pipeline creation time) and saves the pipeline cache on shutdown.
- Depends on: `SpirvCache`, `VulkanPipelineCache`, `DiskCache`, `CacheStatistics`, `JobSystem`.

#### SpirvCache
- Path: `src/main/java/com/aryston/aervium/client/pipeline/SpirvCache.java`
- Role: Returns cached SPIR-V for a GLSL compilation when the key matches, otherwise compiles with shaderc and writes the result on a worker. Cached binaries are copied into native memory the way vanilla allocates them, so vanilla frees them as usual.
- Depends on: `ShaderCacheKey`, `SpirvBinary`, `DiskCache`, `CacheStatistics`, `JobSystem`.

#### VulkanPipelineCache
- Path: `src/main/java/com/aryston/aervium/client/pipeline/VulkanPipelineCache.java`
- Role: One `VkPipelineCache` for every graphics pipeline vanilla creates, created on first use from the saved data when the header matches the GPU and driver, saved and destroyed on shutdown.
- Depends on: `PipelineCacheHeader`, `CacheStatistics`.
- Notes: Vulkan pipeline caches are internally synchronized, so pipelines compiled on several workers share it safely.

#### PipelineWarmup
- Path: `src/main/java/com/aryston/aervium/client/pipeline/PipelineWarmup.java`
- Role: When vanilla installs a new pipeline cache after a resource reload, compiles on the job system every built-in pipeline of `RenderPipelines` that vanilla did not register (variants that share a location, such as the three `pipeline/item_cutout` pipelines) and inserts them on the main thread through the budgeted `MainThreadQueue`, so they are never compiled in the middle of a game frame.
- Depends on: `PipelineCacheAccessor`, `JobSystem`, `MainThreadQueue`.

### `com.aryston.aervium.client.benchmark`

#### BenchmarkDriver
- Path: `src/main/java/com/aryston/aervium/client/benchmark/BenchmarkDriver.java`
- Role: Benchmark state machine on the client tick: creates the world from the first screen, prepares and locates, warms up, runs every scene, measures, writes the report on a worker and quits when asked. Turns pause on lost focus off for the run and restores it.
- Members:
  - `requested(...)`: present when `-Daervium.benchmark=true` is set; `-Daervium.benchmark.quit=true` closes the game afterwards.
  - `frameStarted(long)`: records frames into its own 131072 frame ring while scenes run.
- Depends on: `BenchmarkWorld`, `BenchmarkCamera`, `BenchmarkEnvironmentProbe`, `BenchmarkRoute`, `SceneMeasurement`, `BenchmarkFiles`, `BenchmarkText`, `BenchmarkComparison`, `TickTimeRecorder`, `GcPauseRecorder`, `JobSystem`, `MainThreadQueue`.

#### BenchmarkWorld
- Path: `src/main/java/com/aryston/aervium/client/benchmark/BenchmarkWorld.java`
- Role: Deletes the previous `aervium-benchmark` world, creates a fresh one (seed 8675309, spectator, peaceful, cheats on), runs the world rules, finds the spawn, the nearest village and the redstone platform height on the server thread, and builds each scene.
- Depends on: `BenchmarkCommands`, `BenchmarkAnchors`, `BlockPoint`.

#### BenchmarkCamera
- Path: `src/main/java/com/aryston/aervium/client/benchmark/BenchmarkCamera.java`
- Role: Moves the spectator player along a `CameraPath` every client tick; walking routes follow the client heightmap with at most 0.6 blocks of climb per tick.
- Depends on: `CameraPath`, `CameraPose`.

#### BenchmarkEnvironmentProbe
- Path: `src/main/java/com/aryston/aervium/client/benchmark/BenchmarkEnvironmentProbe.java`
- Role: Collects the `BenchmarkEnvironment` from the game, the options, the window, the GPU and the mod list.
- Depends on: `BenchmarkEnvironment`, `BenchmarkWorld`.

### `com.aryston.aervium.mixin`

#### FrontendGpuDeviceAccessor
- Path: `src/main/java/com/aryston/aervium/mixin/FrontendGpuDeviceAccessor.java`
- Role: Accessor for the private `backend` of `FrontendGpuDevice`, to reach the Vulkan memory allocator.
- Notes: Mixin target `com.mojang.renderpearl.frontend.FrontendGpuDevice`. No API exposes the allocator or video memory use.

#### GlslCompilerMixin
- Path: `src/main/java/com/aryston/aervium/mixin/GlslCompilerMixin.java`
- Role: Wraps `GlslCompiler.compileToSpv` with the `SpirvCache`.
- Notes: Module `shaderCache`. No API lets a mod supply compiled shaders.

#### VulkanRenderPipelineMixin
- Path: `src/main/java/com/aryston/aervium/mixin/VulkanRenderPipelineMixin.java`
- Role: Passes the shared `VkPipelineCache` to every `vkCreateGraphicsPipelines` call in `VulkanRenderPipeline.compile` (vanilla passes none) and times the call.
- Notes: Module `pipelineCache`. Renderpearl offers no hook for pipeline creation.

#### PipelineCacheMixin
- Path: `src/main/java/com/aryston/aervium/mixin/PipelineCacheMixin.java`
- Role: Times `PipelineCache.get` when the pipeline is not compiled yet, which compiles it on the calling (render) thread.
- Notes: Measurement only, always on.

#### PipelineCacheAccessor
- Path: `src/main/java/com/aryston/aervium/mixin/PipelineCacheAccessor.java`
- Role: Accessors for the device, the shader source and the compiled map of a `PipelineCache`, used by `PipelineWarmup`.

#### RenderSystemMixin
- Path: `src/main/java/com/aryston/aervium/mixin/RenderSystemMixin.java`
- Role: Starts the `PipelineWarmup` when `RenderSystem.setCurrentPipelineCache` installs a new cache.
- Notes: Module `pipelineWarmup`. No event fires after the shader manager applies its pipelines.

#### FramerateLimitTrackerMixin
- Path: `src/main/java/com/aryston/aervium/mixin/FramerateLimitTrackerMixin.java`
- Role: Passes the vanilla frame limit through `FrameRateGovernor`.
- Notes: Vanilla limits minimized, idle and menu frames but not a window in the background; there is no API for the limit.

#### PalettedContainerMixin
- Path: `src/main/java/com/aryston/aervium/mixin/PalettedContainerMixin.java`
- Role: Replaces the per-container `ThreadingDetector` (a semaphore, a lock and their syncs, about 128 bytes in every chunk section container) with one `AtomicBoolean`; a second thread entering a container in use still ends in the vanilla "Accessing PalettedContainer from multiple threads" crash report, now with the stack of the thread that entered second.
- Notes: Module `compactChunkLocks`, both sides. Saves about 100 bytes per container: 48.3 MB of lock objects became 15.5 MB with 332000 containers (client and integrated server) in the benchmark village at render distance 16, and the compare-and-set is cheaper than a semaphore and a lock per access (average MSPT was 12 % lower with all modules on in the first comparison; to be confirmed with more runs). Game behavior is unchanged; only the diagnostic of a threading bug differs.

#### LevelRendererSectionSliceMixin
- Path: `src/main/java/com/aryston/aervium/mixin/LevelRendererSectionSliceMixin.java`
- Role: In `LevelRenderer.extractSectionDrawGroups`, answers `getRenderSectionSlice` from the slice cached in the mesh and asks vanilla only on a miss, and replaces the `ChunkSectionLayer.values()` copy for every section with `SectionLayers.ALL`.
- Notes: Module `sectionSliceCache`, renderer module. Vanilla looks up two `HashMap` entries per section and layer in every pass (the camera and each of Helion's shadow cascades) and allocates a new slice record each time; with Helion at render distance 32 that lookup was about 27 % of the render thread. Turning the module on raised the benchmark 1% low from 36.1 to 50.9 FPS and the cave scene from 58 to 78 FPS average (ZGC, Helion and Arkea, 2026-10-12).

#### CompiledSectionMeshMixin
- Path: `src/main/java/com/aryston/aervium/mixin/CompiledSectionMeshMixin.java`
- Role: Implements `SectionSliceHolder` on `CompiledSectionMesh` with one field per vanilla layer (an array only for layers beyond three), so a lookup reads the mesh object the draw loop has just touched.
- Notes: Module `sectionSliceCache`.

#### UberGpuBufferMixin
- Path: `src/main/java/com/aryston/aervium/mixin/UberGpuBufferMixin.java`
- Role: Clears a mesh's cached slices whenever `UberGpuBuffer` frees its allocation and clears every cached slice when the buffer closes.
- Notes: Module `sectionSliceCache`. Every new allocation of a mesh is preceded by `freeAllocation` of that mesh in `uploadStagedAllocations`, and allocations never move inside a heap, so a cached slice (or `ABSENT`) is always what vanilla would return.

#### DataLayerStorageMapMixin
- Path: `src/main/java/com/aryston/aervium/mixin/DataLayerStorageMapMixin.java`
- Role: Follows every write of a light storage map (`setLayer`, `copyDataLayer`, `removeLayer`) in a `ShardedLongMapBuilder` and implements `LightSnapshotSource`; the builder starts at the first copy, and a map that already is a snapshot returns itself.
- Notes: Module `lightSnapshots`, both sides.

#### BlockDataLayerStorageMapMixin, SkyDataLayerStorageMapMixin
- Path: `src/main/java/com/aryston/aervium/mixin/BlockDataLayerStorageMapMixin.java`, `src/main/java/com/aryston/aervium/mixin/SkyDataLayerStorageMapMixin.java`
- Role: In `copy()` of the block and sky light maps, replace the `clone()` of the whole section map with `LightSnapshotSource.aervium$snapshot()`.
- Notes: Module `lightSnapshots`, both sides. Vanilla `LayerLightSectionStorage.swapSectionMap` cloned the full map (several MB at render distance 32) after every light batch so other threads read a stable copy; the snapshot is still a complete, never-changing copy published at the same moment, so readers see exactly the vanilla data. It was about 18 % of all allocation; with G1 the benchmark spent 9 % less time in GC pauses, the 0.1% low rose from 14.6 to 17.9 FPS and the heap peak fell from 4014 to 3046 MB (1% low unchanged); with ZGC the difference was within noise.

## Tests

Plain JUnit 5 tests without a running game, run by `./gradlew build` and the CI. Each test class sits in the package of the class it checks.

| File | What it checks |
|---|---|
| `src/test/java/com/aryston/aervium/LanguageFilesTest.java` | Every language file has the same keys and every config entry has a `.tooltip` translation (lesson L-011). |
| `src/test/java/com/aryston/aervium/core/telemetry/SampleRingTest.java` | Order, overwriting the oldest samples, copies into existing arrays, marks, clearing, invalid capacity. |
| `src/test/java/com/aryston/aervium/core/telemetry/SampleStatisticsTest.java` | Nearest-rank percentiles, mean, population standard deviation, sorting only the counted prefix, empty samples. |
| `src/test/java/com/aryston/aervium/core/telemetry/FrameTimeSummaryTest.java` | Steady frames give equal average and lows; spikes lower the 1% and 0.1% lows and the average as computed by hand. |
| `src/test/java/com/aryston/aervium/core/telemetry/TickSummaryTest.java` | Histogram buckets and their edges, ticks over 50 ms and their share, median. |
| `src/test/java/com/aryston/aervium/core/telemetry/FrameTimeRecorderTest.java` | Frame times are the gaps between frame starts; a reset drops the gap. |
| `src/test/java/com/aryston/aervium/core/collection/ShardedLongMapTest.java` | Snapshot equals the source, published snapshots never change, random writes against a plain map, read-only behavior. |
| `src/test/java/com/aryston/aervium/core/telemetry/GcAdviceTest.java` | Collector recognition from bean names, advice for G1 and for a small heap. |
| `src/test/java/com/aryston/aervium/core/telemetry/GcPauseRecorderTest.java` | Pauses versus concurrent cycles, totals, pauses after a mark. |
| `src/test/java/com/aryston/aervium/core/budget/FrameBudgetTest.java` | Frame and system budgets, fixed 50 ms tick budget, budget edge, invalid target. |
| `src/test/java/com/aryston/aervium/core/budget/SystemTimingsTest.java` | Smoothing, peaks reset on sampling, over budget flags. |
| `src/test/java/com/aryston/aervium/core/job/JobSystemTest.java` | Worker count rule, every job runs on named workers, no jobs after closing. |
| `src/test/java/com/aryston/aervium/core/job/MainThreadQueueTest.java` | Order, time budget, at least one task per drain. |
| `src/test/java/com/aryston/aervium/debug/route/CameraPathTest.java` | Lines move along their yaw; orbits keep their radius and height, look at the center and close after one turn. |
| `src/test/java/com/aryston/aervium/core/cache/ShaderCacheKeyTest.java` | Direct and transitive includes (with a cycle), stable keys for reordered defines, a new key for every change that affects the binary (defines, flags, device switches, include content). |
| `src/test/java/com/aryston/aervium/core/cache/CacheFilesTest.java` | Disk cache round trip without leftover temporary files, existing entries kept, eight concurrent writers and readers of one key, SPIR-V validation, pipeline cache header checks, cache counters. |
| `src/test/java/com/aryston/aervium/core/budget/FrameRatePolicyTest.java` | Active, background and paused limits, never above a lower vanilla limit, disabled policy. |
| `src/test/java/com/aryston/aervium/core/module/ModuleSettingsTest.java` | Modules default to on, switched-off and unknown lines, every module written back. |
| `src/test/java/com/aryston/aervium/debug/benchmark/BenchmarkCommandsTest.java` | Crowd size and distinct positions, build order of the redstone platform, every fill within the vanilla volume limit, frozen time and weather. |
| `src/test/java/com/aryston/aervium/debug/benchmark/SceneMeasurementTest.java` | Only frames after the scene start count, peaks of heap and VRAM, frames lost from a full ring. |
| `src/test/java/com/aryston/aervium/debug/benchmark/BenchmarkReportTest.java` | Comparison percentages, JSON round trip, newest report by start time, missing folder, text report content. |

## Source Files

| File | Purpose |
|---|---|
| `src/main/templates/META-INF/neoforge.mods.toml` | Mod metadata template filled from `gradle.properties`: NeoForge and Minecraft on both sides, `IGNORE_ALL_VERSION` so either side works alone, discouraged Sodium and Iris, mixin config. |
| `src/main/resources/aervium.mixins.json` | Mixin configuration with the `MixinModules` plugin: `BlockDataLayerStorageMapMixin`, `DataLayerStorageMapMixin`, `PalettedContainerMixin` and `SkyDataLayerStorageMapMixin` on both sides; `CompiledSectionMeshMixin`, `LevelRendererSectionSliceMixin`, `UberGpuBufferMixin`, `FramerateLimitTrackerMixin`, `FrontendGpuDeviceAccessor`, `GlslCompilerMixin`, `PipelineCacheAccessor`, `PipelineCacheMixin`, `RenderSystemMixin` and `VulkanRenderPipelineMixin` on the client. |

## Asset Folders

| Folder | Contents |
|---|---|
| `src/main/resources/assets/aervium/lang/` | `en_us.json` and `tr_tr.json`: key binding and config texts. |

## Build Files

| File | Purpose |
|---|---|
| `build.gradle` | ModDevGradle setup, JUnit 5 for `src/test/java` with the Minecraft classpath of `main`, `client` run forced to Vulkan, `server` run without GUI, metadata expansion, packs `branding/logo.png` into the jar as `aervium.png`, jar name `aervium-neoforge-<minecraft_version>-<version>.jar`. |
| `gradle.properties` | Single place for versions and mod metadata. |
| `settings.gradle` | Plugin repositories, Java toolchain resolver, project name. |
| `.github/workflows/build.yml` | CI on every push and pull request: JDK 25, `./gradlew build`, uploads the jar. |
| `.github/workflows/release.yml` | On a `v*` tag: checks the tag against `mod_version`, builds, creates the GitHub release with the changelog section, and uploads to Modrinth and CurseForge when their secrets exist. |
| `.github/workflows/listing.yml` | Syncs the Modrinth project page (README, links, license, optional on both sides, category optimization) on pushes to `main`. |
