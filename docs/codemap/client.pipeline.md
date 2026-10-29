# Kinetic Code Map: `com.aryston.kinetic.client.pipeline`

Part of the [Kinetic code map](../CODEMAP.md). Every class of this package has its entry here and nowhere else (rule D1).

### `com.aryston.kinetic.client.pipeline`

#### ShaderCaches
- Path: `src/main/java/com/aryston/kinetic/client/pipeline/ShaderCaches.java`
- Role: Holds the SPIR-V cache (`<game>/kinetic/cache/spirv/`) and the Vulkan pipeline cache (`<game>/kinetic/cache/vulkan-pipelines.bin`) for the mixins, counts pipelines compiled late on the render thread (logs every one over 1 ms), writes the startup report (time to the first screen, shader hits and misses, pipeline creation time) and saves the pipeline cache on shutdown.
- Depends on: `SpirvCache`, `VulkanPipelineCache`, `DiskCache`, `CacheStatistics`, `JobSystem`.

#### SpirvCache
- Path: `src/main/java/com/aryston/kinetic/client/pipeline/SpirvCache.java`
- Role: Returns cached SPIR-V for a GLSL compilation when the key matches, otherwise compiles with shaderc and writes the result on a worker. Cached binaries are copied into native memory the way vanilla allocates them, so vanilla frees them as usual.
- Depends on: `ShaderCacheKey`, `SpirvBinary`, `DiskCache`, `CacheStatistics`, `JobSystem`.

#### VulkanPipelineCache
- Path: `src/main/java/com/aryston/kinetic/client/pipeline/VulkanPipelineCache.java`
- Role: One `VkPipelineCache` for every graphics pipeline vanilla creates, created on first use from the saved data when the header matches the GPU and driver, saved and destroyed on shutdown.
- Depends on: `PipelineCacheHeader`, `CacheStatistics`.
- Notes: Vulkan pipeline caches are internally synchronized, so pipelines compiled on several workers share it safely.

#### PipelineWarmup
- Path: `src/main/java/com/aryston/kinetic/client/pipeline/PipelineWarmup.java`
- Role: When vanilla installs a new pipeline cache after a resource reload, compiles on the job system every built-in pipeline of `RenderPipelines` that vanilla did not register (variants that share a location, such as the three `pipeline/item_cutout` pipelines) and inserts them on the main thread through the budgeted `MainThreadQueue`, so they are never compiled in the middle of a game frame.
- Depends on: `PipelineCacheAccessor`, `JobSystem`, `MainThreadQueue`.
