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
| Refusing to load next to Sodium (loading error in the game language) | `SodiumClash`, `LoadingText`, `Aervium` |
| Shader and pipeline caches, startup report | `ShaderCaches`, `SpirvCache`, `VulkanPipelineCache`, `ShaderCacheKey`, `DiskCache` |
| Pipelines compiled in the middle of a frame | `PipelineWarmup`, `PipelineCacheMixin` |
| Dynamic frame rate | `FrameRatePolicy`, `FrameRateGovernor`, `FramerateLimitTrackerMixin` |
| Chunk memory | `PalettedContainerMixin` |
| First launch hardware class (lower distances, leaf culling on weak machines) | `HardwareClassifier`, `HardwareTier`, `HardwareProfile`, `GpuKind`, `HardwareSetup`, `VulkanMemoryProbe` |
| Faster point of interest searches (villager beds, job sites) | `ColumnIndexedLongMap`, `PoiColumns`, `PoiAudit`, `SectionStoragePoiIndexMixin`, `SectionStorageAccessor`, `PoiManagerColumnSearchMixin` |
| Entity collision search skip (sections without boats, shulkers, happy ghasts) | `CollidableEntities`, `CollisionAudit`, `EntitySectionCollidableMixin`, `PersistentEntitySectionManagerAccessor`, `ServerLevelEntityCollisionsMixin` |
| Idle hopper entity search skip (sections without items or item-holding entities) | `HopperEntities`, `HopperAudit`, `SectionCounts`, `EntitySectionHopperMixin`, `ServerLevelEntityManagerAccessor`, `EntityCapabilityAccessor`, `HopperBlockEntityItemSearchMixin`, `HopperEntityContainerSearchMixin` |
| Faster packet compression on servers (level 4 instead of 6) | `PacketCompression`, `CompressionCheck`, `CompressionEncoderLevelMixin` |
| Parallel feature generation (trees, ores, plants on several threads) | `ParallelFeatures`, `RegionLocks`, `JobSystem`, `ChunkStatusTasksFeaturesMixin` |
| Crash report entry (modules on, Aervium code in the crash) | `CrashSummary`, `CrashReportSection`, `CrashReportMixin`, `MixinModules` |
| Light engine snapshots | `ShardedLongMap`, `ShardedLongMapBuilder`, `DataLayerStorageMapMixin`, `BlockDataLayerStorageMapMixin`, `SkyDataLayerStorageMapMixin` |
| Chunk draw list (terrain buffer lookups per frame) | `SectionDrawLists`, `SectionDrawRecord`, `LevelRendererSectionSliceMixin`, `DirectDraws`, `PackedDraws`, `PackedDrawPool`, `PackedSectionInfos`, `SectionInfoStorage`, `DrawCommandRing`, `LevelRendererDirectDrawMixin`, `RenderSectionMixin`, `CompiledSectionMeshMixin`, `UberGpuBufferMixin`, `SectionSliceHolder` |
| Block entity scan (skip sections without block entities) | `BlockEntitySectionHolder`, `RenderSectionBlockEntityMixin`, `LevelExtractorBlockEntitiesMixin` |
| Occlusion culling (caves, Helion shadow casters, entities, particles) | `TerrainOcclusion`, `ShadowCascadeCulling`, `CameraResult`, `TileMask`, `EntityOcclusion`, `ParticleOcclusion`, `HelionShadowCasters`, `OcclusionPipeline`, `OcclusionPass`, `DepthRaster`, `CameraOcclusionHolder`, `RenderSectionOcclusionMixin`, `OpaqueMask`, `OccluderBoxes`, `VisGraphMixin`, `LevelRendererOcclusionMixin`, `LevelExtractorOcclusionMixin`, `QuadParticleGroupOcclusionMixin`, `ParticleAccessor`, `HelionShadowCastersMixin` |
| Chunk mesh workers (own thread pool for chunk builds) | `MeshWorkers`, `LevelRendererMeshWorkersMixin`, `JobSystem`, `JobLoadMeter` |
| Leaf culling (optional, off by default) | `LeafCulling`, `ModelBlockRendererLeafCullingMixin`, `AerviumConfig` |
| Chunk upload and builder lock timings | `ChunkUploadTimings`, `StallMeter`, `SectionRenderDispatcherTimingMixin` |
| Server light worker timings | `ServerLightTimings`, `ThreadedLevelLightEngineTimingMixin` |
| Face culling (skip terrain faces turned away from the camera) | `FacingSort`, `FaceRanges`, `FaceCulling`, `FaceRangesHolder`, `CompiledSectionMeshFacingMixin`, `LevelRendererFaceCullingMixin`, `SectionDrawLists` |
| Compact terrain vertices (16 instead of 28 bytes, off with a Helion that cannot read them) | `CompactVertexEncoder`, `CompactTerrainFormat`, `CompactMeshUpload`, `CompactWireframe`, `RenderPipelineBuilderCompactMixin`, `SectionCompilerCompactMixin`, `CompileTaskCompactMixin`, `MeshDataCompactMixin`, `ChunkSectionsToRenderCompactMixin`, `shaders/terrain/compact.vsh` |
| Dirty section scan (clean sections answered from an array) | `DirtySectionMirror`, `DirtySectionIndex`, `SectionDirtyStateMirrorMixin`, `SectionUpdateTrackerMirrorMixin`, `LevelExtractorDirtySectionsMixin` |
| GPU culling (advanced terrain path: the GPU builds the solid and cutout draw lists) | `GpuCulling`, `GpuSections`, `GpuSectionTable`, `GpuSectionEntry`, `TerrainGroups`, `GpuDrawList`, `CullPipeline`, `VulkanAccess`, `IndirectCountFeature`, `GpuBufferUsage`, `ChunkSectionsToRenderIndirectCountMixin`, `VulkanFeatureSetsMixin`, `VulkanConstStorageMixin`, `shaders/culling/terrain_draws.csh` |
| Section grid moves (only the edge columns when the camera enters another chunk) | `RotatingSectionStorageEdgeMixin` |
| Switching modules on and off | `OptimizationModule`, `ModuleSettings`, `MixinModules` |
| Sodium and Iris detection | `CompatibilityGuard` |
| Settings | `AerviumConfig`, `lang/*.json` |
| Key bindings | `AerviumKeys` |
| Event wiring | `CommonEvents` (both sides), `ClientEvents` (client) |
| Mod name, version, loader versions, license, authors | `gradle.properties` |
| Mod list metadata and dependencies | `neoforge.mods.toml` |
| Mod logo | `branding/logo.png` (temporary placeholder) |
| Build setup, run configurations | `build.gradle` |
| Vanilla parity check (same world after N ticks with modules on and off) | `ParityHarness`, `ParityScenario`, `WorldgenCheck`, `ChunkStatusTasksTerrainMixin`, `StateHash`, `LevelRandomAccessor`, `runParity` in `build.gradle` |
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
- Role: Frame time statistics for players: average FPS, 1% low (FPS at the 99th percentile frame time), 0.1% low (99.9th percentile), average, standard deviation, median, p99, p99.9 frames over 33 and 50 ms (counted before the samples are sorted), and maximum in milliseconds.
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

#### FrameTraceEvent
- Path: `src/main/java/com/aryston/aervium/core/telemetry/FrameTraceEvent.java`
- Role: JFR event `aervium.Frame` for one frame, from `RenderFrameEvent.Pre` to the next one: render, submit and present, client ticks, the parts of submitting (queue submit, waiting for an earlier frame on the GPU, resource destruction, image present and acquire), GPU times of an earlier frame (whole frame and rendering before the swapchain copy), chunk lock wait, uploads, culling, camera result, sections, GPU table writes, light updates, chunk packets, skipped uploads, buffer heaps created or destroyed and Vulkan memory allocations and releases with their time. Off unless a recording enables it, so normal play pays nothing.
- Depends on: nothing.

#### StageTimes
- Path: `src/main/java/com/aryston/aervium/core/telemetry/StageTimes.java`
- Role: Named stages (up to 128) with total and longest time over the measured frames; `take` returns the averages per frame and resets.
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

#### StallMeter
- Path: `src/main/java/com/aryston/aervium/core/budget/StallMeter.java`
- Role: Count, total and longest of a repeated render thread stall since the last `take`, for the benchmark log. Render thread only, no locking.
- Depends on: nothing.

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

### `com.aryston.aervium.core.culling`

#### ClipMatrix
- Path: `src/main/java/com/aryston/aervium/core/culling/ClipMatrix.java`
- Role: Column-major 4x4 matrix without allocation: perspective and orthographic projections with a 0 to 1 depth range, multiplication, and `depthFromW`, which replaces the depth row of any perspective matrix with a standard 0 (near) to 1 (far) depth so the rasterizer never depends on the game's depth convention (Vulkan Minecraft uses reversed depth).
- Depends on: nothing.

#### DepthRaster
- Path: `src/main/java/com/aryston/aervium/core/culling/DepthRaster.java`
- Role: Small software depth buffer (for example 256 x 144) for occlusion tests. `addOccluder` draws the front faces of a box, clipped at the near plane; `isVisible` tests a box against it with 8 x 8 tiles that remember their farthest depth.
- Notes: Both sides are conservative, so a visible box is never reported hidden: an occluder only covers pixels lying fully inside its face and writes the farthest depth the face reaches inside that pixel; a tested box covers every pixel it touches with its nearest depth. Boxes crossing the near plane or behind the camera are always visible. With `screenBounded` false (light space) a box reaching outside the raster is visible.
- Depends on: `ClipMatrix`.

#### OpaqueMask
- Path: `src/main/java/com/aryston/aervium/core/culling/OpaqueMask.java`
- Role: The opaque blocks of one 16x16x16 section (the same bits vanilla's `VisGraph` collects, blocks with `isSolidRender`) turned into occluder boxes: fully opaque 2x2x2 cells merged greedily into boxes, at most 24 per section, largest first; a fully opaque section is one box.
- Depends on: `OccluderBoxes`.

#### VoxelRay
- Path: `src/main/java/com/aryston/aervium/core/culling/VoxelRay.java`
- Role: Block-by-block ray walk (`reaches`: does a ray get from one point to another without entering an opaque block, the start block excluded) and the segment parameter where a ray enters a box (`entry`). Used by the culling audit.
- Depends on: nothing.

#### OccluderBoxes
- Path: `src/main/java/com/aryston/aervium/core/culling/OccluderBoxes.java`
- Role: Immutable occluder boxes of a section in section-local block coordinates, packed into one int per box.
- Depends on: nothing.

#### OcclusionPass
- Path: `src/main/java/com/aryston/aervium/core/culling/OcclusionPass.java`
- Role: One culling pass over a list of sections: orders them near to far by projected depth, tests each section box (grown by one block for geometry that reaches out of its section, plus an optional reach for camera movement) against the raster, and draws the occluder boxes only of sections that turned out visible, until a box budget is spent. `measureReveal` records for every hidden section the largest of the reach steps 1, 2, 4 and 8 blocks at which it still stays hidden (testing the largest step first, so a section deep behind a wall costs one test), and `widen` reveals the hidden sections whose recorded step is below a larger reach and lists them, without any raster test; `sectionTiles` gives the raster tiles a section box covers. Reusable arrays, no allocation once grown.
- Depends on: `DepthRaster`, `OccluderBoxes`.

#### OcclusionPipeline
- Path: `src/main/java/com/aryston/aervium/core/culling/OcclusionPipeline.java`
- Role: Runs an `OcclusionPass` (the camera pass, or one Helion shadow cascade with a reach of 0) on a worker one frame behind the render thread. Two `Frame`s (pass, raster, clip matrix, eye position, reach and the opaque target object of each entry) alternate: the render thread fills a free frame and submits it, the worker rasterizes and tests it, and the render thread takes the finished frame at its next call of `activateFinished` while the next one is in flight. `begin` returns nothing while a frame is in flight, so the render thread never waits. `Frame.covers` says whether a later eye position is within the frame's reach; `Frame.isVisible` tests a world box grown by the reach. `Frame.widen` raises the reach of a finished frame for an eye that moved past it (up to `MAX_REVEAL_REACH`, 8 blocks): it reveals every hidden section whose reveal step, measured on the worker after the pass (`measureReveal`), lies below the new reach, and `Frame.revealed` lists them, so widening can only reveal sections and costs the render thread no raster test. Before 2026-10-16 the render thread tested every hidden section again, which was 11 % of it in the cave scene, where 16 % of the frames widen. `Frame.sameOccluders`, `beginChanges`, `markChanged` and `hiddenDespiteChanges` let a shadow cascade reuse a result after its section list changed: the tiles of every occluder that left the list or changed are marked in a `TileMask`, and a hidden section stays hidden only when none of its tiles is marked. `clear` drops the active frame and the result in flight.
- Notes: A frame's data belongs to the worker between `submit` and `finished`; the volatile `finished` flag publishes the result.
- Depends on: `OcclusionPass`, `DepthRaster`, `ClipMatrix`, `OccluderBoxes`, `TileMask`.

#### CullingYield
- Path: `src/main/java/com/aryston/aervium/core/culling/CullingYield.java`
- Role: Decides when a culling pass is worth its cost. `result(tested, hidden, now)` counts results that hid less than the useful share; after a run of them it pauses the pass for a while and returns true; after the pause one more low result pauses again, one useful result keeps the pass running. `paused(now)`, `reset()`.
- Depends on: nothing.

#### CullingStatistics
- Path: `src/main/java/com/aryston/aervium/core/culling/CullingStatistics.java`
- Role: Sections and hidden sections of the last camera pass and the last set of shadow cascades, and tested and hidden entities and particles of the last frame, for the overlay, the worker time of the last camera pass, the share of hidden sections since the last call of `takeCameraHiddenPercent`, the share of hidden shadow casters since the last call of `takeShadowHiddenPercent`, and the share of solid terrain quads face culling skipped since the last call of `takeFaceSkippedPercent`, for the benchmark log. `cameraResult` counts per frame whether the camera result was current, widened, missing, too old or out of reach (`CameraResult`, `takeCameraResults`).
- Depends on: nothing.

#### CameraResult, CameraResultCounts
- Path: `src/main/java/com/aryston/aervium/core/culling/CameraResult.java`, `src/main/java/com/aryston/aervium/core/culling/CameraResultCounts.java`
- Role: What a frame's camera culling could use (the current result, a widened older result, none, a result more than 6 frames and 60 ms old, or one whose eye is beyond 8 blocks) and the counts per scene for the benchmark log; every missing result means every section is drawn that frame.
- Depends on: nothing.

#### TileMask
- Path: `src/main/java/com/aryston/aervium/core/culling/TileMask.java`
- Role: One flag per 8 x 8 raster tile, reused between frames: marks the tile ranges of changed shadow occluders and answers whether a range touches a marked tile (or everything is marked).
- Depends on: nothing.

### `com.aryston.aervium.core.mesh`

#### CompactVertexEncoder
- Path: `src/main/java/com/aryston/aervium/core/mesh/CompactVertexEncoder.java`
- Role: Rewrites a finished chunk layer mesh in place from the vanilla 28-byte block vertex into the 16-byte compact vertex, front to back so no unread vertex is overwritten: position as three unsigned shorts in 1/2048 block steps over -8 to +24 blocks of the section with block light in the low and sky light in the high byte of a fourth short, the color unchanged, texture coordinates as two unsigned shorts over 65536. Texture coordinates that are not on the 1/65536 grid round toward the inside of their quad, so a face never samples the neighbouring sprite. Positions outside the range are moved to its edge and counted.
- Notes: Per-thread instance (`encodeInPlace`), runs on the chunk build workers. Vanilla models reach at most one block outside their block, so the range is never left by vanilla content; coordinates on the 1/16 model grid and on a power-of-two atlas are exact.
- Depends on: nothing.

#### FacingSort
- Path: `src/main/java/com/aryston/aervium/core/mesh/FacingSort.java`
- Role: Reorders the quads of a finished chunk layer mesh in place into seven groups: quads whose plane is not axis aligned first, then east, west, up, down, south and north facing quads. A quad's facing is the normal of its diagonals in vertex order (counter-clockwise seen from the front, the order Minecraft's baked faces use and the GPU culls by); all four corners must lie on one plane. Records per group the plane nearest the far side (lowest plane for faces toward positive axes, highest for negative) and returns a `FaceRanges`.
- Notes: Counting sort through a per-thread scratch array; runs on the chunk build workers before upload. Reads the position as three floats at the start of each vertex in native byte order.
- Depends on: `FaceRanges`.

#### FaceRanges
- Path: `src/main/java/com/aryston/aervium/core/mesh/FaceRanges.java`
- Role: The first quad of each facing group of one layer mesh and its plane bound. Stored as one packed `int[]` (`PACKED_INTS`: first quads, then the planes as float bits); `pack` copies it into a draw record and the static methods read it there. `visibleBuckets` gives the groups that can face an eye at a section-relative position: a group is skipped only when the eye is at least half a block behind every plane in it (the margin covers view bobbing, which moves the real eye slightly).
- Depends on: nothing.

### `com.aryston.aervium.core.module`

#### OptimizationModule
- Path: `src/main/java/com/aryston/aervium/core/module/OptimizationModule.java`
- Role: Optimizations that are switched on or off before the game starts, with their key in `config/aervium-modules.properties`, their default and whether they touch the renderer (off when Sodium or Iris is installed).
- Depends on: nothing.

#### ModuleSettings
- Path: `src/main/java/com/aryston/aervium/core/module/ModuleSettings.java`
- Role: Reads `key=true|false` lines (unknown lines are ignored, a missing line takes the module default) and writes every module back so players see all switches.
- Depends on: `OptimizationModule`.

### `com.aryston.aervium.core.collection`

#### ShardedLongMap
- Path: `src/main/java/com/aryston/aervium/core/collection/ShardedLongMap.java`
- Role: Read-only `Long2ObjectOpenHashMap` view over 1024 immutable shards (by mixed key hash); answers `get`, `getOrDefault`, `containsKey`, `size` and `isEmpty`, throws on writes and iteration, `clone()` returns itself.
- Depends on: nothing (fastutil).

#### ShardedLongMapBuilder
- Path: `src/main/java/com/aryston/aervium/core/collection/ShardedLongMapBuilder.java`
- Role: Keeps a mutable mirror of a map in 1024 shards and publishes `ShardedLongMap` snapshots that copy only the shards written since the last snapshot; unchanged shards are shared between snapshots. 1024 shards instead of 128 (2026-10-16): when chunks arrive, a snapshot copies about an eighth as many entries, which showed in the slowest frames of the elytra flight.
- Members: `copyOf(Long2ObjectMap)`, `put(long, V)`, `remove(long)`, `publish()`.
- Depends on: `ShardedLongMap`.

### `com.aryston.aervium.core.job`

#### JobSystem
- Path: `src/main/java/com/aryston/aervium/core/job/JobSystem.java`
- Role: Work-stealing worker pool (`ForkJoinPool` in async mode) with named daemon threads, below normal priority unless a priority is given; `defaultWorkers` leaves two cores to the game threads and caps at eight. `service()` is an `ExecutorService` view whose jobs are counted and timed (`timedJobs`, `busyNanos`).
- Depends on: nothing.

#### RegionLocks
- Path: `src/main/java/com/aryston/aervium/core/job/RegionLocks.java`
- Role: Square regions of grid cells that threads hold exclusively: `acquire` waits until no cell of the region is held by another holder and takes them all at once (so two holders never deadlock), `release` frees them and wakes the waiters.
- Depends on: nothing.

#### JobLoadMeter
- Path: `src/main/java/com/aryston/aervium/core/job/JobLoadMeter.java`
- Role: Jobs per second and the busy share of all workers of a `JobSystem` since the last `take`; each reader (overlay, benchmark) keeps its own meter.
- Depends on: `JobSystem`.

#### MainThreadQueue
- Path: `src/main/java/com/aryston/aervium/core/job/MainThreadQueue.java`
- Role: Results of jobs that must be applied on the main thread; `drain(budget)` runs them in order until the time budget is spent (at least one per call).
- Depends on: nothing.

### `com.aryston.aervium.core.collection` (column index)

#### ColumnIndexedLongMap
- Path: `src/main/java/com/aryston/aervium/core/collection/ColumnIndexedLongMap.java`
- Role: A `Long2ObjectOpenHashMap` of sections that also counts, per column, how many sections are stored and keeps a 64-bit mask of the sections whose value is occupied; kept by `put(long, V)` and `remove(long)`. `complete(column)` says every section is stored, `occupiedMask(column)` which ones hold a value.
- Depends on: nothing; the key layout and the occupied test come in as functions.

### `com.aryston.aervium.core.crash`

#### CrashSummary
- Path: `src/main/java/com/aryston/aervium/core/crash/CrashSummary.java`
- Role: Finds Aervium code in a crash stack (merged mixin handlers by their `aervium$` name, frames in `com.aryston.aervium`) and builds the lines of the crash report entry: version, module states, the code found or "Aervium code in crash: none", one fixed `Aervium module in crash: <key>` line per module and how to test without it.
- Depends on: nothing; the mixin lookup and module map come in as functions.

### `com.aryston.aervium.core.hardware`

#### HardwareClassifier
- Path: `src/main/java/com/aryston/aervium/core/hardware/HardwareClassifier.java`
- Role: Sorts a `HardwareProfile` into `LOW`, `MEDIUM` or `HIGH`; each part (CPU threads, heap, RAM, GPU kind and video memory) can only lower the class, and unknown values never do. Software renderers are low, integrated GPUs at most medium.
- Depends on: `HardwareProfile`, `HardwareTier`, `GpuKind`.

#### HardwareTier
- Path: `src/main/java/com/aryston/aervium/core/hardware/HardwareTier.java`
- Role: What each class sets on first launch: render and simulation distance caps (8/6 low, 12/8 medium, none high) that only lower, and leaf culling on for low.
- Depends on: nothing.

#### HardwareProfile, GpuKind
- Path: `src/main/java/com/aryston/aervium/core/hardware/HardwareProfile.java`, `src/main/java/com/aryston/aervium/core/hardware/GpuKind.java`
- Role: The measured machine (CPU threads, max heap, physical RAM, GPU name, kind and device-local memory) and its log text.
- Depends on: nothing.

### `com.aryston.aervium.core.parity`

#### StateHash
- Path: `src/main/java/com/aryston/aervium/core/parity/StateHash.java`
- Role: 64-bit FNV-1a hash over ints, longs and strings (length-prefixed) for the parity check; `hex()` prints it with 16 digits.
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

#### ParityHarness
- Path: `src/main/java/com/aryston/aervium/world/parity/ParityHarness.java`
- Role: Active only with `-Daervium.parity=true` (the `parity` run). On server start freezes time, weather and mob spawning and force-loads the scenario chunks; after 40 ticks seeds the level random and the random tick position generator, builds `ParityScenario`, runs its light changes every tick, waits for the light worker after every tick, force-loads `aervium.parity.forceRadius` chunks around 0, 0 (0), logs the light worker time before and during the scenario, logs block, light and entity hashes of the area every 200 ticks and stops the server after `aervium.parity.ticks` (1200).
- Depends on: `ParityScenario`, `StateHash`, `LevelRandomAccessor`.

#### WorldgenCheck
- Path: `src/main/java/com/aryston/aervium/world/parity/WorldgenCheck.java`
- Role: Active with `-Daervium.parity.worldgen=true` instead of the scenario. Freezes ticks, force-loads `aervium.parity.worldgenRadius` (12) chunks around 0, 0, hashes every chunk's blocks and biomes when its terrain step finishes, and once all are full logs the combined terrain hash, missing chunks, a save and load round trip of every section through the real chunk codecs, generation time, process CPU and light worker CPU, then stops the server.
- Depends on: `StateHash`, `ServerLightTimings`, `ChunkStatusTasksTerrainMixin`.

#### ParityLoad
- Path: `src/main/java/com/aryston/aervium/world/parity/ParityLoad.java`
- Role: Optional server load for profiling (`aervium.parity.mobs`): spawns cows, sheep, pigs, chickens and villagers at fixed random spots over the force-loaded square, measures each server tick from `ServerTickEvent.Pre` to `Post` after 100 ticks and logs the mean and longest; also logs the `PoiAudit` summary.
- Depends on: `PoiAudit`.

#### ParityScenario
- Path: `src/main/java/com/aryston/aervium/world/parity/ParityScenario.java`
- Role: Builds the fixed parity scenario on the flat ground: observer clock with redstone, lamps and a sticky piston, hopper chain between chests with cobblestone, furnace with raw iron and coal, 9 x 9 wheat farm around water, water and lava in a stone basin, saplings, sugar cane and torches. `changeLight` places light sources and blockers in the light lab with a fixed sequence per tick. Also defines the hashed area.
- Depends on: nothing.

### `com.aryston.aervium.world.light`

#### ServerLightTimings
- Path: `src/main/java/com/aryston/aervium/world/light/ServerLightTimings.java`
- Role: Thread-safe totals of the server light worker batches: count, wall time, longest batch and CPU time; `take()` returns and resets them.
- Depends on: `StallMeter`.

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

#### SodiumClash, LoadingText
- Path: `src/main/java/com/aryston/aervium/integration/SodiumClash.java`, `src/main/java/com/aryston/aervium/integration/LoadingText.java`
- Role: `SodiumClash` runs first in the mod constructor and throws a NeoForge `ModLoadingException` with one error issue when Sodium is loaded, unless `-Daervium.benchmark=true` is set. `LoadingText` resolves the message from the mod's own language file for the language in `options.txt` (falling back to English), because the loading screen only knows FML's own translations, and doubles apostrophes for the message format.
- Depends on: nothing inside the mod.

#### CompatibilityGuard
- Path: `src/main/java/com/aryston/aervium/integration/CompatibilityGuard.java`
- Role: Lists installed renderer mods (Sodium, which only loads next to Aervium in the benchmark, and Iris) that turn Aervium's optimizations off; `REPLACED_RENDERERS` is shared with `MixinModules`.
- Depends on: nothing inside the mod.

#### MixinModules
- Path: `src/main/java/com/aryston/aervium/integration/MixinModules.java`
- Role: Mixin config plugin: reads and rewrites `config/aervium-modules.properties` before any mixin applies, skips the mixins of switched-off modules, skips renderer modules when Sodium or Iris is in the mod list, keeps `compactVertices` off when Helion is installed without its `CompactTerrainVertices` class (Helion 0.1) and skips mixins into Helion classes when Helion is not installed. `-Daervium.modules=off` keeps every module off for that start without touching the file.
- Depends on: `ModuleSettings`, `OptimizationModule`, `CompatibilityGuard`.
- Notes: Runs before mods are constructed, so it reads the mod list through `FMLLoader.getCurrent().getLoadingModList()`. Also keeps, for the crash report, the active state of every module (`activeModules`), the module of each mixin (`moduleOf`) and which Aervium mixins were applied to each class with their method names (`mixinsDeclaring`, filled in `postApply`).

#### CrashReportSection
- Path: `src/main/java/com/aryston/aervium/integration/crash/CrashReportSection.java`
- Role: Text of the "Aervium" System Details entry: collects the frames of the exception and its causes and runs `CrashSummary` with the mod version and `MixinModules` lookups.
- Depends on: `CrashSummary`, `MixinModules`.

### `com.aryston.aervium.config`

#### AerviumConfig
- Path: `src/main/java/com/aryston/aervium/config/AerviumConfig.java`
- Role: Client config: overlay visibility (saved when F7 toggles it), the target frame rate of the frame budget, dynamic frame rate (on by default, 30 FPS in the background, 60 FPS while paused), occlusion culling (on) and leaf culling (off).
- Depends on: `FrameBudget`, `FrameRatePolicy`.

### `com.aryston.aervium.client`

#### AerviumClient
- Path: `src/main/java/com/aryston/aervium/client/AerviumClient.java`
- Role: Client entry point (`@Mod` with `Dist.CLIENT`): registers the client config, the NeoForge config screen and the client events.
- Depends on: `AerviumConfig`, `ClientEvents`.

#### ClientEvents
- Path: `src/main/java/com/aryston/aervium/client/ClientEvents.java`
- Role: Client adapter: frame start and end (`RenderFrameEvent`), client tick scopes, the F7 key, the overlay layer, the end of every frame for `DirectDraws` (`FlipFrameEvent`), config loading (overlay, budget, dynamic frame rate), Sodium and Iris check, installing the shader caches and the pipeline warm-up (only without Sodium and Iris), the startup report, world joins and shutdown (saves the pipeline cache before the job system stops).
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
- Role: GUI layer in the top right corner: text lines and a 240 frame time graph (0 to 50 ms) with the budget line; bars are green, yellow over budget and red over twice the budget. Reads frames into a preallocated array and submits all bars as one `FrameGraphRenderState`, because vanilla checks every new GUI element against the elements before it, which made 240 single bars cost about 7 % of the render thread. The visual order of the text lines is built when a snapshot arrives, not every frame: the bidirectional reordering of the plain strings was about 5 % of the render thread with the overlay open.
- Depends on: `ClientTelemetry`, `OverlayLines`, `FrameBudget`, `SystemTimings`, `FrameGraphRenderState`.

#### FrameTrace
- Path: `src/main/java/com/aryston/aervium/client/FrameTrace.java`
- Role: Collects what happened in the current frame from the timing mixins and commits it as a `FrameTraceEvent` when the next frame starts; does nothing while the event is disabled.
- Depends on: `FrameTraceEvent`, `CameraResult`.

#### FrameGraphRenderState
- Path: `src/main/java/com/aryston/aervium/client/FrameGraphRenderState.java`
- Role: One GUI element with the quads of every bar of the overlay graph (x, top, bottom and color per bar) on the plain GUI pipeline.
- Depends on: nothing.

#### OverlayLines
- Path: `src/main/java/com/aryston/aervium/client/OverlayLines.java`
- Role: Turns a `TelemetrySnapshot` into colored text lines; rebuilt only when a snapshot arrives. Shows culling counts and, with module `meshWorkers`, chunk builds per second, busy workers and queued builds.
- Depends on: `TelemetrySnapshot`, `ByteSizes`, `CullingStatistics`, `MeshWorkers`.

#### AerviumKeys
- Path: `src/main/java/com/aryston/aervium/client/AerviumKeys.java`
- Role: Key binding category and the overlay toggle (F7).
- Depends on: `Aervium`.

#### HardwareSetup
- Path: `src/main/java/com/aryston/aervium/client/HardwareSetup.java`
- Role: When the game has loaded, measures the machine (device type from `DeviceInfo`, video memory from `VulkanMemoryProbe`, RAM from the OS bean) and logs its class. On Aervium's first launch only (no `config/aervium-hardware.properties` yet, and not a benchmark run) applies the class: lowers distances in `options.txt`, turns leaf culling on for low, writes the record file, and shows a toast when something changed. Deleting the file detects again.
- Depends on: `HardwareClassifier`, `VulkanMemoryProbe`, `AerviumConfig`, `LeafCulling`.

#### VulkanMemoryProbe
- Path: `src/main/java/com/aryston/aervium/client/VulkanMemoryProbe.java`
- Role: Reads used and budgeted device-local memory from the game's Vulkan memory allocator (`vmaGetHeapBudgets`), and the total size of the device-local heaps for the hardware class.
- Depends on: `FrontendGpuDeviceAccessor`, `VramUsage`.

### `com.aryston.aervium.client.chunk`

#### SectionSliceHolder
- Path: `src/main/java/com/aryston/aervium/client/chunk/SectionSliceHolder.java`
- Role: Interface `CompiledSectionMeshMixin` adds to every compiled chunk section mesh: one cached terrain buffer slice per layer, or `ABSENT` when the mesh has no buffer for that layer.
- Depends on: nothing.

#### SectionDrawRecord, LayerDraw
- Path: `src/main/java/com/aryston/aervium/client/chunk/SectionDrawRecord.java`, `src/main/java/com/aryston/aervium/client/chunk/LayerDraw.java`
- Role: Everything `extractSectionDrawGroups` computes for one section mesh, prepared once. The numbers the draw list reads every frame (layer, group hash, index count, first index, base vertex, flags and the packed `FaceRanges` of each drawable layer, up to three slots in layer order) and the section origin sit in one `int[]` whose header also holds the mesh version, a valid flag, the section origin and the slot count. `RenderSectionMixin` keeps that array on the section next to the record, so a frame reads the section and one array instead of up to ten objects; the `LayerDraw` objects with the buffers are only read when a draw group is created.
- Notes: Valid while the section's mesh version (`RenderSectionMeshVersionMixin`) is the one it was built for and no allocation of that mesh was freed; `CompiledSectionMeshMixin` invalidates it (clears the valid flag in the array) together with the cached slices. Moving the header into the array raised the 1% low of the redstone scene by 3 to 4 % (2026-10-16). A mesh's draws never change while it is shown (they are filled in its constructor and cleared only when it is released).

#### BlockEntitySectionHolder
- Path: `src/main/java/com/aryston/aervium/client/chunk/BlockEntitySectionHolder.java`
- Role: Interface `RenderSectionBlockEntityMixin` adds to `SectionRenderDispatcher.RenderSection`: whether the section's mesh may hold renderable block entities.
- Depends on: nothing.

#### SectionDrawRecordHolder
- Path: `src/main/java/com/aryston/aervium/client/chunk/SectionDrawRecordHolder.java`
- Role: Interface `RenderSectionMixin` adds to `SectionRenderDispatcher.RenderSection` to keep its last `SectionDrawRecord` and that record's packed array.
- Depends on: `SectionDrawRecord`.

#### SectionDrawLists
- Path: `src/main/java/com/aryston/aervium/client/chunk/SectionDrawLists.java`
- Role: Same result as vanilla `LevelRenderer.extractSectionDrawGroups` (section infos, draw groups by buffer hash, translucent order, largest index count) built from the section records. Every draw group gets a pooled `PackedDraws` list, and in a multi-draw pass the section infos go into `DirectDraws` as plain numbers instead of records, so a frame allocates no object per section or draw. In the camera pass (`FaceCulling.cameraPass`) a layer with `FaceRanges` gets one draw per run of facing groups that can face the camera instead of one draw for the whole layer; empty groups join runs, so a section needs at most four draws per layer. Counts drawn and skipped quads for `CullingStatistics`.
- Depends on: `FaceCulling`, `FaceRanges`, `CullingStatistics`, `SectionDrawRecord`, `SectionDrawRecordHolder`, `SectionSliceHolder`, `TerrainOcclusion` (skips sections the last finished camera pass hid); the access transformer for `LevelRenderer$ChunkDrawGroup`.
- Notes: In the cave scene it cut the share of the render thread spent building draw lists from about 42 % to 28 %; the frame rate did not change because that scene waits on the GPU (`VulkanCommandEncoder.awaitSubmitCompletion`), so the gain shows on CPU-bound PCs.

#### PackedDraws, PackedDrawPool
- Path: `src/main/java/com/aryston/aervium/client/chunk/PackedDraws.java`, `src/main/java/com/aryston/aervium/client/chunk/PackedDrawPool.java`
- Role: `PackedDraws` is the draw list of one draw group: a `List<IndexedDraw>` that stores the five numbers of every indirect draw command in one `int[]`, copies them into mapped memory in one block (or backwards for the reversed view vanilla asks for translucent layers) and builds `IndexedDraw` records only when vanilla code reads an element (the path without multi-draw). `PackedDrawPool` hands out the lists and reuses them in the next pass, because vanilla reads a group's list only inside the `prepareChunkRenders` call that built it.
- Depends on: nothing.

#### PackedSectionInfos, SectionInfoStorage, DrawCommandRing
- Path: `src/main/java/com/aryston/aervium/client/chunk/PackedSectionInfos.java`, `src/main/java/com/aryston/aervium/client/chunk/SectionInfoStorage.java`, `src/main/java/com/aryston/aervium/client/chunk/DrawCommandRing.java`
- Role: `PackedSectionInfos` holds the origin and fade of every drawn section of a pass as four ints. `SectionInfoStorage` uploads them like vanilla's instanced section buffer (one device buffer, a staging copy per pass, `writeToBuffer`). `DrawCommandRing` writes the indirect commands into a mapped ring of three buffers like vanilla's command buffer. Both grow by doubling, keep a buffer they replaced until the frame ends and start over at `FlipFrameEvent`.
- Depends on: `PackedDraws`.

#### GpuSections
- Path: `src/main/java/com/aryston/aervium/client/chunk/GpuSections.java`
- Role: The per-section step of the GPU path inside `SectionDrawLists`: uploads a section's solid and cutout slots to the GPU section table when its mesh version or the table epoch changed, lists the section as visible, and tells the draw list which slots stay on the CPU (none, the translucent ones, or all of them when a slot does not fit the table).
- Depends on: `GpuCulling`, `GpuSectionEntry`, `SectionDrawRecord`, `SectionDrawRecordHolder`, `SectionDrawLists`.

#### DirtySectionMirror, DirtySectionIndex
- Path: `src/main/java/com/aryston/aervium/client/chunk/DirtySectionMirror.java`, `src/main/java/com/aryston/aervium/client/chunk/DirtySectionIndex.java`
- Role: Interfaces the `dirtySections` mixins add to `SectionUpdateTracker.SectionDirtyState` (attach the shared dirty array and the storage index) and to `SectionUpdateTracker` (whether a section node may be dirty, by its storage index).
- Depends on: nothing.

#### DirectDraws
- Path: `src/main/java/com/aryston/aervium/client/chunk/DirectDraws.java`
- Role: Entry point of the module `directDraws`: marks whether the current `prepareChunkRenders` call is a multi-draw pass, hands `SectionDrawLists` its pooled lists and packed section infos, writes them for vanilla's `writeChunkSectionCommands` and `writeChunkSectionsInstanced` calls, and falls back to vanilla for any list that is not packed (for example with `sectionSliceCache` off). `endFrame` runs at `FlipFrameEvent`.
- Depends on: `PackedDraws`, `PackedDrawPool`, `PackedSectionInfos`, `SectionInfoStorage`, `DrawCommandRing`.

#### FaceCulling
- Path: `src/main/java/com/aryston/aervium/client/chunk/FaceCulling.java`
- Role: Sorts every solid and cutout layer of a new section mesh by facing (`FacingSort`) when the layer uses the vanilla block vertex format and the shared quad index buffer, and marks a `prepareChunkRenders` call as the camera pass when its matrix equals the camera's view rotation, so Helion's shadow cascades and other views keep drawing every face.
- Notes: Module `faceCulling`. Needs `sectionSliceCache` too, because the faces are skipped in `SectionDrawLists`. Translucent layers keep vanilla's sorted order. A skipped face is one the GPU would cull as a back face anyway, so the picture is unchanged; the gain is vertex work. Benchmark on lavapipe: 35 % of solid quads skipped in the village and redstone scenes, 49 % in the caves.
- Depends on: `FacingSort`, `FaceRanges`, `SectionLayers`.

#### FaceRangesHolder
- Path: `src/main/java/com/aryston/aervium/client/chunk/FaceRangesHolder.java`
- Role: Interface `CompiledSectionMeshFacingMixin` adds to `CompiledSectionMesh`: the `FaceRanges` of a layer, or none.
- Depends on: `FaceRanges`.

#### LeafCulling
- Path: `src/main/java/com/aryston/aervium/client/chunk/LeafCulling.java`
- Role: Optional leaf culling: a leaf face toward another leaf is skipped when the block behind that leaf is a leaf too, so only faces two or more leaves deep inside a tree are dropped. Switching it rebuilds every chunk through `LevelExtractor.allChanged` on the main thread.
- Notes: Off by default because it changes the look slightly (inner faces show through the gaps of detailed leaves); with fast leaves vanilla already drops every face between leaves. Runs on the chunk build workers.
- Depends on: nothing.

#### ChunkUploadTimings
- Path: `src/main/java/com/aryston/aervium/client/chunk/ChunkUploadTimings.java`
- Role: Times vanilla's terrain buffer upload and how long the render thread waits for the chunk builder lock (workers hold it while they copy a finished mesh into the staging buffer); feeds `SystemTimings` for the overlay and two `StallMeter`s for the benchmark log.
- Notes: Measurement only. It decides whether uploads or the lock need their own work: vanilla only resets its linear staging buffer once every staged mesh is uploaded, so a plain per-frame upload budget would starve it.
- Depends on: `StallMeter`, `SystemTimings`.

#### MeshWorkers
- Path: `src/main/java/com/aryston/aervium/client/chunk/MeshWorkers.java`
- Role: Owns the `Aervium Mesh` job system (two cores left to the game threads, at most eight workers, one priority step below normal) and the `TracingExecutor` vanilla's chunk build dispatcher runs on instead of the shared background pool; gives meters for the overlay and the benchmark.
- Notes: In single player the shared background pool also generates, loads and saves chunks for the integrated server, so chunk builds waited behind world generation while flying; vanilla still picks the nearest section first. At normal priority, eight workers waking together whenever the camera entered a new section kept the graphics driver and the window compositor off the processor, and the next frame waited 15 to 25 ms for a swapchain image; one step below normal took those waits underground from 22 to 28 per scene to 4 to 9 and raised the 0.1% low there from 100 to 128 to 192 to 203 FPS at the same build rate (2026-10-17).
- Depends on: `JobSystem`, `JobLoadMeter`.

#### CompactTerrainFormat
- Path: `src/main/java/com/aryston/aervium/client/chunk/CompactTerrainFormat.java`
- Role: The 16-byte compact terrain vertex format (`PositionLight` RGBA16_UINT, `Color` RGBA8_UNORM, `PackedUV0` RG16_UINT), the shader define contract `AERVIUM_COMPACT_VERTICES=1`, the vanilla pipelines that get it (solid and cutout terrain, direct and multi-draw) and `apply`, which gives a pipeline builder the format, the define and the vertex shader `aervium:terrain/compact`.
- Notes: Module `compactVertices`. Loaded inside the static setup of `RenderPipelines`, so it touches no chunk class (lesson L-087). Translucent terrain keeps the block format, because vanilla sorts translucent quads from their float positions.
- Depends on: nothing of this mod.

#### CompactMeshUpload, CompactMeshHolder
- Path: `src/main/java/com/aryston/aervium/client/chunk/CompactMeshUpload.java`, `src/main/java/com/aryston/aervium/client/chunk/CompactMeshHolder.java`
- Role: `buildFormat` keeps chunk builds in the vanilla block format (so face sorting and NeoForge geometry renderers see vanilla data); `uploadView` encodes a compact layer's mesh once (`CompactMeshHolder` marks the `MeshData`, because vanilla retries the upload while the staging buffer is full) and hands the upload a view of 16 bytes per vertex. Warns once per session when a mesh had vertices outside the compact range.
- Depends on: `CompactTerrainFormat`, `CompactVertexEncoder`.

#### CompactWireframe
- Path: `src/main/java/com/aryston/aervium/client/chunk/CompactWireframe.java`
- Role: Compact copies of the vanilla wireframe debug pipelines, used for the opaque layer group so the wireframe view still reads compact meshes.
- Depends on: `CompactTerrainFormat`.

#### SectionOwnership
- Path: `src/main/java/com/aryston/aervium/client/chunk/SectionOwnership.java`
- Role: Module `lockFreeSections`: the render thread owns the terrain buffer allocations. Meshes that mesh workers release (empty result, cancelled build) wait in a lock-free queue and the render thread releases them right before the next upload; the render thread takes the section dispatcher lock only for the upload, with `tryLock`, and waits for it only after four skipped frames in a row; workers that find the staging buffer full park for 0.2 ms instead of spinning.
- Notes: Workers still stage a whole layer inside one hold of the lock, and an upload needs the same lock, so a mesh the render thread resets can never be published after the upload that clears its skip mark. On 2026-10-16 the longest lock wait of the render thread fell from 19.5 ms to 0 and the 1% low rose by 12 % at render distance 12.
- Depends on: `RenderSectionAccessor`, `ChunkUploadTimings`, `FrameTrace`, `MixinModules`.

#### SectionLayers
- Path: `src/main/java/com/aryston/aervium/client/chunk/SectionLayers.java`
- Role: One shared `ChunkSectionLayer.values()` array.
- Notes: Lives in its own class so the enum is first loaded at the first frame; a static field in a mixin would load it inside the static setup of `LevelRenderer`, before `RenderPipelines` exists, and every layer would get a null pipeline.

### `com.aryston.aervium.client.culling`

#### TerrainOcclusion
- Path: `src/main/java/com/aryston/aervium/client/culling/TerrainOcclusion.java`
- Role: Occlusion culling for terrain and the camera raster that entity culling shares. `prepareCamera` runs once per frame before entities are extracted and never waits: it takes the camera pass the worker finished (`OcclusionPipeline`), stamps every section that pass hid with the pass generation (`CameraOcclusionHolder`), uses the result this frame unless it is both more than 6 frames and more than 60 ms old (so high frame rates do not age results out early) and while the eye is still within its reach (or widens it to the eye, up to 8 blocks, with `Frame.widen` instead of drawing every section), and then copies the visible sections (origin, computed from the section node instead of the origin object, occluder boxes, fully faded in) into the free frame and submits it. The reach is 0.125 blocks plus three times the distance the eye moved since the last frame, at most 4 blocks; test boxes grow by it. The camera raster uses a projection 25 % wider than the screen and treats boxes reaching past its edge as visible, so turning the camera cannot reveal a section the pass hid. `cullsCameraSections` and `hiddenFromCamera` let `SectionDrawLists` skip hidden sections; sections not in the pass are never hidden. The frame counter advances at `RenderFrameEvent.Post`, because vanilla extracts a frame before `RenderFrameEvent.Pre` fires. `beginTerrainPass` marks a `prepareChunkRenders` call as the camera pass when its matrix equals the camera's view rotation (vanilla and Helion both pass a copy of it; Helion's shadow cascades pass the light view). `filterShadowCasters` removes casters the light cannot reach, one orthographic pass per shadow cascade in light space (up to 8 cascades). Each cascade is a `ShadowCascadeCulling` on the `Aervium Shadow Culling` worker, matched by section position so results stay usable while the camera moves.
- Notes: Sections still fading in never occlude. The camera pass and the shadow pass each rest for a second (`CullingYield`) after 20 results in a row hid less than 15 % of what they tested, then probe again; `cameraPaused` and `shadowsPaused` tell the overlay. The camera pass runs on the single `Aervium Culling` worker; the render thread part (copies, stamps, cascade hashes) shows as `Occlusion culling` in the overlay, the worker time as `worker` in the culling line. F6 and `occlusionCulling` in the client config switch it at runtime; switching it off drops the result in flight.
- Depends on: `OcclusionPipeline`, `OcclusionPass`, `DepthRaster`, `ClipMatrix`, `CullingStatistics`, `JobSystem`, `CameraOcclusionHolder`, `OccluderHolder`, `CompiledSectionMeshVisibilityAccessor`, `ShadowCascadeCulling`, `CameraResult`.

#### ShadowCascadeCulling
- Path: `src/main/java/com/aryston/aervium/client/culling/ShadowCascadeCulling.java`
- Role: Occlusion culling of one Helion shadow cascade on the `Aervium Shadow Culling` worker. Per frame it hashes the light direction and the cascade (section, occluder boxes, fade state) and submits a changed cascade; a finished result is matched to the current list by section position (`Long2IntOpenHashMap`), not by list order. While the light direction is unchanged, a caster the result hid is dropped unless it changed itself or an occluder that left the list or changed covers one of its raster tiles, so results stay usable while the camera moves and the cascade gains and loses sections at its edges.
- Notes: Removing a caster is lossless because the shadow map keeps only the nearest depth and the occluders in front of it are still drawn. New sections are never hidden.
- Depends on: `OcclusionPipeline`, `TerrainOcclusion`.

#### EntityOcclusion
- Path: `src/main/java/com/aryston/aervium/client/culling/EntityOcclusion.java`
- Role: Decides whether an entity or a block entity is hidden from the camera by testing its box (entities: interpolated bounding box grown by 1 block; block entities: the renderer's render box grown by half a block) against this frame's camera raster in `TerrainOcclusion`. Counts tested and hidden entities for the overlay.
- Notes: Only mobs, dropped items, experience orbs and armor stands are culled; glowing, named, leashed, riding or ridden entities are always drawn. With Helion loaded an entity under open sky is always drawn, because it may cast a sun shadow onto visible ground.
- Depends on: `TerrainOcclusion`, `CullingAudit`.

#### ParticleOcclusion
- Path: `src/main/java/com/aryston/aervium/client/culling/ParticleOcclusion.java`
- Role: Decides whether a particle is hidden from the camera: the box spans its position last tick and now, grown by one and a half times its quad size plus half a block, and is tested against this frame's camera raster in `TerrainOcclusion`. Counts tested and hidden particles for the overlay.
- Depends on: `TerrainOcclusion`, `CullingAudit`.

#### HelionShadowCasters
- Path: `src/main/java/com/aryston/aervium/client/culling/HelionShadowCasters.java`
- Role: Called by `HelionShadowCastersMixin` with Helion's cascades and caster lists; reads the light view of the first cascade (all cascades share it) through `lightView()` and passes the lists to `TerrainOcclusion`.
- Notes: Aervium does not compile against Helion; if Helion changes that method the hook does nothing and the overlay shows no shadow numbers.
- Depends on: `TerrainOcclusion`.

#### CullingAudit
- Path: `src/main/java/com/aryston/aervium/client/culling/CullingAudit.java`
- Role: Debug check of the camera pass, on with `-Daervium.cullingAudit=true`: every 30 finished camera passes it casts 24 rays from the current camera (not the eye the pass ran at) to random on-screen points of up to 48 hidden sections through the real blocks and counts rays that reach the section box without passing an opaque block (each one would be a wrongly hidden section). Logs totals every 20 checks and the first 20 failures.
- Depends on: `VoxelRay`, `OcclusionPass`, `OcclusionPipeline`, `ClipMatrix`.

#### OccluderHolder
- Path: `src/main/java/com/aryston/aervium/client/culling/OccluderHolder.java`
- Role: Interface `VisibilitySetMixin` adds to `VisibilitySet` to carry the section's `OccluderBoxes` from the mesh build to the compiled mesh.
- Depends on: `OccluderBoxes`.

#### CameraOcclusionHolder
- Path: `src/main/java/com/aryston/aervium/client/culling/CameraOcclusionHolder.java`
- Role: Interface `RenderSectionOcclusionMixin` adds to `SectionRenderDispatcher.RenderSection`: the generation of the last camera pass that hid the section, and its occluder boxes cached for a mesh version (`TerrainOcclusion.occluders(section)` reads mesh and visibility set only when the version changed).
- Depends on: nothing.

### `com.aryston.aervium.client.gpu`

#### GpuCulling
- Path: `src/main/java/com/aryston/aervium/client/gpu/GpuCulling.java`
- Role: The advanced terrain path of module `gpuCulling`. In the camera pass the CPU only lists the visible sections that occlusion culling kept (section index and fade) in a mapped ring; the GPU section table holds each section's solid and cutout draws; a compute shader (`shaders/culling/terrain_draws.csh`) applies face culling against the camera, writes the section instance data and appends the draw commands of each draw group with an atomic counter, and the terrain layers draw them with `vkCmdDrawIndexedIndirectCount` (`ChunkSectionsToRenderIndirectCountMixin`). Translucent layers, Helion's shadow cascades and every other pass keep the CPU draw list. Sets itself up on first use (Vulkan device, `drawIndirectCount` enabled through `IndirectCountFeature`, compiled pipeline) and otherwise stays off with one log line, so devices without the feature keep the CPU path (the baseline path).
- Notes: One GPU pass per frame. The dispatch is recorded into the frame's command buffer when the section infos are written, between full memory barriers (`VulkanCommandEncoder.memoryBarrier`), after the table uploads and a fill of the draw counts. A group gets room for four draws per visible section (face runs), so no draw can be lost.
- Depends on: `GpuSectionTable`, `GpuSectionEntry`, `TerrainGroups`, `CullPipeline`, `GpuDrawList`, `VulkanAccess`, `GpuBufferUsage`, `FrontendRenderPassAccessor`, `VulkanRenderPassAccessor`, `MixinModules`.

#### GpuSectionTable, GpuSectionEntry
- Path: `src/main/java/com/aryston/aervium/client/gpu/GpuSectionTable.java`, `src/main/java/com/aryston/aervium/client/gpu/GpuSectionEntry.java`
- Role: The device buffer with one 36-int entry per render section index (origin, slot count, and per solid or cutout slot: draw group, index count, first index, base vertex, face flag, the eight face run boundaries as 16-bit pairs and the seven face planes). Entries that changed are written into a mapped staging ring during the draw list and copied in one `vkCmdCopyBuffer` before the dispatch, so frames in flight never see a half-updated entry. A larger table starts a new epoch, which marks every section stale.
- Depends on: `VulkanAccess`, `GpuBufferUsage`.

#### TerrainGroups, GpuDrawList
- Path: `src/main/java/com/aryston/aervium/client/gpu/TerrainGroups.java`, `src/main/java/com/aryston/aervium/client/gpu/GpuDrawList.java`
- Role: `TerrainGroups` gives every (layer, vertex buffer, index buffer, index type) a small id that the table stores, drops groups whose buffers were closed and ranks the live ones for the frame. `GpuDrawList` is the draw list vanilla sees for such a group: its size is the group's command capacity and its commands come from the GPU.
- Depends on: nothing.

#### CullPipeline, VulkanAccess, IndirectCountFeature, GpuBufferUsage
- Path: `src/main/java/com/aryston/aervium/client/gpu/CullPipeline.java`, `src/main/java/com/aryston/aervium/client/gpu/VulkanAccess.java`, `src/main/java/com/aryston/aervium/client/gpu/IndirectCountFeature.java`, `src/main/java/com/aryston/aervium/client/gpu/GpuBufferUsage.java`
- Role: `CullPipeline` compiles the compute shader with shaderc and creates the push-descriptor set layout, pipeline layout and compute pipeline, and records one dispatch. `VulkanAccess` reaches the Vulkan device, its enabled features, the frame's command buffer and buffer handles. `IndirectCountFeature` is the optional device feature set `VulkanFeatureSetsMixin` adds. `GpuBufferUsage.STORAGE` is an extra buffer usage bit that `VulkanConstStorageMixin` maps to Vulkan storage buffer usage.
- Depends on: `FrontendGpuDeviceAccessor`, `VulkanDeviceAccessor`, `VulkanCommandEncoderAccessor`.

#### GpuStageTimer, RenderPassTimingHolder
- Path: `src/main/java/com/aryston/aervium/client/gpu/GpuStageTimer.java`, `src/main/java/com/aryston/aervium/client/gpu/RenderPassTimingHolder.java`
- Role: GPU timestamps per named stage (frame graph passes, render passes, main pass segments, the whole frame and the rendering before the swapchain copy) in a timestamp query pool with four frames in flight; results are read without waiting when a slot comes round again and summed in `StageTimes`. Only on with `-Daervium.benchmark.gpuTimings=true`, so normal benchmark reports stay comparable. `RenderPassTimingHolder` lets a render pass carry its open timestamp pair until it closes.
- Depends on: `StageTimes`, `FrameTrace`, `VulkanAccess`, `VulkanQueryPoolAccessor`.

#### MainPassTimings
- Path: `src/main/java/com/aryston/aervium/client/gpu/MainPassTimings.java`
- Role: Splits the main pass into opaque terrain, solid entities and block entities, the classic translucent segments and improved transparency (whole and its terrain part) for `GpuStageTimer`.
- Depends on: `GpuStageTimer`.

#### SwapchainImages
- Path: `src/main/java/com/aryston/aervium/client/gpu/SwapchainImages.java`
- Role: Module `swapchainImages`: asks the swapchain for two more images than vanilla (within the surface's maximum). With vsync off the presentation engine holds images now and then for up to about 25 ms; with only three images the GPU then waits for an image before its swapchain copy, and the render thread waits for that frame. Extra images only add slack; frames are presented exactly as before.
- Depends on: `Aervium`.

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

#### BenchmarkOptions
- Path: `src/main/java/com/aryston/aervium/client/benchmark/BenchmarkOptions.java`
- Role: Before the benchmark world is created, remembers the frame limit, vsync, render and simulation distance, sets an unlimited frame rate (`Options.UNLIMITED_FRAMERATE_CUTOFF`), vsync off and the distances from `-Daervium.benchmark.renderDistance` and `.simulationDistance` (current values when absent), and restores all four when the benchmark ends.
- Depends on: nothing inside the mod.

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

#### FrameGraphBuilderTimingMixin, FrontendCommandEncoderTimingMixin, FrontendRenderPassTimingMixin, LevelRendererMainPassTimingMixin, VulkanQueryPoolAccessor
- Path: `src/main/java/com/aryston/aervium/mixin/FrameGraphBuilderTimingMixin.java`, `src/main/java/com/aryston/aervium/mixin/FrontendCommandEncoderTimingMixin.java`, `src/main/java/com/aryston/aervium/mixin/FrontendRenderPassTimingMixin.java`, `src/main/java/com/aryston/aervium/mixin/LevelRendererMainPassTimingMixin.java`, `src/main/java/com/aryston/aervium/mixin/VulkanQueryPoolAccessor.java`
- Role: Feed `GpuStageTimer`: a timestamp pair around every frame graph pass, around every render pass from `createRenderPass` to `close` (labeled by the pass), around the main pass segments (`executeSolid`, `executeClassicTransparency`, `executeOit` and its terrain part), and the end of the whole frame at `submit`. The accessor exposes the Vulkan query pool handle for reading results without waiting.
- Notes: Measurement only, always on, idle unless GPU timings are enabled. Renderpearl offers timestamp queries but no per-pass hook.

#### VulkanCommandEncoderTimingMixin, VulkanGpuSurfaceTimingMixin
- Path: `src/main/java/com/aryston/aervium/mixin/VulkanCommandEncoderTimingMixin.java`, `src/main/java/com/aryston/aervium/mixin/VulkanGpuSurfaceTimingMixin.java`
- Role: Time the parts of `VulkanCommandEncoder.submit` (queue submit, waiting for the frame before the previous one, resource destruction) and of the swapchain (`acquireNextTexture`, `present`) for `FrameTrace`, and mark the start of the swapchain copy for `GpuStageTimer`.
- Notes: Measurement only, always on. Found that the worst frames of the benchmark wait for the presentation engine, not for rendering (2026-10-17).

#### VulkanMemoryTraceMixin, VulkanTransientMemoryTraceMixin
- Path: `src/main/java/com/aryston/aervium/mixin/VulkanMemoryTraceMixin.java`, `src/main/java/com/aryston/aervium/mixin/VulkanTransientMemoryTraceMixin.java`
- Role: Count and time every Vulkan memory allocation and release (`vmaCreateBuffer`, `vmaCreateImage`, `vmaDestroyBuffer`, `vmaDestroyImage`) of buffers, textures and transient memory blocks for `FrameTrace`.
- Notes: Measurement only, always on. The constructors of `VulkanGpuBuffer.Direct` and `VulkanGpuTexture` allocate before they call `super`, so the handlers there are static (lesson L-103 of the workspace). Showed that the presentation stalls of 2026-10-17 came with no allocations.

#### VulkanGpuSurfaceImageCountMixin
- Path: `src/main/java/com/aryston/aervium/mixin/VulkanGpuSurfaceImageCountMixin.java`
- Role: Passes the swapchain image count of `VulkanGpuSurface.configure` through `SwapchainImages`.
- Notes: Module `swapchainImages`. Renderpearl fixes the count at three with no setting.

#### ClientLevelLightTimingMixin, ClientPacketListenerChunkTimingMixin
- Path: `src/main/java/com/aryston/aervium/mixin/ClientLevelLightTimingMixin.java`, `src/main/java/com/aryston/aervium/mixin/ClientPacketListenerChunkTimingMixin.java`
- Role: Count client light updates polled and chunk packets handled per frame for `FrameTrace`.
- Notes: Measurement only, always on.

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
- Role: Replaces `LevelRenderer.extractSectionDrawGroups` with `SectionDrawLists` (under the dispatcher lock, as vanilla) when the game has at most three section layers; the redirects below remain for the vanilla path. In that path it answers `getRenderSectionSlice` from the slice cached in the mesh and asks vanilla only on a miss, and replaces the `ChunkSectionLayer.values()` copy for every section with `SectionLayers.ALL`.
- Notes: Module `sectionSliceCache`, renderer module. Vanilla looks up two `HashMap` entries per section and layer in every pass (the camera and each of Helion's shadow cascades) and allocates a new slice record each time; with Helion at render distance 32 that lookup was about 27 % of the render thread. Turning the module on raised the benchmark 1% low from 36.1 to 50.9 FPS and the cave scene from 58 to 78 FPS average (ZGC, Helion and Arkea, 2026-10-12).

#### RotatingSectionStorageEdgeMixin
- Path: `src/main/java/com/aryston/aervium/mixin/RotatingSectionStorageEdgeMixin.java`
- Role: When the camera enters another chunk column, vanilla `RotatingSectionStorage.repositionCenter` walks every node of the view area and of the section update tracker (about 100,000 each at render distance 32) to find the few that moved. The mixin compares only the grid columns: a column's sections move only when its x or z coordinate wraps, so it touches the columns at the leading and trailing edges and leaves the rest alone. The first placement and jumps of a whole grid width or more take the vanilla path.
- Notes: Module `sectionGridMoves`, renderer module. Needs the access transformer entry for `RotatingSectionStorage$Node`. In the slowest frames of the benchmark the vanilla walk was 5 % of the render thread against 0.4 % in normal frames; with this module and 1024 light snapshot shards the 1% low rose by 6 % and the elytra 1% low from 115 to 126 to 132 FPS (2026-10-16).

#### SectionDirtyStateMirrorMixin, SectionUpdateTrackerMirrorMixin, LevelExtractorDirtySectionsMixin
- Path: `src/main/java/com/aryston/aervium/mixin/SectionDirtyStateMirrorMixin.java`, `src/main/java/com/aryston/aervium/mixin/SectionUpdateTrackerMirrorMixin.java`, `src/main/java/com/aryston/aervium/mixin/LevelExtractorDirtySectionsMixin.java`
- Role: Every frame vanilla `LevelExtractor.extract` asks the `SectionUpdateTracker` for the dirty state of every visible section, two scattered objects per section (the storage node and the state). The tracker mixin gives each state its storage index and one shared `boolean[]` when the tracker is created (`DirtySectionMirror`), the state mixin copies `isDirty` into that array after `setDirty`, `setNotDirty` and `setSectionNode`, and the extractor mixin answers clean sections from the array (`DirtySectionIndex`) without touching their state; dirty or unknown sections take the vanilla lookup.
- Notes: Module `dirtySections`, renderer module. In the cave scene the lookup was 17 % of the render thread (thousands of visible sections behind cave walls). The tracker iterates its storage with `forEach`, because the storage's own iterator skips the last value.

#### ChunkSectionsToRenderIndirectCountMixin
- Path: `src/main/java/com/aryston/aervium/mixin/ChunkSectionsToRenderIndirectCountMixin.java`
- Role: Sends every indirect terrain draw of `ChunkSectionsToRender.DrawIndirect.render` through `GpuCulling.drawIndexedIndirect`, which draws GPU-built groups with `vkCmdDrawIndexedIndirectCount` and passes every other draw on unchanged.
- Notes: Module `gpuCulling`. Renderpearl has no counted indirect draw.

#### VulkanFeatureSetsMixin, VulkanConstStorageMixin
- Path: `src/main/java/com/aryston/aervium/mixin/VulkanFeatureSetsMixin.java`, `src/main/java/com/aryston/aervium/mixin/VulkanConstStorageMixin.java`
- Role: Adds `IndirectCountFeature` to the optional device feature sets, so the device enables `drawIndirectCount` where the GPU supports it, and maps `GpuBufferUsage.STORAGE` to Vulkan storage buffer usage.
- Notes: Module `gpuCulling`. Renderpearl enables only the features vanilla uses and has no storage buffer usage.

#### VulkanDeviceAccessor, VulkanCommandEncoderAccessor, VulkanRenderPassAccessor, FrontendRenderPassAccessor
- Path: `src/main/java/com/aryston/aervium/mixin/VulkanDeviceAccessor.java`, `src/main/java/com/aryston/aervium/mixin/VulkanCommandEncoderAccessor.java`, `src/main/java/com/aryston/aervium/mixin/VulkanRenderPassAccessor.java`, `src/main/java/com/aryston/aervium/mixin/FrontendRenderPassAccessor.java`
- Role: Read the enabled device features, the encoder's current command buffer outside render passes, a Vulkan render pass's command buffer and descriptor push, and the backend behind a frontend render pass.
- Notes: Applied always; only `GpuCulling` uses them, and only after its module check.

#### LevelRendererDirectDrawMixin
- Path: `src/main/java/com/aryston/aervium/mixin/LevelRendererDirectDrawMixin.java`
- Role: Tells `DirectDraws` at the start of `prepareChunkRendersIndirect` and `prepareChunkRenders` which kind of pass runs, and redirects the two writes of `prepareChunkRendersIndirect` (indirect commands, instanced section infos) to it.
- Notes: Module `directDraws`, renderer module. Vanilla's writes take one record per draw and per section; the draw list built them by the ten thousand per frame.

#### RenderSectionBlockEntityMixin, LevelExtractorBlockEntitiesMixin
- Path: `src/main/java/com/aryston/aervium/mixin/RenderSectionBlockEntityMixin.java`, `src/main/java/com/aryston/aervium/mixin/LevelExtractorBlockEntitiesMixin.java`
- Role: `RenderSectionBlockEntityMixin` keeps a flag on each section: set before `setSectionMesh` swaps in a mesh with renderable block entities, set to the new mesh's state right after the swap and cleared after `reset`, so it is never false while the current mesh has block entities. `LevelExtractorBlockEntitiesMixin` wraps the `getSectionMesh` call of `LevelExtractor.extractVisibleBlockEntities` and hands vanilla the shared empty mesh for flagged-off sections, so the frame no longer reads the mesh of every visible section to find the few with chests, signs or beds.
- Notes: Module `blockEntitySections`. No event exposes the block entity scan.

#### RenderSectionMixin
- Path: `src/main/java/com/aryston/aervium/mixin/RenderSectionMixin.java`
- Role: Implements `SectionDrawRecordHolder` on `SectionRenderDispatcher.RenderSection`.
- Notes: Module `sectionSliceCache`.

#### RenderSectionMeshVersionMixin, MeshVersionHolder
- Path: `src/main/java/com/aryston/aervium/mixin/RenderSectionMeshVersionMixin.java`, `src/main/java/com/aryston/aervium/client/chunk/MeshVersionHolder.java`
- Role: Gives each section a new mesh version from `SectionDrawRecord.nextMeshVersion` right after `setSectionMesh` and `reset` swap its mesh, so the draw list and the occlusion snapshots check a field of the section instead of reading the mesh through its `AtomicReference` every frame. Versions are unique, so a lost write between two threads never matches an older cached value, and a reader that reads the version before the mesh never keeps a stale mesh.
- Notes: Belongs to no module and always applies, because draw records (`sectionSliceCache`) and cached occluders (`occlusionCulling`) both depend on it.

#### RenderSectionOcclusionMixin
- Path: `src/main/java/com/aryston/aervium/mixin/RenderSectionOcclusionMixin.java`
- Role: Implements `CameraOcclusionHolder` on `SectionRenderDispatcher.RenderSection`: the generation that hid the section and the section's occluder boxes cached for one mesh version.
- Notes: Module `occlusionCulling`.

#### CompiledSectionMeshMixin
- Path: `src/main/java/com/aryston/aervium/mixin/CompiledSectionMeshMixin.java`
- Role: Implements `SectionSliceHolder` on `CompiledSectionMesh` with one field per vanilla layer (an array only for layers beyond three), so a lookup reads the mesh object the draw loop has just touched.
- Notes: Module `sectionSliceCache`.

#### UberGpuBufferMixin
- Path: `src/main/java/com/aryston/aervium/mixin/UberGpuBufferMixin.java`
- Role: Clears a mesh's cached slices whenever `UberGpuBuffer` frees its allocation and clears every cached slice when the buffer closes.
- Notes: Module `sectionSliceCache`. Every new allocation of a mesh is preceded by `freeAllocation` of that mesh in `uploadStagedAllocations`, and allocations never move inside a heap, so a cached slice (or `ABSENT`) is always what vanilla would return.

#### VisGraphMixin
- Path: `src/main/java/com/aryston/aervium/mixin/VisGraphMixin.java`
- Role: At the start of `VisGraph.resolve` (before its flood fill overwrites the bits) turns the opaque block bits into `OccluderBoxes`, and attaches them to the returned `VisibilitySet`.
- Notes: Module `occlusionCulling`. Runs on the mesh build threads; no API exposes the opaque blocks of a section.

#### VisibilitySetMixin
- Path: `src/main/java/com/aryston/aervium/mixin/VisibilitySetMixin.java`
- Role: Implements `OccluderHolder` on `VisibilitySet`.
- Notes: Module `occlusionCulling`.

#### CompiledSectionMeshVisibilityAccessor
- Path: `src/main/java/com/aryston/aervium/mixin/CompiledSectionMeshVisibilityAccessor.java`
- Role: Accessor for the private `visibilitySet` of `CompiledSectionMesh`.
- Notes: Module `occlusionCulling`.

#### LevelRendererOcclusionMixin
- Path: `src/main/java/com/aryston/aervium/mixin/LevelRendererOcclusionMixin.java`
- Role: Around `prepareChunkRenders` and `prepareChunkRendersIndirect` tells `TerrainOcclusion` which matrix the pass uses and the section fade-in time.
- Notes: Module `occlusionCulling`. Camera culling itself runs in `SectionDrawLists`, so it also needs module `sectionSliceCache`.

#### LevelExtractorOcclusionMixin
- Path: `src/main/java/com/aryston/aervium/mixin/LevelExtractorOcclusionMixin.java`
- Role: At the start of `extractVisibleEntities` takes the finished camera pass and submits the next one through `TerrainOcclusion.prepareCamera`; drops entities from `isEntityVisible` and block entities from `extractVisibleBlockEntities` that `EntityOcclusion` finds hidden.
- Notes: Module `occlusionCulling`. Globally rendered block entities (drawn regardless of where the camera looks, like beacon beams) are never dropped.

#### QuadParticleGroupOcclusionMixin
- Path: `src/main/java/com/aryston/aervium/mixin/QuadParticleGroupOcclusionMixin.java`
- Role: Wraps the frustum check in `QuadParticleGroup.extractRenderState`; a particle inside the frustum that `ParticleOcclusion` finds hidden is not extracted.
- Notes: Module `occlusionCulling`. Runs after `extractVisibleEntities`, so the frame's camera raster is ready.

#### ParticleAccessor
- Path: `src/main/java/com/aryston/aervium/mixin/ParticleAccessor.java`
- Role: Accessor for the protected last-tick position (`xo`, `yo`, `zo`) of `Particle`.
- Notes: Module `occlusionCulling`.

#### ModelBlockRendererLeafCullingMixin
- Path: `src/main/java/com/aryston/aervium/mixin/ModelBlockRendererLeafCullingMixin.java`
- Role: At the start of `ModelBlockRenderer.shouldRenderFace` (the overload with the level and both positions) returns false for faces `LeafCulling` hides, only while face culling is on.
- Notes: Not tied to a module; the `leafCulling` client setting switches it.

#### LevelRandomAccessor
- Path: `src/main/java/com/aryston/aervium/mixin/LevelRandomAccessor.java`
- Role: Setter for the protected random tick position generator (`randValue`) of `Level`, used by `ParityHarness` to make random ticks repeatable.
- Notes: Not tied to a module; it only adds an accessor.

#### CrashReportMixin
- Path: `src/main/java/com/aryston/aervium/mixin/CrashReportMixin.java`
- Role: Adds the "Aervium" entry from `CrashReportSection` to the system report once per crash report, at the start of `getDetails`.
- Notes: Not tied to a module.

#### ParallelFeatures
- Path: `src/main/java/com/aryston/aervium/world/worldgen/ParallelFeatures.java`
- Role: Runs the feature step of a chunk (trees, ores, plants, structure pieces) on the `Aervium Worldgen` job system instead of vanilla's single `worldgen` executor. Each step holds the 3 x 3 chunks around its chunk in `RegionLocks`, the radius vanilla lets a feature step write (`blockStateWriteRadius(1)`), so two steps that could touch the same chunk never run at once.
- Depends on: `JobSystem`, `RegionLocks`.

#### ChunkStatusTasksFeaturesMixin
- Path: `src/main/java/com/aryston/aervium/mixin/ChunkStatusTasksFeaturesMixin.java`
- Role: Wraps `ChunkStatusTasks.generateFeatures` (`@WrapMethod`) and returns the future of `ParallelFeatures.place`; the vanilla body runs unchanged on the worker.
- Notes: Module `parallelFeatures`, listed under `server` in the mixin configuration so it applies on dedicated servers only. The step is a static method of the generation pyramid and has no event.

#### ChunkStatusTasksTerrainMixin
- Path: `src/main/java/com/aryston/aervium/mixin/ChunkStatusTasksTerrainMixin.java`
- Role: While `WorldgenCheck` records, chains `WorldgenCheck.terrainBuilt` onto the future `ChunkStatusTasks.buildTerrain` returns.
- Notes: Not tied to a module; outside the check it only reads a flag.

#### ThreadedLevelLightEngineTimingMixin
- Path: `src/main/java/com/aryston/aervium/mixin/ThreadedLevelLightEngineTimingMixin.java`
- Role: Times every `ThreadedLevelLightEngine.runUpdate` batch, wall and CPU, for `ServerLightTimings`.
- Notes: Not tied to a module; two clock reads per batch.

#### SectionRenderDispatcherOwnershipMixin, CompileTaskOwnershipMixin, ResortTransparencyTaskOwnershipMixin, RenderSectionOwnershipMixin, RenderSectionAccessor
- Path: `src/main/java/com/aryston/aervium/mixin/SectionRenderDispatcherOwnershipMixin.java`, `src/main/java/com/aryston/aervium/mixin/CompileTaskOwnershipMixin.java`, `src/main/java/com/aryston/aervium/mixin/ResortTransparencyTaskOwnershipMixin.java`, `src/main/java/com/aryston/aervium/mixin/RenderSectionOwnershipMixin.java`, `src/main/java/com/aryston/aervium/mixin/RenderSectionAccessor.java`
- Role: Apply `SectionOwnership`: the dispatcher's `lock` on the render thread becomes a `tryLock` for the upload, the upload runs only while the lock is held and first releases the retired meshes, `dispose` releases them too; compile tasks on workers retire meshes instead of releasing them under the lock; both task kinds park instead of `Thread.onSpinWait` while the staging buffer is full; `RenderSection.reset` on the render thread skips the lock. The accessor exposes `releaseSectionMesh`.
- Notes: Module `lockFreeSections` (accessor always). Vanilla shares one lock between eight mesh workers and the render thread for the draw list, every section reset on a chunk crossing and the upload; a worker preempted while holding it stalled the render thread for up to 26 ms. No API covers chunk section memory.

#### UberGpuBufferHeapTraceMixin
- Path: `src/main/java/com/aryston/aervium/mixin/UberGpuBufferHeapTraceMixin.java`
- Role: Counts terrain buffer heaps created and destroyed in `UberGpuBuffer.uploadStagedAllocations` for `FrameTrace`.
- Notes: Measurement only, always on. Heaps are allocated inside the upload with no hook.

#### SectionRenderDispatcherTimingMixin
- Path: `src/main/java/com/aryston/aervium/mixin/SectionRenderDispatcherTimingMixin.java`
- Role: Times the lock call in `SectionRenderDispatcher.lock` and the whole of `uploadTerrainBuffersToGpu` for `ChunkUploadTimings`.
- Notes: Not tied to a module, so the numbers exist with every module on or off.

#### SectionStoragePoiIndexMixin
- Path: `src/main/java/com/aryston/aervium/mixin/SectionStoragePoiIndexMixin.java`
- Role: When a `PoiManager` is constructed, swaps its empty section map for a `ColumnIndexedLongMap` from `PoiColumns` (levels taller than 64 sections keep the vanilla map).
- Notes: Module `poiSearch`.

#### PoiManagerColumnSearchMixin
- Path: `src/main/java/com/aryston/aervium/mixin/PoiManagerColumnSearchMixin.java`
- Role: At the head of `PoiManager.getInChunk`, answers from the occupied sections of a fully loaded column; columns not fully in memory fall through to vanilla, which loads them. With `PoiAudit.ENABLED` also runs the vanilla search and compares.
- Notes: Module `poiSearch`. Uses `SectionStorageAccessor` for the map and the level height.

#### CompiledSectionMeshFacingMixin
- Path: `src/main/java/com/aryston/aervium/mixin/CompiledSectionMeshFacingMixin.java`
- Role: At the end of the `CompiledSectionMesh` constructor (on the chunk build worker, before the layers are uploaded) sorts the layers through `FaceCulling.sortLayers` and keeps the ranges; implements `FaceRangesHolder`.
- Notes: Module `faceCulling`, renderer module.

#### LevelRendererFaceCullingMixin
- Path: `src/main/java/com/aryston/aervium/mixin/LevelRendererFaceCullingMixin.java`
- Role: Around `prepareChunkRenders` and `prepareChunkRendersIndirect` tells `FaceCulling` which matrix the pass uses.
- Notes: Module `faceCulling`, renderer module.

#### LevelRendererMeshWorkersMixin
- Path: `src/main/java/com/aryston/aervium/mixin/LevelRendererMeshWorkersMixin.java`
- Role: Replaces the executor argument of `new SectionRenderDispatcher` in `LevelRenderer.invalidateCompiledGeometry` with `MeshWorkers.executor()`.
- Notes: Module `meshWorkers`, renderer module.

#### CollidableEntities, CollisionAudit
- Path: `src/main/java/com/aryston/aervium/world/collision/CollidableEntities.java`, `src/main/java/com/aryston/aervium/world/collision/CollisionAudit.java`
- Role: `CollidableEntities` tells per class (cached in a `ClassValue`) whether an entity class overrides `canBeCollidedWith` below `Entity` (it may be bumped into) and whether a source overrides `canCollideWith` (it has its own rule); defines the per-section counter interface. `CollisionAudit` (`-Daervium.collisionAudit=true`) counts skipped searches and the ones where vanilla found an entity.
- Depends on: nothing.

#### EntitySectionCollidableMixin
- Path: `src/main/java/com/aryston/aervium/mixin/EntitySectionCollidableMixin.java`
- Role: Counts the entities in an entity section that may be collided with, on `add` and on successful `remove`.
- Notes: Module `entityCollisions`.

#### ServerLevelEntityCollisionsMixin
- Path: `src/main/java/com/aryston/aervium/mixin/ServerLevelEntityCollisionsMixin.java`
- Role: Overrides `getEntityCollisions` in `ServerLevel`: for a source with the default collision rule, when no section in the range vanilla would search counts a collidable entity and the level has no part entities, returns the empty list; otherwise runs the vanilla body. Uses `PersistentEntitySectionManagerAccessor` for the section storage.
- Notes: Module `entityCollisions`.

#### SectionCounts
- Path: `src/main/java/com/aryston/aervium/world/entity/SectionCounts.java`
- Role: Walks the entity sections an entity search in a box would visit (same range as `EntitySectionStorage.forEachAccessibleNonEmptySection`) and answers whether a per-section count is zero in all of them. Used by `entityCollisions` and `hopperSearch`.
- Depends on: nothing.

#### HopperEntities, HopperAudit
- Path: `src/main/java/com/aryston/aervium/world/hopper/HopperEntities.java`, `src/main/java/com/aryston/aervium/world/hopper/HopperAudit.java`
- Role: `HopperEntities` tells which entities count as items and which may hold items for a hopper (`Container` entities and entity types with a NeoForge `Capabilities.Item.ENTITY_AUTOMATION` provider, read through `EntityCapabilityAccessor`), answers whether the sections around a search box hold none (server levels without part entities only), and defines the per-section counter interface. `HopperAudit` (`-Daervium.hopperAudit=true`) counts skippable searches, all searches, and skippable ones where vanilla found an entity.
- Depends on: `SectionCounts`, `ServerLevelEntityManagerAccessor`, `PersistentEntitySectionManagerAccessor`.

#### EntitySectionHopperMixin
- Path: `src/main/java/com/aryston/aervium/mixin/EntitySectionHopperMixin.java`
- Role: Counts item entities and entities that may hold items in an entity section, on `add` and on successful `remove`.
- Notes: Module `hopperSearch`.

#### HopperBlockEntityItemSearchMixin, HopperEntityContainerSearchMixin
- Path: `src/main/java/com/aryston/aervium/mixin/HopperBlockEntityItemSearchMixin.java`, `src/main/java/com/aryston/aervium/mixin/HopperEntityContainerSearchMixin.java`
- Role: At the head of `HopperBlockEntity.getItemsAtAndAbove` and NeoForge `VanillaInventoryCodeHooks.getEntityContainerOrHandler`, return an empty result when the section counts show nothing to find; with the audit on they let vanilla run and check its result at return.
- Notes: Module `hopperSearch`.

#### PacketCompression, CompressionCheck
- Path: `src/main/java/com/aryston/aervium/world/network/PacketCompression.java`, `src/main/java/com/aryston/aervium/world/network/CompressionCheck.java`
- Role: `PacketCompression` creates the deflater of a connection at level 4. `CompressionCheck` runs at the start of the parity run: it sends a 128 KB packet through a real `CompressionEncoder` and `CompressionDecoder` (with the module's mixin applied when it is on) and logs whether the bytes came back identical and the compressed size.
- Depends on: nothing.

#### CompressionEncoderLevelMixin
- Path: `src/main/java/com/aryston/aervium/mixin/CompressionEncoderLevelMixin.java`
- Role: Replaces the `new Deflater()` (level 6) in the `CompressionEncoder` constructor with `PacketCompression.newDeflater()`. Only network bytes change: the client inflates any level, and the decompressed packets are the same.
- Notes: Module `networkCompression`. Vanilla offers no way to choose the compression level.

#### EntityCapabilityAccessor
- Path: `src/main/java/com/aryston/aervium/mixin/EntityCapabilityAccessor.java`
- Role: Accessor for the NeoForge `providers` map of an `EntityCapability`, read by `HopperEntities`.
- Notes: Module `hopperSearch`.

#### ServerLevelEntityManagerAccessor
- Path: `src/main/java/com/aryston/aervium/mixin/ServerLevelEntityManagerAccessor.java`
- Role: Accessor for the private `entityManager` of `ServerLevel`, used by the entity section searches.
- Notes: Modules `entityCollisions` and `hopperSearch`.

#### PersistentEntitySectionManagerAccessor
- Path: `src/main/java/com/aryston/aervium/mixin/PersistentEntitySectionManagerAccessor.java`
- Role: Accessor for the private `sectionStorage` of `PersistentEntitySectionManager`.
- Notes: Modules `entityCollisions` and `hopperSearch`.

#### SectionStorageAccessor
- Path: `src/main/java/com/aryston/aervium/mixin/SectionStorageAccessor.java`
- Role: Accessors for the section map (`storage`) and the level height of a `SectionStorage`, used by `SectionStoragePoiIndexMixin`.
- Notes: Module `poiSearch`.

#### PoiColumns, PoiAudit
- Path: `src/main/java/com/aryston/aervium/world/poi/PoiColumns.java`, `src/main/java/com/aryston/aervium/world/poi/PoiAudit.java`
- Role: `PoiColumns` builds the column index for a level height (section key to chunk column and section index) and streams the records of a column's occupied sections from the lowest up, or returns null when the column is not fully loaded. `PoiAudit` (`-Daervium.poiAudit=true`) counts searches, records and searches whose indexed and vanilla records differ.
- Depends on: `ColumnIndexedLongMap`.

#### HelionShadowCastersMixin
- Path: `src/main/java/com/aryston/aervium/mixin/HelionShadowCastersMixin.java`
- Role: Pseudo mixin on Helion's `VanillaShadowCasterSource.collectSections`; filters the returned per-cascade caster lists with `HelionShadowCasters`.
- Notes: Module `occlusionCulling`. Optional (`require = 0`): without Helion nothing is applied. Lossless: a caster the light cannot see is behind other casters in the same cascade, which already write a nearer depth into that shadow map.

#### RenderPipelineBuilderCompactMixin
- Path: `src/main/java/com/aryston/aervium/mixin/RenderPipelineBuilderCompactMixin.java`
- Role: At `RenderPipeline.Builder.build`, gives the vanilla solid and cutout terrain pipelines the compact vertex format, define and vertex shader (`CompactTerrainFormat.apply`).
- Notes: Module `compactVertices`. No API: the chunk buffers take their vertex size from `ChunkSectionLayer.pipeline(false)`, a static vanilla pipeline built in the static setup of `RenderPipelines`; NeoForge pipeline modifiers only swap pipelines at draw time, after the buffers were sized. Helion's geometry pipelines inherit the change through `toBuilder()`.

#### SectionCompilerCompactMixin
- Path: `src/main/java/com/aryston/aervium/mixin/SectionCompilerCompactMixin.java`
- Role: In `SectionCompiler.getOrBeginLayer`, builds compact layers in the vanilla block format (`CompactMeshUpload.buildFormat`), because the vertex consumers only write the block format.
- Notes: Module `compactVertices`. No API for the builder format of a chunk layer.

#### CompileTaskCompactMixin
- Path: `src/main/java/com/aryston/aervium/mixin/CompileTaskCompactMixin.java`
- Role: Wraps the upload of each layer in `SectionRenderDispatcher.RenderSection.CompileTask.doTask` and hands it the compact view of the mesh (`CompactMeshUpload.uploadView`).
- Notes: Module `compactVertices`. Runs after `CompiledSectionMesh` was created, so face culling has sorted the float positions already. No API between the mesh build and the upload.

#### MeshDataCompactMixin
- Path: `src/main/java/com/aryston/aervium/mixin/MeshDataCompactMixin.java`
- Role: Adds `CompactMeshHolder` to `MeshData`: whether its vertices were encoded already.
- Notes: Module `compactVertices`.

#### ChunkSectionsToRenderCompactMixin
- Path: `src/main/java/com/aryston/aervium/mixin/ChunkSectionsToRenderCompactMixin.java`
- Role: In `ChunkSectionsToRender.renderGroup`, replaces the wireframe debug pipelines of the opaque group with `CompactWireframe`.
- Notes: Module `compactVertices`. The override pipelines are chosen inside `renderGroup` with no hook.

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
| `src/test/java/com/aryston/aervium/core/telemetry/FrameTimeSummaryTest.java` | Steady frames give equal average and lows; spikes lower the 1% and 0.1% lows and the average as computed by hand. Frames over 33 and 50 ms are counted at the exact thresholds. |
| `src/test/java/com/aryston/aervium/core/telemetry/TickSummaryTest.java` | Histogram buckets and their edges, ticks over 50 ms and their share, median. |
| `src/test/java/com/aryston/aervium/core/telemetry/FrameTimeRecorderTest.java` | Frame times are the gaps between frame starts; a reset drops the gap. |
| `src/test/java/com/aryston/aervium/core/culling/DepthRasterTest.java` | Walls hide boxes behind them but never boxes in front, partly covered boxes, gaps, a section never hides itself, near plane clipping, boxes behind the camera, orthographic light space, and 200 random scenes checked against ray casting: nothing visible is ever hidden. |
| `src/test/java/com/aryston/aervium/core/culling/OpaqueMaskTest.java` | Empty and sparse sections give no boxes, a solid section is one box, a cave pocket splits into a few boxes with the exact volume, boxes only ever cover opaque blocks. |
| `src/test/java/com/aryston/aervium/core/culling/VoxelRayTest.java` | Rays through open air, blocked by an opaque block, start block ignored, diagonal rays visit every crossed block, box entry parameter. |
| `src/test/java/com/aryston/aervium/core/mesh/FacingSortTest.java` | Quads are grouped by facing with their bytes intact and slanted quads first; the winding of Minecraft's up face counts as up; groups behind the eye by more than the margin are skipped and both sides stay when the eye is between the planes. |
| `src/test/java/com/aryston/aervium/core/mesh/FaceRangesTest.java` | Packed face ranges at an offset give the same first quads, quad counts and visible groups as the object for eyes on every side; each side of the eye shows only the faces turned to it. |
| `src/test/java/com/aryston/aervium/core/culling/OcclusionPipelineTest.java` | A camera pass arrives one frame later and `begin` never blocks while one is in flight; `clear` drops the result in flight; `covers` accepts only eyes within the reach; the worker result equals the same `OcclusionPass` run directly; widening reaches farther eyes and only reveals sections; a changed occluder in front reveals the section behind it while a far one does not; only the same boxes and fade state count as unchanged. |
| `src/test/java/com/aryston/aervium/core/culling/TileMaskTest.java` | Ranges touching a marked tile, marking everything, clearing. |
| `src/test/java/com/aryston/aervium/core/mesh/CompactVertexEncoderTest.java` | Positions, color and texture coordinates on the model grid decode back exactly; block and sky light packing; positions outside the range are clamped and counted; off-grid texture edges round toward the inside of the quad; the far atlas edge stays in range. |
| `src/test/java/com/aryston/aervium/core/culling/CullingYieldTest.java` | Culling that hides enough never pauses, culling that hides little pauses after a run of results and probes again after the pause, one useful probe keeps it running, and a reset ends a pause. |
| `src/test/java/com/aryston/aervium/core/culling/CullingStatisticsTest.java` | The shadow caster share for the benchmark log sums every cascade of a frame and every frame since the last take, and resets when taken; camera result counts per frame since the last take. |
| `src/test/java/com/aryston/aervium/core/culling/OcclusionPassTest.java` | A solid wall of sections hides the sections behind it and none of its own; sections that may not occlude hide nothing; `depthFromW` keeps depth order. |
| `src/test/java/com/aryston/aervium/core/collection/ShardedLongMapTest.java` | Snapshot equals the source, published snapshots never change, random writes against a plain map, read-only behavior. |
| `src/test/java/com/aryston/aervium/core/telemetry/GcAdviceTest.java` | Collector recognition from bean names, advice for G1 and for a small heap. |
| `src/test/java/com/aryston/aervium/core/telemetry/GcPauseRecorderTest.java` | Pauses versus concurrent cycles, totals, pauses after a mark. |
| `src/test/java/com/aryston/aervium/core/budget/FrameBudgetTest.java` | Frame and system budgets, fixed 50 ms tick budget, budget edge, invalid target. |
| `src/test/java/com/aryston/aervium/core/collection/ColumnIndexedLongMapTest.java` | A column is complete only with every section stored; replacing and removing keep counts and masks exact, double removes are harmless; columns over 64 sections are rejected. |
| `src/test/java/com/aryston/aervium/core/hardware/HardwareClassifierTest.java` | A gaming desktop is high, an integrated laptop medium; the weakest part (4 threads, small heap, software renderer, little video memory or RAM) decides; unknown values do not lower; tiers only lower distances. |
| `src/test/java/com/aryston/aervium/core/crash/CrashSummaryTest.java` | No Aervium code gives the "none" line; a mixin handler in the stack names its module with the fixed marker and the switch-off hint; own frames and always-on mixins are listed without a hint. |
| `src/test/java/com/aryston/aervium/core/parity/StateHashTest.java` | Same input same hash, order and every value matter, FNV offset basis for an empty hash. |
| `src/test/java/com/aryston/aervium/core/budget/StallMeterTest.java` | Sum, average and longest stall, reset on take. |
| `src/test/java/com/aryston/aervium/core/budget/SystemTimingsTest.java` | Smoothing, peaks reset on sampling, over budget flags. |
| `src/test/java/com/aryston/aervium/core/job/JobSystemTest.java` | Worker count rule, every job runs on named workers, no jobs after closing. |
| `src/test/java/com/aryston/aervium/core/job/JobLoadMeterTest.java` | Timed service counts jobs and busy time, jobs per second and busy share, empty interval, no jobs after closing. |
| `src/test/java/com/aryston/aervium/core/job/RegionLocksTest.java` | Separate regions are held together, an overlapping region waits for the release, and eight threads taking random overlapping regions never hold the same cell at once. |
| `src/test/java/com/aryston/aervium/core/job/MainThreadQueueTest.java` | Order, time budget, at least one task per drain. |
| `src/test/java/com/aryston/aervium/debug/route/CameraPathTest.java` | Lines move along their yaw; orbits keep their radius and height, look at the center and close after one turn. |
| `src/test/java/com/aryston/aervium/core/cache/ShaderCacheKeyTest.java` | Direct and transitive includes (with a cycle), stable keys for reordered defines, a new key for every change that affects the binary (defines, flags, device switches, include content). |
| `src/test/java/com/aryston/aervium/core/cache/CacheFilesTest.java` | Disk cache round trip without leftover temporary files, existing entries kept, eight concurrent writers and readers of one key, SPIR-V validation, pipeline cache header checks, cache counters. |
| `src/test/java/com/aryston/aervium/core/budget/FrameRatePolicyTest.java` | Active, background and paused limits, never above a lower vanilla limit, disabled policy. |
| `src/test/java/com/aryston/aervium/core/module/ModuleSettingsTest.java` | Every module starts on, switched-off and unknown lines, every module written back. |
| `src/test/java/com/aryston/aervium/debug/benchmark/BenchmarkCommandsTest.java` | Crowd size and distinct positions, build order of the redstone platform, every fill within the vanilla volume limit, frozen time and weather. |
| `src/test/java/com/aryston/aervium/debug/benchmark/SceneMeasurementTest.java` | Only frames after the scene start count, peaks of heap and VRAM, frames lost from a full ring. |
| `src/test/java/com/aryston/aervium/debug/benchmark/BenchmarkReportTest.java` | Comparison percentages, JSON round trip, newest report by start time, missing folder, text report content. |
| `src/test/java/com/aryston/aervium/world/network/PacketCompressionTest.java` | Packets compressed at the faster level inflate to the same bytes and stay within 10 % of the vanilla size. |

## Source Files

| File | Purpose |
|---|---|
| `src/main/templates/META-INF/neoforge.mods.toml` | Mod metadata template filled from `gradle.properties`: NeoForge and Minecraft on both sides, `IGNORE_ALL_VERSION` so either side works alone, discouraged Sodium and Iris, mixin config. |
| `src/main/resources/META-INF/accesstransformer.cfg` | Makes `LevelRenderer$ChunkDrawGroup` and its constructor public for `SectionDrawLists`; declared in `neoforge.mods.toml`. |
| `src/main/resources/aervium.mixins.json` | Mixin configuration with the `MixinModules` plugin: `BlockDataLayerStorageMapMixin`, `ChunkStatusTasksTerrainMixin`, `CompressionEncoderLevelMixin`, `CrashReportMixin`, `DataLayerStorageMapMixin`, `EntityCapabilityAccessor`, `EntitySectionCollidableMixin`, `EntitySectionHopperMixin`, `HopperBlockEntityItemSearchMixin`, `HopperEntityContainerSearchMixin`, `LevelRandomAccessor`, `PalettedContainerMixin`, `PersistentEntitySectionManagerAccessor`, `PoiManagerColumnSearchMixin`, `ServerLevelEntityCollisionsMixin`, `ServerLevelEntityManagerAccessor`, `SectionStorageAccessor`, `SectionStoragePoiIndexMixin`, `SkyDataLayerStorageMapMixin` and `ThreadedLevelLightEngineTimingMixin` on both sides; `ChunkStatusTasksFeaturesMixin` on dedicated servers only (module `parallelFeatures`); `CompiledSectionMeshFacingMixin`, `CompiledSectionMeshMixin`, `CompiledSectionMeshVisibilityAccessor`, `HelionShadowCastersMixin`, `LevelExtractorBlockEntitiesMixin`, `LevelExtractorOcclusionMixin`, `LevelRendererFaceCullingMixin`, `LevelRendererMeshWorkersMixin`, `LevelRendererOcclusionMixin`, `LevelRendererSectionSliceMixin`, `RenderSectionBlockEntityMixin`, `RenderSectionMeshVersionMixin`, `RenderSectionMixin`, `RenderSectionOcclusionMixin`, `SectionRenderDispatcherTimingMixin`, `UberGpuBufferMixin`, `VisGraphMixin`, `VisibilitySetMixin`, `FramerateLimitTrackerMixin`, `ModelBlockRendererLeafCullingMixin`, `FrontendGpuDeviceAccessor`, `GlslCompilerMixin`, `ParticleAccessor`, `PipelineCacheAccessor`, `PipelineCacheMixin`, `QuadParticleGroupOcclusionMixin`, `RenderSystemMixin` `VulkanRenderPipelineMixin`, the frame and GPU timing mixins (`ClientLevelLightTimingMixin`, `ClientPacketListenerChunkTimingMixin`, `FrameGraphBuilderTimingMixin`, `FrontendCommandEncoderTimingMixin`, `FrontendRenderPassTimingMixin`, `LevelRendererMainPassTimingMixin`, `UberGpuBufferHeapTraceMixin`, `VulkanCommandEncoderTimingMixin`, `VulkanGpuSurfaceTimingMixin`, `VulkanMemoryTraceMixin`, `VulkanTransientMemoryTraceMixin`, `VulkanQueryPoolAccessor`), the section ownership mixins (`CompileTaskOwnershipMixin`, `RenderSectionAccessor`, `RenderSectionOwnershipMixin`, `ResortTransparencyTaskOwnershipMixin`, `SectionRenderDispatcherOwnershipMixin`, module `lockFreeSections`) and `VulkanGpuSurfaceImageCountMixin` (module `swapchainImages`) on the client, plus `ChunkSectionsToRenderCompactMixin`, `CompileTaskCompactMixin`, `MeshDataCompactMixin`, `RenderPipelineBuilderCompactMixin` and `SectionCompilerCompactMixin` (module `compactVertices`). |

## Asset Folders

| Folder | Contents |
|---|---|
| `src/main/resources/assets/aervium/lang/` | `en_us.json` and `tr_tr.json`: key binding and config texts. |
| `src/main/resources/assets/aervium/shaders/terrain/` | `compact.vsh`: the vanilla `core/terrain` vertex shader reading the compact vertex format (module `compactVertices`); the fragment shader stays vanilla. |

## Build Files

| File | Purpose |
|---|---|
| `build.gradle` | ModDevGradle setup, JUnit 5 for `src/test/java` with the Minecraft classpath of `main`, `client` run forced to Vulkan, `server` run without GUI, `parity` dedicated server run in `runs/parity` (`setUpParityWorld` writes the flat world settings, or normal terrain with `-PparityWorld=normal`, EULA and module switches from `-PparityModules` and `-PparityOff` and deletes the old world first), metadata expansion, packs `branding/logo.png` into the jar as `aervium.png`, jar name `aervium-neoforge-<minecraft_version>-<version>.jar`. |
| `gradle.properties` | Single place for versions and mod metadata. |
| `settings.gradle` | Plugin repositories, Java toolchain resolver, project name. |
| `.github/workflows/build.yml` | CI on every push and pull request: JDK 25, `./gradlew build`, the vanilla parity check with every module on and off (the hash lines must match), uploads the jar. |
| `.github/workflows/release.yml` | On a `v*` tag: checks the tag against `mod_version`, builds, creates the GitHub release with the changelog section, and uploads to Modrinth and CurseForge when their secrets exist. |
| `.github/workflows/listing.yml` | Syncs the Modrinth project page (README, links, license, optional on both sides, category optimization) on pushes to `main`. |
