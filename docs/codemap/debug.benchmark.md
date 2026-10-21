# Aervium Code Map: `com.aryston.aervium.debug.benchmark`

Part of the [Aervium code map](../CODEMAP.md). Every class of this package has its entry here and nowhere else (rule D1).

### `com.aryston.aervium.debug.benchmark`

#### BenchmarkScene
- Path: `src/main/java/com/aryston/aervium/debug/benchmark/BenchmarkScene.java`
- Role: The stress scenes in route order: walk, flight, underground, crowd, storage, particles, redstone, explosion, server and overlook (circling 300 blocks above the spawn and looking down), each with a setup and site.
- Depends on: `SceneSetup`, `SceneSite`.

#### BenchmarkRoute
- Path: `src/main/java/com/aryston/aervium/debug/benchmark/BenchmarkRoute.java`
- Role: Timing (200 settle and 600 measured ticks per scene) and the camera path of every scene from the anchors.
- Depends on: `BenchmarkScene`, `BenchmarkAnchors`, `LinePath`, `OrbitPath`.

#### BenchmarkCommands
- Path: `src/main/java/com/aryston/aervium/debug/benchmark/BenchmarkCommands.java`
- Role: Server world rules for the benchmark.
- Depends on: nothing.

#### BoxCommands
- Path: `src/main/java/com/aryston/aervium/debug/benchmark/BoxCommands.java`
- Role: Builds bounded fill, force-load and cleanup commands for stress scenes; splits fills within the vanilla 32768-block limit, removes loose item and experience-orb drops between scenes, and uses the `aervium_stress` entity tag.
- Depends on: `BlockPoint`.

#### SceneSetup
- Path: `src/main/java/com/aryston/aervium/debug/benchmark/SceneSetup.java`
- Role: Interface for a stress scene's size, height, build, per-tick commands, cleanup, center and far point; `NONE` has no setup.
- Depends on: `BlockPoint`.

#### CrowdSetup
- Path: `src/main/java/com/aryston/aervium/debug/benchmark/CrowdSetup.java`
- Role: Crowd scene: a glass pen on a platform with 1600 tagged mobs of eight passive types (AI on, persistent), 800 floating items that never despawn or merge and 200 floating armor stands; clearing kills the tag and empties the box.
- Depends on: `SceneSetup`, `BoxCommands`.

#### StorageSetup
- Path: `src/main/java/com/aryston/aervium/debug/benchmark/StorageSetup.java`
- Role: Storage scene: a 64 x 64 platform with two layers of block entity rows (chests, signs with text on both sides, banners with patterns, lit furnaces, hopper chains, ender chests, barrels, shulker boxes), 8192 in all.
- Depends on: `SceneSetup`, `BoxCommands`.

#### ParticleStormSetup
- Path: `src/main/java/com/aryston/aervium/debug/benchmark/ParticleStormSetup.java`
- Role: Particle scene: 32 x 32 lit campfires and, every tick, forced bursts of flame, end rod, smoke and explosion particles over them (`particlesPerTick()`).
- Depends on: `SceneSetup`, `BoxCommands`.

#### RedstoneSetup
- Path: `src/main/java/com/aryston/aervium/debug/benchmark/RedstoneSetup.java`
- Role: Redstone scene: 12 clusters of 16 observer clocks with sticky pistons and lamps on a stone platform (192 clocks), built after their surroundings are cleared.
- Depends on: `SceneSetup`, `BoxCommands`.

#### ExplosionSetup
- Path: `src/main/java/com/aryston/aervium/debug/benchmark/ExplosionSetup.java`
- Role: Explosion scene: 16 x 16 x 8 TNT on the ground, lit by a redstone block on the first measured tick; clearing kills dropped items and leftover primed TNT around the crater.
- Depends on: `SceneSetup`, `BoxCommands`, `BenchmarkRoute`.

#### SimulatedPlayerRoutes
- Path: `src/main/java/com/aryston/aervium/debug/benchmark/SimulatedPlayerRoutes.java`
- Role: Game-independent routes for twelve moving ender-pearl chunk tickets that load, generate and tick terrain like extra integrated-server players.
- Depends on: `BlockPoint`.

#### BlockPoint
- Path: `src/main/java/com/aryston/aervium/debug/benchmark/BlockPoint.java`
- Role: Game-independent block position with offset and command coordinates.
- Depends on: nothing.

#### SceneSite
- Path: `src/main/java/com/aryston/aervium/debug/benchmark/SceneSite.java`
- Role: A scene's east and south offset from spawn and height above the surface.
- Depends on: nothing.

#### BenchmarkAnchors
- Path: `src/main/java/com/aryston/aervium/debug/benchmark/BenchmarkAnchors.java`
- Role: Record of the spawn and sites by scene id.
- Depends on: `BlockPoint`.

#### SceneMeasurement
- Path: `src/main/java/com/aryston/aervium/debug/benchmark/SceneMeasurement.java`
- Role: Marks of the frame, motion-error, tick and GC recorders at the start of a measured scene, peak heap and VRAM during it, and the `SceneResult` at its end including frames lost from a full ring.
- Depends on: `SampleRing`, `FrameTimeSummary`, `MotionErrorSummary`, `TickSummary`, `GcPauseTotals`, `SceneResult`.

#### SceneResult
- Path: `src/main/java/com/aryston/aervium/debug/benchmark/SceneResult.java`
- Role: Record of one measured scene for the JSON report, including motion error.
- Depends on: `FrameTimeSummary`, `MotionErrorSummary`, `TickSummary`, `GcPauseTotals`.

#### BenchmarkEnvironment
- Path: `src/main/java/com/aryston/aervium/debug/benchmark/BenchmarkEnvironment.java`
- Role: Record of versions, operating system, CPU threads, heap limit, GPU, window size, render and simulation distance, frame limit, vsync, seed and installed mods.
- Depends on: nothing.

#### BenchmarkReport
- Path: `src/main/java/com/aryston/aervium/debug/benchmark/BenchmarkReport.java`
- Role: Format-version-2 record of a whole run with start time, environment, anchors, scenes and overall frame and tick summaries.
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
- Role: Writes `benchmark-<start>.json` and `.txt` and reads the newest earlier format-version-2 JSON report.
- Depends on: `BenchmarkReport`.

#### JoinTimeline
- Path: `src/main/java/com/aryston/aervium/debug/benchmark/JoinTimeline.java`
- Role: Frame starts of a world join in phases (reading world data, data packs, server, terrain): per phase its start, length, frame count, longest frame and frames over 50 ms, and the eight longest frame gaps with the phases they fell between, as log lines.
- Depends on: `Durations`.
