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
| Frame budget and timed systems | `FrameBudget`, `TimedSystem`, `SystemTimings` |
| Worker threads and results back on the main thread | `JobSystem`, `MainThreadQueue` |
| Overlay drawing and text | `TelemetryOverlay`, `OverlayLines`, `ClientTelemetry` |
| VRAM usage | `VulkanMemoryProbe`, `FrontendGpuDeviceAccessor` |
| Benchmark route and scenes | `BenchmarkRoute`, `BenchmarkScene`, `LinePath`, `OrbitPath` |
| Benchmark world, commands and camera | `BenchmarkWorld`, `BenchmarkCommands`, `BenchmarkCamera` |
| Benchmark flow and report | `BenchmarkDriver`, `SceneMeasurement`, `BenchmarkReport`, `BenchmarkText`, `BenchmarkFiles`, `BenchmarkComparison` |
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

#### CompatibilityGuard
- Path: `src/main/java/com/aryston/aervium/integration/CompatibilityGuard.java`
- Role: Lists installed renderer mods (Sodium, Iris) that turn Aervium's optimizations off.
- Depends on: nothing inside the mod.

### `com.aryston.aervium.config`

#### AerviumConfig
- Path: `src/main/java/com/aryston/aervium/config/AerviumConfig.java`
- Role: Client config: overlay visibility (saved when F7 toggles it) and the target frame rate of the frame budget.
- Depends on: `FrameBudget`.

### `com.aryston.aervium.client`

#### AerviumClient
- Path: `src/main/java/com/aryston/aervium/client/AerviumClient.java`
- Role: Client entry point (`@Mod` with `Dist.CLIENT`): registers the client config, the NeoForge config screen and the client events.
- Depends on: `AerviumConfig`, `ClientEvents`.

#### ClientEvents
- Path: `src/main/java/com/aryston/aervium/client/ClientEvents.java`
- Role: Client adapter: frame start and end (`RenderFrameEvent`), client tick scopes, the F7 key, the overlay layer, config loading, Sodium and Iris check, world joins and shutdown.
- Depends on: `ClientTelemetry`, `TelemetryOverlay`, `BenchmarkDriver`, `AerviumKeys`, `AerviumConfig`, `CompatibilityGuard`, `SystemTimings`.

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
| `src/test/java/com/aryston/aervium/core/telemetry/GcPauseRecorderTest.java` | Pauses versus concurrent cycles, totals, pauses after a mark. |
| `src/test/java/com/aryston/aervium/core/budget/FrameBudgetTest.java` | Frame and system budgets, fixed 50 ms tick budget, budget edge, invalid target. |
| `src/test/java/com/aryston/aervium/core/budget/SystemTimingsTest.java` | Smoothing, peaks reset on sampling, over budget flags. |
| `src/test/java/com/aryston/aervium/core/job/JobSystemTest.java` | Worker count rule, every job runs on named workers, no jobs after closing. |
| `src/test/java/com/aryston/aervium/core/job/MainThreadQueueTest.java` | Order, time budget, at least one task per drain. |
| `src/test/java/com/aryston/aervium/debug/route/CameraPathTest.java` | Lines move along their yaw; orbits keep their radius and height, look at the center and close after one turn. |
| `src/test/java/com/aryston/aervium/debug/benchmark/BenchmarkCommandsTest.java` | Crowd size and distinct positions, build order of the redstone platform, every fill within the vanilla volume limit, frozen time and weather. |
| `src/test/java/com/aryston/aervium/debug/benchmark/SceneMeasurementTest.java` | Only frames after the scene start count, peaks of heap and VRAM, frames lost from a full ring. |
| `src/test/java/com/aryston/aervium/debug/benchmark/BenchmarkReportTest.java` | Comparison percentages, JSON round trip, newest report by start time, missing folder, text report content. |

## Source Files

| File | Purpose |
|---|---|
| `src/main/templates/META-INF/neoforge.mods.toml` | Mod metadata template filled from `gradle.properties`: NeoForge and Minecraft on both sides, `IGNORE_ALL_VERSION` so either side works alone, discouraged Sodium and Iris, mixin config. |
| `src/main/resources/aervium.mixins.json` | Mixin configuration listing `FrontendGpuDeviceAccessor` (client). |

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
