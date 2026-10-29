# Kinetic Code Map: `com.aryston.kinetic.client.benchmark`

Part of the [Kinetic code map](../CODEMAP.md). Every class of this package has its entry here and nowhere else (rule D1).

### `com.aryston.kinetic.client.benchmark`

#### BenchmarkOptions
- Path: `src/main/java/com/aryston/kinetic/client/benchmark/BenchmarkOptions.java`
- Role: Before the benchmark world is created, remembers the frame limit, vsync, render and simulation distance, sets an unlimited frame rate (`Options.UNLIMITED_FRAMERATE_CUTOFF`), vsync off, render distance 32 unless `-Dkinetic.benchmark.renderDistance` sets it and the simulation distance from `.simulationDistance`, then restores all four when the benchmark ends.
- Depends on: nothing inside the mod.

#### BenchmarkDriver
- Path: `src/main/java/com/aryston/kinetic/client/benchmark/BenchmarkDriver.java`
- Role: Benchmark state machine on the client tick: creates the world from the first screen, prepares and locates, warms up, runs every scene, measures, writes the report on a worker and quits when asked. Turns pause on lost focus off for the run and restores it; `-Dkinetic.benchmark.screenshots=true` saves a screenshot at the end of every scene.
- Members:
  - `requested(...)`: present when `-Dkinetic.benchmark=true` is set; `-Dkinetic.benchmark.quit=true` closes the game afterwards.
  - `frameStarted(long)`: records frames into its own 131072 frame ring while scenes run.
- Depends on: `BenchmarkWorld`, `BenchmarkCamera`, `BenchmarkEnvironmentProbe`, `BenchmarkRoute`, `SceneMeasurement`, `BenchmarkFiles`, `BenchmarkText`, `BenchmarkComparison`, `TickTimeRecorder`, `GcPauseRecorder`, `JobSystem`, `MainThreadQueue`.

#### BenchmarkWorld
- Path: `src/main/java/com/aryston/kinetic/client/benchmark/BenchmarkWorld.java`
- Role: Deletes the previous `kinetic-benchmark` world, creates a fresh one (seed 8675309, spectator, peaceful, cheats on), runs world rules, force-loads, builds and ticks each scene setup once its entities are loaded, then clears and releases it.
- Depends on: `BenchmarkCommands`, `BenchmarkAnchors`, `BenchmarkScene`, `SceneSetup`, `CommandFailureLog`, `BlockPoint`, `FreshWorld`.

#### BenchmarkCamera
- Path: `src/main/java/com/aryston/kinetic/client/benchmark/BenchmarkCamera.java`
- Role: Moves the spectator player along a `CameraPath` every client tick; walking routes follow the client heightmap with at most 0.6 blocks of climb per tick.
- Depends on: `CameraPath`, `CameraPose`.

#### CommandFailureLog
- Path: `src/main/java/com/aryston/kinetic/client/benchmark/CommandFailureLog.java`
- Role: Command source for benchmark scene setup and cleanup that logs failures only.
- Depends on: nothing inside the mod.

#### SimulatedPlayers
- Path: `src/main/java/com/aryston/kinetic/client/benchmark/SimulatedPlayers.java`
- Role: Client adapter that runs the engine's simulated-player chunk-ticket routes in the benchmark world.
- Depends on: `SimulatedPlayerRoutes`.

#### BenchmarkEnvironmentProbe
- Path: `src/main/java/com/aryston/kinetic/client/benchmark/BenchmarkEnvironmentProbe.java`
- Role: Collects the `BenchmarkEnvironment` from the game, the options, the window, the GPU and the mod list.
- Depends on: `BenchmarkEnvironment`, `BenchmarkWorld`.

#### FreshWorld
- Path: `src/main/java/com/aryston/kinetic/client/benchmark/FreshWorld.java`
- Role: Deletes a world folder if it exists and creates a fresh normal world with the given settings and seed through vanilla's world creation; shared by the benchmark and the join check.
- Depends on: nothing inside the mod.

#### JoinCheck
- Path: `src/main/java/com/aryston/kinetic/client/benchmark/JoinCheck.java`
- Role: Developer check of a world join (`-Dkinetic.joinCheck=true`): from the first loading screen frame it times every frame per phase, logs at entry how many chunks within the render distance are missing and how many visible non-empty sections have no mesh, samples both every second for 10 s, and logs the frame rate, lows and longest frame of those 10 s. `kinetic.joinCheck.world=<folder>` opens a world and `kinetic.joinCheck.newWorld=true` creates a fresh one after `kinetic.joinCheck.delay` seconds (5) on the title screen; `kinetic.joinCheck.quit=true` closes the game afterwards. Joins started otherwise (quick play, the menus) are measured too.
- Depends on: `JoinTimeline`, `JoinShots`, `FreshWorld`, `FrameTimeRecorder`, `FrameTimeSummary`.

#### JoinShots
- Path: `src/main/java/com/aryston/kinetic/client/benchmark/JoinShots.java`
- Role: With `-Dkinetic.joinCheck.shots=true` saves `screenshots/join_<ms>_<phase>.png` every 700 ms while loading and `join_entry_<ms>.png` every 70 ms in the first 1.4 s in game, to check loading screens and transitions. Screenshots cost frame time, so timings of such a run are not comparable.
- Depends on: `Durations`.
