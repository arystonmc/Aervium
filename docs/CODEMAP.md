# Kinetic Code Map

Index of the Kinetic code map. Entries live in `docs/codemap/`, one file per Java package plus tests, resources and build files. The big picture is in `docs/ARCHITECTURE.md`.

## How to Find a Class

1. Find the feature in Quick Lookup below to get the class names.
2. Search the entry from the mod folder: `rg -n "^#### .*\bClassName\b" docs/codemap -A12`.
3. Or open one package file from the Packages table.

A class of package `P` is documented only in `codemap/P.md`, or in `codemap/P--<topic>.md` when the package is split. Update only that file in the same commit as the code (rule D1), and split it at a topic boundary once it grows past 24 KB. `scripts/verify.sh` enforces both.

## Quick Lookup

| I want to change | Go to |
|---|---|
| Mod id, logger, startup | `Kinetic` (both sides), `KineticClient` (client) |
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
| Benchmark world, commands and camera | `BenchmarkWorld`, `BenchmarkCommands`, `BoxCommands`, `SceneSetup`, `CrowdSetup`, `StorageSetup`, `ParticleStormSetup`, `RedstoneSetup`, `ExplosionSetup`, `SimulatedPlayerRoutes`, `BenchmarkCamera` |
| Benchmark flow and report | `BenchmarkDriver`, `SceneMeasurement`, `BenchmarkReport`, `BenchmarkText`, `BenchmarkFiles`, `BenchmarkComparison` |
| Refusing to load next to Sodium (loading error in the game language) | `SodiumClash`, `LoadingText`, `Kinetic` |
| Shader and pipeline caches, startup report | `ShaderCaches`, `SpirvCache`, `VulkanPipelineCache`, `ShaderCacheKey`, `DiskCache` |
| Pipelines compiled in the middle of a frame | `PipelineWarmup`, `PipelineCacheMixin` |
| Dynamic frame rate | `FrameRatePolicy`, `FrameRateGovernor`, `FramerateLimitTrackerMixin` |
| Chunk memory | `PalettedContainerMixin` |
| First launch hardware class (lower distances, leaf culling on weak machines) | `HardwareClassifier`, `HardwareTier`, `HardwareProfile`, `GpuKind`, `HardwareSetup`, `VulkanMemoryProbe` |
| Faster point of interest searches (villager beds, job sites) | `ColumnIndexedLongMap`, `PoiColumns`, `PoiAudit`, `SectionStoragePoiIndexMixin`, `SectionStorageAccessor`, `PoiManagerColumnSearchMixin` |
| Entity collision search skip (sections without boats, shulkers, happy ghasts) | `CollidableEntities`, `CollisionAudit`, `EntityCollisionShapes`, `EntitySectionCollidableMixin`, `PersistentEntitySectionManagerAccessor`, `TransientEntitySectionManagerAccessor`, `ServerLevelEntityCollisionsMixin`, `ClientLevelEntityCollisionsMixin` |
| Particle collision search skip (empty air around a moving particle) | `ParticleCollisions`, `ParticleCollisionMixin` |
| Model cube corner reuse | `SharedCorners`, `CubeCorners`, `ModelCubeCornersMixin` |
| Teamless pushing | `PushRules`, `EntitySelectorPushMixin` |
| Idle hopper entity search skip (sections without items or item-holding entities) | `HopperEntities`, `HopperAudit`, `SectionCounts`, `EntitySectionHopperMixin`, `ServerLevelEntityManagerAccessor`, `EntityCapabilityAccessor`, `HopperBlockEntityItemSearchMixin`, `HopperEntityContainerSearchMixin` |
| Faster packet compression on servers (level 4 instead of 6) | `PacketCompression`, `CompressionCheck`, `CompressionEncoderLevelMixin` |
| Parallel feature generation (trees, ores, plants on several threads) | `ParallelFeatures`, `RegionLocks`, `JobSystem`, `ChunkStatusTasksFeaturesMixin` |
| Crash report entry (modules on, Kinetic code in the crash) | `CrashSummary`, `CrashReportSection`, `CrashReportMixin`, `MixinModules` |
| Light engine snapshots | `ShardedLongMap`, `ShardedLongMapBuilder`, `DataLayerStorageMapMixin`, `BlockDataLayerStorageMapMixin`, `SkyDataLayerStorageMapMixin` |
| Chunk draw list (terrain buffer lookups per frame) | `SectionDrawLists`, `SectionDrawRecord`, `LevelRendererSectionSliceMixin`, `DirectDraws`, `PackedDraws`, `PackedDrawPool`, `PackedSectionInfos`, `SectionInfoStorage`, `DrawCommandRing`, `LevelRendererDirectDrawMixin`, `RenderSectionMixin`, `CompiledSectionMeshMixin`, `UberGpuBufferMixin`, `SectionSliceHolder` |
| Block entity scan (skip sections without block entities) | `BlockEntitySectionHolder`, `RenderSectionBlockEntityMixin`, `LevelExtractorBlockEntitiesMixin` |
| Occlusion culling (caves, Helion shadow casters, entities, particles) | `TerrainOcclusion`, `ShadowCascadeCulling`, `CameraResult`, `TileMask`, `EntityOcclusion`, `ParticleOcclusion`, `HelionShadowCasters`, `OcclusionPipeline`, `OcclusionPass`, `DepthRaster`, `CameraOcclusionHolder`, `RenderSectionOcclusionMixin`, `OpaqueMask`, `OccluderBoxes`, `VisGraphMixin`, `LevelRendererOcclusionMixin`, `LevelExtractorOcclusionMixin`, `QuadParticleGroupOcclusionMixin`, `ParticleAccessor`, `HelionShadowCastersMixin` |
| Chunk mesh workers (own thread pool for chunk builds) | `MeshWorkers`, `LevelRendererMeshWorkersMixin`, `JobSystem`, `JobLoadMeter` |
| Leaf culling (optional, off by default) | `LeafCulling`, `ModelBlockRendererLeafCullingMixin`, `KineticConfig` |
| Chunk upload and builder lock timings | `ChunkUploadTimings`, `StallMeter`, `SectionRenderDispatcherTimingMixin` |
| Server light worker timings | `ServerLightTimings`, `ThreadedLevelLightEngineTimingMixin` |
| Face culling (skip terrain faces turned away from the camera) | `FacingSort`, `FaceRanges`, `FaceCulling`, `FaceRangesHolder`, `CompiledSectionMeshFacingMixin`, `LevelRendererFaceCullingMixin`, `SectionDrawLists` |
| Compact terrain vertices (16 instead of 28 bytes, off with a Helion that cannot read them) | `CompactVertexEncoder`, `CompactTerrainFormat`, `CompactMeshUpload`, `CompactWireframe`, `RenderPipelineBuilderCompactMixin`, `SectionCompilerCompactMixin`, `CompileTaskCompactMixin`, `MeshDataCompactMixin`, `ChunkSectionsToRenderCompactMixin`, `shaders/terrain/compact.vsh` |
| Dirty section scan (clean sections answered from an array) | `DirtySectionMirror`, `DirtySectionIndex`, `SectionDirtyStateMirrorMixin`, `SectionUpdateTrackerMirrorMixin`, `LevelExtractorDirtySectionsMixin` |
| GPU culling (advanced terrain path: the GPU builds the solid and cutout draw lists) | `GpuCulling`, `GpuSections`, `GpuSectionTable`, `GpuSectionEntry`, `TerrainGroups`, `GpuDrawList`, `CullPipeline`, `VulkanAccess`, `IndirectCountFeature`, `GpuBufferUsage`, `ChunkSectionsToRenderIndirectCountMixin`, `VulkanFeatureSetsMixin`, `VulkanConstStorageMixin`, `shaders/culling/terrain_draws.csh` |
| Section grid moves (only the edge columns when the camera enters another chunk) | `RotatingSectionStorageEdgeMixin` |
| Switching modules on and off | `OptimizationModule`, `ModuleSettings`, `ModuleFile`, `MixinModules`, `DeltaTrackerTimerPrecisionMixin` |
| Sodium and Iris detection | `CompatibilityGuard` |
| Pregeneration on join (everything within the radius generated, sent, lit and built before the loading screen closes) | `SpawnPregeneration`, `SpawnPregenerationSettings`, `PrepareSpawnTaskPreparingMixin`, `PrepareSpawnTaskPregenerationMixin`, `JoinPregeneration`, `TerrainReadiness`, `PregenerationHold`, `TerrainProgress`, `JoinProgress`, `LevelLoadTrackerPregenerationMixin`, `LevelExtractorPregenerationMixin`, `PacketProcessorPregenerationMixin`, `ClientLevelPregenerationMixin`, `ServerGamePacketListenerTerrainWaitMixin`, `OwnerTerrainWait` |
| Join warm-up (classes, network and the GPU culling pipeline ready before the first join) | `JoinWarmup`, `assets/kinetic/warmup/join_classes.txt`, `GpuCulling.warmUp` |
| Join check (frame gaps per loading phase, terrain missing at entry, first 10 s in game) | `JoinCheck`, `JoinShots`, `JoinTimeline`, `FreshWorld` |
| Settings with Arkea installed | `ArkeaSettings`, `KineticConfigScreen`, `KineticOptions`, `KineticPresets`, `DistantTerrainMeter`, `ModuleFile` |
| Settings without Arkea installed | `KineticClient`, `KineticConfig` |
| Key bindings | `KineticKeys` |
| Event wiring | `CommonEvents` (both sides), `ClientEvents` (client) |
| Mod name, version, loader versions, license, authors | `gradle.properties` |
| Mod list metadata and dependencies | `neoforge.mods.toml` |
| Mod logo | `branding/logo.png`, `assets/kinetic/textures/gui/logo.png` |
| Build setup, run configurations | `build.gradle` |
| Vanilla parity check (same world after N ticks with modules on and off) | `ParityHarness`, `ParityScenario`, `WorldgenCheck`, `ChunkStatusTasksTerrainMixin`, `StateHash`, `LevelRandomAccessor`, `runParity` in `build.gradle` |
| Unit tests | `src/test/java/`, see [codemap/tests.md](codemap/tests.md) |

## Overview

- Side: client and server. The common part records server tick times; the client part holds the overlay, the benchmark and every other measurement. Each side works without the other.
- Root package: `com.aryston.kinetic`.
- Game-independent code lives in `core`, `world` and `debug` and is covered by unit tests. Code that touches Minecraft or NeoForge lives in `integration` (both sides), `client` (client side) and `mixin`.

## Packages

| Package | Code map file | Classes | Purpose |
|---|---|---|---|
| `com.aryston.kinetic` | [root.md](codemap/root.md) | 1 | Entry class, logger, Sodium check. |
| `com.aryston.kinetic.client.benchmark` | [client.benchmark.md](codemap/client.benchmark.md) | 10 | Benchmark driver, world, camera, simulated players and join check. |
| `com.aryston.kinetic.client.chunk` | [client.chunk.md](codemap/client.chunk.md) | 30 | Chunk draw lists, dirty sections, face and leaf culling, mesh workers, compact vertices. |
| `com.aryston.kinetic.client.config` | [client.config.md](codemap/client.config.md) | 6 | Settings with Arkea installed: config screen, options, presets, module file. |
| `com.aryston.kinetic.client.culling` | [client.culling.md](codemap/client.culling.md) | 8 | Terrain, shadow, entity and particle occlusion on the client. |
| `com.aryston.kinetic.client.gpu` | [client.gpu.md](codemap/client.gpu.md) | 13 | GPU culling, indirect draws, Vulkan access and GPU stage timings. |
| `com.aryston.kinetic.client.lod` | [client.lod.md](codemap/client.lod.md) | 15 | Distant terrain on the client: sampling, generation, meshing, rendering, cache. |
| `com.aryston.kinetic.client` | [client.md](codemap/client.md) | 15 | Client setup, events, overlay, frame trace, keys, zoom, hardware setup. |
| `com.aryston.kinetic.client.model` | [client.model.md](codemap/client.model.md) | 1 | Shared model cube corners. |
| `com.aryston.kinetic.client.particle` | [client.particle.md](codemap/client.particle.md) | 1 | Particle collision search skip. |
| `com.aryston.kinetic.client.pipeline` | [client.pipeline.md](codemap/client.pipeline.md) | 4 | Shader and pipeline caches and pipeline warm-up. |
| `com.aryston.kinetic.client.pregeneration` | [client.pregeneration.md](codemap/client.pregeneration.md) | 4 | Join pregeneration and terrain readiness on the client. |
| `com.aryston.kinetic.config` | [config.md](codemap/config.md) | 1 | Common config spec. |
| `com.aryston.kinetic.core.budget` | [core.budget.md](codemap/core.budget.md) | 6 | Frame budget, timed systems, lap timer, stall meter and frame rate policy. |
| `com.aryston.kinetic.core.cache` | [core.cache.md](codemap/core.cache.md) | 7 | Disk cache, content keys, shader cache keys and SPIR-V binaries. |
| `com.aryston.kinetic.core.collection` | [core.collection.md](codemap/core.collection.md) | 3 | Sharded and column indexed long maps. |
| `com.aryston.kinetic.core.crash` | [core.crash.md](codemap/core.crash.md) | 1 | Crash report summary. |
| `com.aryston.kinetic.core.culling` | [core.culling.md](codemap/core.culling.md) | 12 | Occlusion engine: depth raster, opaque masks, voxel rays, passes. |
| `com.aryston.kinetic.core.hardware` | [core.hardware.md](codemap/core.hardware.md) | 4 | Hardware classification into tiers. |
| `com.aryston.kinetic.core.job` | [core.job.md](codemap/core.job.md) | 4 | Job system, region locks, load meter and main thread queue. |
| `com.aryston.kinetic.core.lod` | [core.lod.md](codemap/core.lod.md) | 20 | Distant terrain engine: store, mesher, planner, layers, disk cache, transitions. |
| `com.aryston.kinetic.core.mesh` | [core.mesh.md](codemap/core.mesh.md) | 4 | Compact vertex encoder, facing sort, face ranges and shared corners. |
| `com.aryston.kinetic.core.module` | [core.module.md](codemap/core.module.md) | 2 | Optimization modules and their settings. |
| `com.aryston.kinetic.core.parity` | [core.parity.md](codemap/core.parity.md) | 1 | State hash for the parity check. |
| `com.aryston.kinetic.core.pregeneration` | [core.pregeneration.md](codemap/core.pregeneration.md) | 3 | Terrain and join progress and the pregeneration hold. |
| `com.aryston.kinetic.core.telemetry` | [core.telemetry.md](codemap/core.telemetry.md) | 19 | Sample rings, statistics, frame, tick and GC summaries, heap and VRAM. |
| `com.aryston.kinetic.debug.benchmark` | [debug.benchmark.md](codemap/debug.benchmark.md) | 22 | Benchmark scenes, commands, setups, measurement, report and comparison. |
| `com.aryston.kinetic.debug.route` | [debug.route.md](codemap/debug.route.md) | 4 | Benchmark camera paths. |
| `com.aryston.kinetic.integration.crash` | [integration.crash.md](codemap/integration.crash.md) | 1 | Crash report section. |
| `com.aryston.kinetic.integration.light` | [integration.light.md](codemap/integration.light.md) | 1 | Light snapshot source. |
| `com.aryston.kinetic.integration` | [integration.md](codemap/integration.md) | 5 | Common events, Sodium clash, loading text, compatibility guard, mixin modules. |
| `com.aryston.kinetic.mixin` | [mixin--rendering.md](codemap/mixin--rendering.md), [mixin--startup-timing.md](codemap/mixin--startup-timing.md), [mixin--terrain-data.md](codemap/mixin--terrain-data.md), [mixin--world-server.md](codemap/mixin--world-server.md) | 110 | Split by topic: rendering and culling; join, caches and timings; compact vertices, chunk decoding, distant terrain; world and server. |
| `com.aryston.kinetic.world.collision` | [world.collision.md](codemap/world.collision.md) | 3 | Entity collision search skip. |
| `com.aryston.kinetic.world.entity` | [world.entity.md](codemap/world.entity.md) | 2 | Entity section counts and teamless pushing. |
| `com.aryston.kinetic.world.hopper` | [world.hopper.md](codemap/world.hopper.md) | 2 | Idle hopper entity search skip. |
| `com.aryston.kinetic.world.light` | [world.light.md](codemap/world.light.md) | 1 | Server light worker timings. |
| `com.aryston.kinetic.world` | [world.md](codemap/world.md) | 1 | Server tick time recorder. |
| `com.aryston.kinetic.world.network` | [world.network.md](codemap/world.network.md) | 2 | Faster packet compression. |
| `com.aryston.kinetic.world.parity` | [world.parity.md](codemap/world.parity.md) | 4 | Vanilla parity harness, scenarios and worldgen check. |
| `com.aryston.kinetic.world.poi` | [world.poi.md](codemap/world.poi.md) | 2 | Point of interest column index. |
| `com.aryston.kinetic.world.spawn` | [world.spawn.md](codemap/world.spawn.md) | 4 | Spawn pregeneration and owner terrain wait. |
| `com.aryston.kinetic.world.worldgen` | [world.worldgen.md](codemap/world.worldgen.md) | 1 | Parallel feature generation. |

## Other Files

| Topic | Code map file | Contents |
|---|---|---|
| Tests | [tests.md](codemap/tests.md) | 59 JUnit test files under `src/test/`. |
| Source files and asset folders | [resources.md](codemap/resources.md) | Non-Java files under `src/` and asset folders. |
| Build files | [build.md](codemap/build.md) | Gradle and CI files. |
