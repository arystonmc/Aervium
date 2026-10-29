# Kinetic Code Map: `com.aryston.kinetic.client.gpu`

Part of the [Kinetic code map](../CODEMAP.md). Every class of this package has its entry here and nowhere else (rule D1).

### `com.aryston.kinetic.client.gpu`

#### GpuCulling
- Path: `src/main/java/com/aryston/kinetic/client/gpu/GpuCulling.java`
- Role: The advanced terrain path of module `gpuCulling`. In the camera pass the CPU only lists the visible sections that occlusion culling kept (section index and fade) in a mapped ring; the GPU section table holds each section's solid and cutout draws; a compute shader (`shaders/culling/terrain_draws.csh`) applies face culling against the camera, writes the section instance data and appends the draw commands of each draw group with an atomic counter, and the terrain layers draw them with `vkCmdDrawIndexedIndirectCount` (`ChunkSectionsToRenderIndirectCountMixin`). Translucent layers, Helion's shadow cascades and every other pass keep the CPU draw list. Checks the device once the game has loaded (`warmUp`: Vulkan device, `drawIndirectCount` enabled through `IndirectCountFeature`) and compiles its compute pipeline on a job system worker; until the pipeline is ready the CPU path draws, so the first world frame never waits for shaderc (that compile cost the first frame of every join about 300 ms). Devices without the feature stay on the CPU path (the baseline path) with one log line.
- Notes: One GPU pass per frame. The dispatch is recorded into the frame's command buffer when the section infos are written, between full memory barriers (`VulkanCommandEncoder.memoryBarrier`), after the table uploads and a fill of the draw counts. A group gets room for four draws per visible section (face runs), so no draw can be lost.
- Depends on: `GpuSectionTable`, `GpuSectionEntry`, `TerrainGroups`, `CullPipeline`, `GpuDrawList`, `VulkanAccess`, `GpuBufferUsage`, `FrontendRenderPassAccessor`, `VulkanRenderPassAccessor`, `MixinModules`.

#### GpuSectionTable, GpuSectionEntry
- Path: `src/main/java/com/aryston/kinetic/client/gpu/GpuSectionTable.java`, `src/main/java/com/aryston/kinetic/client/gpu/GpuSectionEntry.java`
- Role: The device buffer with one 36-int entry per render section index (origin, slot count, and per solid or cutout slot: draw group, index count, first index, base vertex, face flag, the eight face run boundaries as 16-bit pairs and the seven face planes). Entries that changed are written into a mapped staging ring during the draw list and copied in one `vkCmdCopyBuffer` before the dispatch, so frames in flight never see a half-updated entry. A larger table starts a new epoch, which marks every section stale.
- Depends on: `VulkanAccess`, `GpuBufferUsage`.

#### TerrainGroups, GpuDrawList
- Path: `src/main/java/com/aryston/kinetic/client/gpu/TerrainGroups.java`, `src/main/java/com/aryston/kinetic/client/gpu/GpuDrawList.java`
- Role: `TerrainGroups` gives every (layer, vertex buffer, index buffer, index type) a small id that the table stores, drops groups whose buffers were closed and ranks the live ones for the frame. `GpuDrawList` is the draw list vanilla sees for such a group: its size is the group's command capacity and its commands come from the GPU.
- Depends on: nothing.

#### CullPipeline, VulkanAccess, IndirectCountFeature, GpuBufferUsage
- Path: `src/main/java/com/aryston/kinetic/client/gpu/CullPipeline.java`, `src/main/java/com/aryston/kinetic/client/gpu/VulkanAccess.java`, `src/main/java/com/aryston/kinetic/client/gpu/IndirectCountFeature.java`, `src/main/java/com/aryston/kinetic/client/gpu/GpuBufferUsage.java`
- Role: `CullPipeline` compiles the compute shader with shaderc and creates the push-descriptor set layout, pipeline layout and compute pipeline, and records one dispatch. `VulkanAccess` reaches the Vulkan device, its enabled features, the frame's command buffer and buffer handles. `IndirectCountFeature` is the optional device feature set `VulkanFeatureSetsMixin` adds. `GpuBufferUsage.STORAGE` is an extra buffer usage bit that `VulkanConstStorageMixin` maps to Vulkan storage buffer usage.
- Depends on: `FrontendGpuDeviceAccessor`, `VulkanDeviceAccessor`, `VulkanCommandEncoderAccessor`.

#### GpuStageTimer, RenderPassTimingHolder
- Path: `src/main/java/com/aryston/kinetic/client/gpu/GpuStageTimer.java`, `src/main/java/com/aryston/kinetic/client/gpu/RenderPassTimingHolder.java`
- Role: GPU timestamps per named stage (frame graph passes, render passes, main pass segments, the whole frame and the rendering before the swapchain copy) in a timestamp query pool with four frames in flight; results are read without waiting when a slot comes round again and summed in `StageTimes`. Only on with `-Dkinetic.benchmark.gpuTimings=true`, so normal benchmark reports stay comparable. `RenderPassTimingHolder` lets a render pass carry its open timestamp pair until it closes.
- Depends on: `StageTimes`, `FrameTrace`, `VulkanAccess`, `VulkanQueryPoolAccessor`.

#### MainPassTimings
- Path: `src/main/java/com/aryston/kinetic/client/gpu/MainPassTimings.java`
- Role: Splits the main pass into opaque terrain, solid entities and block entities, the classic translucent segments and improved transparency (whole and its terrain part) for `GpuStageTimer`.
- Depends on: `GpuStageTimer`.

#### SwapchainImages
- Path: `src/main/java/com/aryston/kinetic/client/gpu/SwapchainImages.java`
- Role: Module `swapchainImages`: asks the swapchain for two more images than vanilla (within the surface's maximum). With vsync off the presentation engine holds images now and then for up to about 25 ms; with only three images the GPU then waits for an image before its swapchain copy, and the render thread waits for that frame. Extra images only add slack; frames are presented exactly as before.
- Depends on: `Kinetic`.
