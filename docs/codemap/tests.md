# Kinetic Code Map: Tests

Part of the [Kinetic code map](../CODEMAP.md).

Plain JUnit 5 tests without a running game, run by `./gradlew build` and the CI. Each test class sits in the package of the class it checks.

### `com.aryston.kinetic.core.lod`

#### LodStoreTest
- Path: `src/test/java/com/aryston/kinetic/core/lod/LodStoreTest.java`
- Role: Checks packed fields, coordinates, downsampling, client/saved/seed precedence, complete preview repair of sparse client ancestors, partial/void coverage and distance/detail/byte residency including layers.
- Depends on: `LodStore`.

#### LodMesherTest
- Path: `src/test/java/com/aryston/kinetic/core/lod/LodMesherTest.java`
- Role: Checks top rectangle area/height/colour separation, five-quad flat tiles, complete void meshes, uploaded-child selection, stable uploaded detail after CPU eviction and skirts across detail levels and negative coordinates.
- Depends on: `LodMesher`, `LodTile`, `LodSample`.

#### GenerationPowerTest
- Path: `src/test/java/com/aryston/kinetic/core/lod/GenerationPowerTest.java`
- Role: Checks worker limits and monotonic concurrency across processor counts, plus Low/Balanced idle targets.
- Depends on: `GenerationPower`.

#### LodLayersTest
- Path: `src/test/java/com/aryston/kinetic/core/lod/LodLayersTest.java`
- Role: Checks canopy gaps/undersides, hidden faces, underwater ground, opaque/water ranges, layer validation, near-only propagation, light metadata, tint multiplication and pathological mesh byte limits.

#### LodDiskCacheTest
- Path: `src/test/java/com/aryston/kinetic/core/lod/LodDiskCacheTest.java`
- Role: Checks lossless compression, identity separation, corrupt/truncated entry repair and concurrent Windows publication without temporary debris.

#### LodAllocatorTest
- Path: `src/test/java/com/aryston/kinetic/core/lod/LodAllocatorTest.java`
- Role: Checks alignment, coalescing, page/size limits and double-release rejection.

#### LodTransitionsTest
- Path: `src/test/java/com/aryston/kinetic/core/lod/LodTransitionsTest.java`
- Role: Checks subdivision weights, negative coordinates, sparse-to-coarse coverage and eventual release of old keys.

#### LodCoverageTest
- Path: `src/test/java/com/aryston/kinetic/core/lod/LodCoverageTest.java`
- Role: Checks that loaded chunks cannot hide LOD before required meshes arrive, negative mask coordinates, fade preservation and known-requirement/unload/trim clearing.

#### LodPlannerTest
- Path: `src/test/java/com/aryston/kinetic/core/lod/LodPlannerTest.java`
- Role: Checks that coarse previews come before finer levels.

### `com.aryston.kinetic.client.lod`

#### LodShaderTest
- Path: `src/test/java/com/aryston/kinetic/client/lod/LodShaderTest.java`
- Role: Compiles both fragment paths and the vertex shader to SPIR-V with actual transitive game/resource includes, without starting the game or a GPU device.

### Other engine and adapter tests

| File | What it checks |
|---|---|
| `src/test/java/com/aryston/kinetic/LanguageFilesTest.java` | Every language file has the same keys and every config entry has a `.tooltip` translation (lesson L-011). |
| `src/test/java/com/aryston/kinetic/core/telemetry/SampleRingTest.java` | Order, overwriting the oldest samples, copies into existing arrays, marks, clearing, invalid capacity. |
| `src/test/java/com/aryston/kinetic/core/telemetry/MotionErrorRecorderTest.java` | The error is the distance between the simulated and the real step, an interruption skips the next step, the summary is in milliseconds. |
| `src/test/java/com/aryston/kinetic/core/telemetry/SampleStatisticsTest.java` | Nearest-rank percentiles, mean, population standard deviation, sorting only the counted prefix, empty samples. |
| `src/test/java/com/aryston/kinetic/core/telemetry/FrameTimeSummaryTest.java` | Steady frames give equal average and lows; spikes lower the 1% and 0.1% lows and the average as computed by hand. Frames over 33 and 50 ms and hitches are counted at the exact thresholds. |
| `src/test/java/com/aryston/kinetic/core/telemetry/TickSummaryTest.java` | Histogram buckets and their edges, ticks over 50 ms and their share, median. |
| `src/test/java/com/aryston/kinetic/core/telemetry/FrameTimeRecorderTest.java` | Frame times are the gaps between frame starts; a reset drops the gap. |
| `src/test/java/com/aryston/kinetic/core/culling/DepthRasterTest.java` | Walls hide boxes behind them but never boxes in front, partly covered boxes, gaps, a section never hides itself, near plane clipping, boxes behind the camera, orthographic light space, and 200 random scenes checked against ray casting: nothing visible is ever hidden. |
| `src/test/java/com/aryston/kinetic/core/culling/OpaqueMaskTest.java` | Empty and sparse sections give no boxes, a solid section is one box, a cave pocket splits into a few boxes with the exact volume, boxes only ever cover opaque blocks. |
| `src/test/java/com/aryston/kinetic/core/culling/VoxelRayTest.java` | Rays through open air, blocked by an opaque block, start block ignored, diagonal rays visit every crossed block, box entry parameter. |
| `src/test/java/com/aryston/kinetic/core/mesh/FacingSortTest.java` | Quads are grouped by facing with their bytes intact and slanted quads first; the winding of Minecraft's up face counts as up; groups behind the eye by more than the margin are skipped and both sides stay when the eye is between the planes. |
| `src/test/java/com/aryston/kinetic/core/mesh/SharedCornersTest.java` | Equal positions share a corner, while positive and negative zero remain distinct by exact float bits. |
| `src/test/java/com/aryston/kinetic/core/mesh/FaceRangesTest.java` | Packed face ranges at an offset give the same first quads, quad counts and visible groups as the object for eyes on every side; each side of the eye shows only the faces turned to it. |
| `src/test/java/com/aryston/kinetic/core/culling/OcclusionPipelineTest.java` | A camera pass arrives one frame later and `begin` never blocks while one is in flight; `clear` drops the result in flight; `covers` accepts only eyes within the reach; the worker result equals the same `OcclusionPass` run directly; widening reaches farther eyes and only reveals sections; a changed occluder in front reveals the section behind it while a far one does not; only the same boxes and fade state count as unchanged. |
| `src/test/java/com/aryston/kinetic/core/culling/TileMaskTest.java` | Ranges touching a marked tile, marking everything, clearing. |
| `src/test/java/com/aryston/kinetic/core/mesh/CompactVertexEncoderTest.java` | Positions, color and texture coordinates on the model grid decode back exactly; block and sky light packing; positions outside the range are clamped and counted; off-grid texture edges round toward the inside of the quad; the far atlas edge stays in range. |
| `src/test/java/com/aryston/kinetic/core/culling/CullingYieldTest.java` | Culling that hides enough never pauses, culling that hides little pauses after a run of results and probes again after the pause, one useful probe keeps it running, and a reset ends a pause. |
| `src/test/java/com/aryston/kinetic/core/culling/CullingStatisticsTest.java` | The shadow caster share for the benchmark log sums every cascade of a frame and every frame since the last take, and resets when taken; camera result counts per frame since the last take. |
| `src/test/java/com/aryston/kinetic/core/culling/OcclusionPassTest.java` | A solid wall of sections hides the sections behind it and none of its own; sections that may not occlude hide nothing; `depthFromW` keeps depth order. |
| `src/test/java/com/aryston/kinetic/core/collection/ShardedLongMapTest.java` | Snapshot equals the source, published snapshots never change, random writes against a plain map, read-only behavior. |
| `src/test/java/com/aryston/kinetic/core/telemetry/GcAdviceTest.java` | Collector recognition from bean names, advice for G1 and for a small heap. |
| `src/test/java/com/aryston/kinetic/core/telemetry/GcPauseRecorderTest.java` | Pauses versus concurrent cycles, totals, pauses after a mark. |
| `src/test/java/com/aryston/kinetic/core/budget/FrameBudgetTest.java` | Frame and system budgets, fixed 50 ms tick budget, budget edge, invalid target. |
| `src/test/java/com/aryston/kinetic/core/budget/LapTimerTest.java` | Empty first lap, laps keep the fraction of a millisecond, elapsed time does not start a lap, laps add up to the whole time. |
| `src/test/java/com/aryston/kinetic/core/collection/ColumnIndexedLongMapTest.java` | A column is complete only with every section stored; replacing and removing keep counts and masks exact, double removes are harmless; columns over 64 sections are rejected. |
| `src/test/java/com/aryston/kinetic/core/hardware/HardwareClassifierTest.java` | A gaming desktop is high, an integrated laptop medium; the weakest part (4 threads, small heap, software renderer, little video memory or RAM) decides; unknown values do not lower; tiers only lower distances. |
| `src/test/java/com/aryston/kinetic/core/crash/CrashSummaryTest.java` | No Kinetic code gives the "none" line; a mixin handler in the stack names its module with the fixed marker and the switch-off hint; own frames and always-on mixins are listed without a hint. |
| `src/test/java/com/aryston/kinetic/core/parity/StateHashTest.java` | Same input same hash, order and every value matter, FNV offset basis for an empty hash. |
| `src/test/java/com/aryston/kinetic/core/budget/StallMeterTest.java` | Sum, average and longest stall, reset on take. |
| `src/test/java/com/aryston/kinetic/core/budget/SystemTimingsTest.java` | Smoothing, peaks reset on sampling, over budget flags. |
| `src/test/java/com/aryston/kinetic/core/job/JobSystemTest.java` | Worker count rule, every job runs on named workers, no jobs after closing. |
| `src/test/java/com/aryston/kinetic/core/job/JobLoadMeterTest.java` | Timed service counts jobs and busy time, jobs per second and busy share, empty interval, no jobs after closing. |
| `src/test/java/com/aryston/kinetic/core/job/RegionLocksTest.java` | Separate regions are held together, an overlapping region waits for the release, and eight threads taking random overlapping regions never hold the same cell at once. |
| `src/test/java/com/aryston/kinetic/core/job/MainThreadQueueTest.java` | Order, time budget, at least one task per drain. |
| `src/test/java/com/aryston/kinetic/debug/route/CameraPathTest.java` | Lines move along their yaw; orbits keep their radius and height, look at the center and close after one turn. |
| `src/test/java/com/aryston/kinetic/core/cache/ShaderCacheKeyTest.java` | Direct and transitive includes (with a cycle), stable keys for reordered defines, a new key for every change that affects the binary (defines, flags, device switches, include content). |
| `src/test/java/com/aryston/kinetic/core/cache/CacheFilesTest.java` | Disk cache round trip without leftover temporary files, existing entries kept, eight concurrent writers and readers of one key, SPIR-V validation, pipeline cache header checks, cache counters. |
| `src/test/java/com/aryston/kinetic/core/budget/FrameRatePolicyTest.java` | Active, background and paused limits, never above a lower vanilla limit, disabled policy. |
| `src/test/java/com/aryston/kinetic/core/module/ModuleSettingsTest.java` | Every module starts on, switched-off and unknown lines, every module written back. |
| `src/test/java/com/aryston/kinetic/debug/benchmark/BenchmarkCommandsTest.java` | Frozen time and weather, large boxes split into fills within the vanilla limit, oversized layers refused, every scene setup within the limit, tagged crowd summons and their removal, every storage row placed, the explosion lit only on the first measured tick, the particle count per tick, scenes without a site build nothing. |
| `src/test/java/com/aryston/kinetic/debug/benchmark/SceneMeasurementTest.java` | Only frames after the scene start count, peaks of heap and VRAM, frames lost from a full ring. |
| `src/test/java/com/aryston/kinetic/debug/benchmark/SimulatedPlayerRoutesTest.java` | Simulated players start in distinct directions, walk one chunk outward per step and move only on step ticks. |
| `src/test/java/com/aryston/kinetic/debug/benchmark/BenchmarkReportTest.java` | Comparison percentages, JSON round trip, newest report by start time, missing folder, text report content. |
| `src/test/java/com/aryston/kinetic/world/network/PacketCompressionTest.java` | Packets compressed at the faster level inflate to the same bytes and stay within 10 % of the vanilla size. |
| `src/test/java/com/aryston/kinetic/core/pregeneration/TerrainProgressTest.java` | Chunks fill the first half and built sections the second; complete only with every chunk, section and light update; every kind of advance is noticed. |
| `src/test/java/com/aryston/kinetic/core/pregeneration/PregenerationHoldTest.java` | Waits while terrain is missing, releases after the fade-in time, restarts the fade when new sections appear, gives up when nothing advances and after the limit. |
| `src/test/java/com/aryston/kinetic/core/pregeneration/JoinProgressTest.java` | The server fills the first 60 %, terrain the rest, values outside the range are clamped. |
| `src/test/java/com/aryston/kinetic/world/spawn/SpawnPregenerationSettingsTest.java` | The radius stays within 6 to 32, never goes past the render distance, the switch is followed. |
| `src/test/java/com/aryston/kinetic/debug/benchmark/JoinTimelineTest.java` | The first frame starts the timeline, each frame gap is charged to the phase it started in, long gaps name both phases. |
