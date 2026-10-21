# Aervium Code Map: `com.aryston.aervium.mixin` (startup-timing)

Part of the [Aervium code map](../CODEMAP.md). The package is split by topic; this part covers world join, shader and pipeline caches, timings, crash report, parity. Every class of the package has its entry in exactly one of these files (rule D1).

### `com.aryston.aervium.mixin`

#### DeltaTrackerTimerPrecisionMixin
- Path: `src/main/java/com/aryston/aervium/mixin/DeltaTrackerTimerPrecisionMixin.java`
- Role: Advances `DeltaTracker$Timer` game and real time from `Util.getNanos()` through `LapTimer`, then moves the partial tick to the draw moment when it remains in the current tick.
- Notes: Module `preciseFrameTime`, client side. No API replaces the millisecond time vanilla feeds to the timer.

#### IntegratedServerJoinDistanceMixin
- Path: `src/main/java/com/aryston/aervium/mixin/IntegratedServerJoinDistanceMixin.java`
- Role: Sets the integrated server's initial view and simulation distances from the client options after server initialization, before a player joins.
- Notes: No API sets the initial integrated-server distances at this point.

#### PrepareSpawnTaskPregenerationMixin, PrepareSpawnTaskPreparingMixin
- Path: `src/main/java/com/aryston/aervium/mixin/PrepareSpawnTaskPregenerationMixin.java`, `src/main/java/com/aryston/aervium/mixin/PrepareSpawnTaskPreparingMixin.java`
- Role: When vanilla's `PrepareSpawnTask$Preparing` schedules its three chunk radius around the found spawn, also starts the `SpawnPregeneration`, reports its chunk counts through the same `LOAD_PLAYER_CHUNKS` progress and holds the task until both loads are done; the outer task keeps the pregeneration ticket alive every configuration tick until `spawnPlayer` or `close`.
- Notes: Module `spawnPregeneration`, physical client only (integrated server). No API exposes the spawn preparation or its progress. The inner class is reached through its synthetic `this$0`.

#### ServerGamePacketListenerTerrainWaitMixin
- Path: `src/main/java/com/aryston/aervium/mixin/ServerGamePacketListenerTerrainWaitMixin.java`
- Role: Pauses `tickClientLoadTimeout` for the owner while `OwnerTerrainWait` is set, so the server treats the player as still loading (invulnerable, not moved) during the terrain wait instead of after vanilla's three seconds.
- Notes: Module `spawnPregeneration`, physical client only.

#### LevelLoadTrackerPregenerationMixin
- Path: `src/main/java/com/aryston/aervium/mixin/LevelLoadTrackerPregenerationMixin.java`
- Role: Adopts the tracker of a single player world in `setServerChunkStatusView` (only `Minecraft.doWorldLoad` calls it), starts the terrain wait in `startClientLoad`, keeps `isLevelReady` false while `JoinPregeneration` holds and blends the terrain progress into `serverProgress`.
- Notes: Module `spawnPregeneration`. Vanilla offers no hook between "player chunk built" and "loading screen closes".

#### LevelExtractorPregenerationMixin
- Path: `src/main/java/com/aryston/aervium/mixin/LevelExtractorPregenerationMixin.java`
- Role: Replaces the visible section list that `LevelExtractor.extract` scans for sections to build with the batch from `JoinPregeneration` while the terrain wait runs; drawing, entities and culling keep the camera's list.
- Notes: Module `spawnPregeneration`. The build loop has no hook and only knows sections inside the camera frustum.

#### PacketProcessorPregenerationMixin, ClientLevelPregenerationMixin
- Path: `src/main/java/com/aryston/aervium/mixin/PacketProcessorPregenerationMixin.java`, `src/main/java/com/aryston/aervium/mixin/ClientLevelPregenerationMixin.java`
- Role: During the terrain wait, handle queued client packets for at most 10 ms and queued client light updates for at most 6 ms per frame, in order, and leave the rest for the next frame; `ClientLevelPregenerationMixin` also reports the queued light updates.
- Notes: Module `spawnPregeneration`. A single player server sends every pending chunk in one batch, and vanilla's `pollLightUpdates` runs the whole queue once it holds 1000 updates, which froze the loading screen for up to 815 ms at render distance 32.

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

#### LevelRandomAccessor
- Path: `src/main/java/com/aryston/aervium/mixin/LevelRandomAccessor.java`
- Role: Setter for the protected random tick position generator (`randValue`) of `Level`, used by `ParityHarness` to make random ticks repeatable.
- Notes: Not tied to a module; it only adds an accessor.

#### CrashReportMixin
- Path: `src/main/java/com/aryston/aervium/mixin/CrashReportMixin.java`
- Role: Adds the "Aervium" entry from `CrashReportSection` to the system report once per crash report, at the start of `getDetails`.
- Notes: Not tied to a module.

#### ChunkStatusTasksTerrainMixin
- Path: `src/main/java/com/aryston/aervium/mixin/ChunkStatusTasksTerrainMixin.java`
- Role: While `WorldgenCheck` records, chains `WorldgenCheck.terrainBuilt` onto the future `ChunkStatusTasks.buildTerrain` returns.
- Notes: Not tied to a module; outside the check it only reads a flag.

#### ThreadedLevelLightEngineTimingMixin
- Path: `src/main/java/com/aryston/aervium/mixin/ThreadedLevelLightEngineTimingMixin.java`
- Role: Times every `ThreadedLevelLightEngine.runUpdate` batch, wall and CPU, for `ServerLightTimings`.
- Notes: Not tied to a module; two clock reads per batch.

#### UberGpuBufferHeapTraceMixin
- Path: `src/main/java/com/aryston/aervium/mixin/UberGpuBufferHeapTraceMixin.java`
- Role: Counts terrain buffer heaps created and destroyed in `UberGpuBuffer.uploadStagedAllocations` for `FrameTrace`.
- Notes: Measurement only, always on. Heaps are allocated inside the upload with no hook.

#### SectionRenderDispatcherTimingMixin
- Path: `src/main/java/com/aryston/aervium/mixin/SectionRenderDispatcherTimingMixin.java`
- Role: Times the lock call in `SectionRenderDispatcher.lock` and the whole of `uploadTerrainBuffersToGpu` for `ChunkUploadTimings`.
- Notes: Not tied to a module, so the numbers exist with every module on or off.
