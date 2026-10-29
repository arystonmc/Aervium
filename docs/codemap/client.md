# Kinetic Code Map: `com.aryston.kinetic.client`

Part of the [Kinetic code map](../CODEMAP.md). Every class of this package has its entry here and nowhere else (rule D1).

### `com.aryston.kinetic.client`

#### KineticClient
- Path: `src/main/java/com/aryston/kinetic/client/KineticClient.java`
- Role: Client entry point (`@Mod` with `Dist.CLIENT`): registers the client config and events, then checks `ModList` for Arkea. With Arkea it delegates the settings-screen factory to `ArkeaSettings`; without Arkea it keeps NeoForge's `ConfigurationScreen`.
- Depends on: `KineticConfig`, `ArkeaSettings`, `ClientEvents`.

#### ClientEvents
- Path: `src/main/java/com/aryston/kinetic/client/ClientEvents.java`
- Distant terrain lifecycle: live/module enable gates, chunk load/unload, model-reload invalidation, indirect staging retirement and worker/GPU shutdown before device destruction.
- Role: Client adapter: frame start and end (`RenderFrameEvent`), client tick scopes, the F3 debug entry, the benchmark-only overlay layer, the end of every frame for `DirectDraws` (`FlipFrameEvent`), config loading (overlay, budget, dynamic frame rate, pregeneration and distant terrain; pregeneration stays off during the benchmark), loaded chunks for distant-terrain capture, fog extension, Sodium and Iris check, installing the shader caches and the pipeline warm-up (only without Sodium and Iris), the startup report, the join warm-up and the GPU culling pipeline compile once the game has loaded, the join check, world joins, leaving a world (ends a terrain wait) and shutdown (saves the pipeline cache before the job system stops).
- Depends on: `ClientTelemetry`, `TelemetryOverlay`, `BenchmarkDriver`, `JoinCheck`, `JoinWarmup`, `JoinPregeneration`, `DistantTerrain`, `DistantTerrainFog`, `DistantTerrainRenderer`, `GpuCulling`, `KineticKeys`, `KineticConfig`, `SpawnPregenerationSettings`, `MixinModules`, `CompatibilityGuard`, `SystemTimings`, `ShaderCaches`, `PipelineWarmup`, `FrameRateGovernor`.

#### JoinWarmup
- Path: `src/main/java/com/aryston/kinetic/client/JoinWarmup.java`
- Role: Once the game has loaded, a low priority thread prepares the first world join: it starts Netty's network address lookup (about 100 ms the first time a connection is made) and loads, without initializing, every class in `assets/kinetic/warmup/join_classes.txt`, the 1721 classes the render thread loaded during a join on Minecraft 26.3. Loading and transforming them during the join cost the render thread about 600 ms in frames of 70 to 160 ms each. Classes that are not installed are skipped.
- Depends on: `Durations`.
- Notes: Module `joinWarmup` (no mixins). The list is version specific; regenerate it from a JFR recording with class loading on when Minecraft changes (see the porting checklist in `docs/ARCHITECTURE.md`).

#### FrameRateGovernor
- Path: `src/main/java/com/aryston/kinetic/client/FrameRateGovernor.java`
- Role: Applies the `FrameRatePolicy` to the vanilla frame limit from the window focus and pause state; suspended while the benchmark runs.
- Depends on: `FrameRatePolicy`.

#### ClientTelemetry
- Path: `src/main/java/com/aryston/kinetic/client/ClientTelemetry.java`
- Role: Owns the overlay frame recorder (2048 frames), the GC recorder, the job system and the main-thread queue; every five client ticks it copies the raw data, summarizes it on a worker and publishes the snapshot on the main thread within 0.5 ms.
- Depends on: `FrameTimeRecorder`, `MotionErrorRecorder`, `GcPauseRecorder`, `JobSystem`, `MainThreadQueue`, `TickTimeRecorder`, `SystemTimings`, `VulkanMemoryProbe`.

#### MotionSampler
- Path: `src/main/java/com/aryston/kinetic/client/MotionSampler.java`
- Role: Reads the game-time delta of a presented frame and converts it to milliseconds, skipping paused or frozen worlds.
- Depends on: nothing inside the mod.

#### TelemetryOverlay
- Path: `src/main/java/com/aryston/kinetic/client/TelemetryOverlay.java`
- Role: GUI layer in the top right corner, shown only during benchmark runs (players see the same lines in F3): text lines and a 240 frame time graph (0 to 50 ms) with the budget line; bars are green, yellow over budget and red over twice the budget. Reads frames into a preallocated array and submits all bars as one `FrameGraphRenderState`, because vanilla checks every new GUI element against the elements before it, which made 240 single bars cost about 7 % of the render thread. The visual order of the text lines is built when a snapshot arrives, not every frame: the bidirectional reordering of the plain strings was about 5 % of the render thread with the overlay open.
- Depends on: `ClientTelemetry`, `OverlayLines`, `FrameBudget`, `SystemTimings`, `FrameGraphRenderState`.

#### FrameTrace
- Path: `src/main/java/com/aryston/kinetic/client/FrameTrace.java`
- Role: Collects what happened in the current frame from the timing mixins and commits it as a `FrameTraceEvent` when the next frame starts; does nothing while the event is disabled.
- Depends on: `FrameTraceEvent`, `CameraResult`.

#### FrameGraphRenderState
- Path: `src/main/java/com/aryston/kinetic/client/FrameGraphRenderState.java`
- Role: One GUI element with the quads of every bar of the overlay graph (x, top, bottom and color per bar) on the plain GUI pipeline.
- Depends on: nothing.

#### OverlayLines
- Path: `src/main/java/com/aryston/kinetic/client/OverlayLines.java`
- Role: Turns a `TelemetrySnapshot` into colored text lines; rebuilt only when a snapshot arrives. Shows hitches and motion error, yellow when the p99 motion error exceeds 0.5 ms or a hitch occurred, culling counts, distant terrain recorded chunks, tiles, sample memory, meshes and drawn tiles, and, with module `meshWorkers`, chunk builds per second, busy workers and queued builds.
- Depends on: `TelemetrySnapshot`, `ByteSizes`, `CullingStatistics`, `MeshWorkers`, `DistantTerrain`, `DistantTerrainRenderer`.

#### KineticKeys
- Path: `src/main/java/com/aryston/kinetic/client/KineticKeys.java`
- Role: Key binding category and the zoom key mapping (`C`).
- Depends on: `Kinetic`.

#### ZoomController
- Path: `src/main/java/com/aryston/kinetic/client/ZoomController.java`
- Role: Handles smooth in-game FOV zoom while holding `C`, exponential zoom smoothing, mouse wheel magnification adjustments, and turn sensitivity scaling.
- Depends on: `KineticKeys`.

#### KineticDebugEntry
- Path: `src/main/java/com/aryston/kinetic/client/KineticDebugEntry.java`
- Role: F3 debug screen entry (shown only while F3 is open) with every performance overlay line: frame rate and lows, smoothness, MSPT, GC, memory, culling, distant terrain, chunk builds and system timings; hidden when the F3 performance details setting is off.
- Depends on: `ClientEvents`, `OverlayLines`, `KineticConfig`.

#### HardwareSetup
- Path: `src/main/java/com/aryston/kinetic/client/HardwareSetup.java`
- Role: When the game has loaded, measures the machine (device type from `DeviceInfo`, video memory from `VulkanMemoryProbe`, RAM from the OS bean) and logs its class. On Kinetic's first launch only (no `config/kinetic-hardware.properties` yet, and not a benchmark run) applies the class: lowers distances in `options.txt`, turns leaf culling on for low, writes the record file, and shows a toast when something changed. Deleting the file detects again.
- Depends on: `HardwareClassifier`, `VulkanMemoryProbe`, `KineticConfig`, `LeafCulling`.

#### VulkanMemoryProbe
- Path: `src/main/java/com/aryston/kinetic/client/VulkanMemoryProbe.java`
- Role: Reads used and budgeted device-local memory from the game's Vulkan memory allocator (`vmaGetHeapBudgets`), and the total size of the device-local heaps for the hardware class.
- Depends on: `FrontendGpuDeviceAccessor`, `VramUsage`.
