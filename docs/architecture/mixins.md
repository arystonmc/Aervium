# Kinetic Architecture: Mixins

Part of the [Kinetic architecture](../ARCHITECTURE.md).

## Mixins

| Mixin | Target | Why no API |
|---|---|---|
| `FrontendGpuDeviceAccessor` | `FrontendGpuDevice.backend` | The Vulkan memory allocator and its heap budgets are not exposed by renderpearl. |
| `GlslCompilerMixin` | `GlslCompiler.compileToSpv` | No way to supply compiled shaders. |
| `VulkanRenderPipelineMixin` | `vkCreateGraphicsPipelines` in `VulkanRenderPipeline.compile` | Vanilla passes no pipeline cache and offers no hook. |
| `PipelineCacheMixin`, `PipelineCacheAccessor` | `PipelineCache.get`, its fields | Late compiles are invisible otherwise; the warm-up needs the device and shader source of the cache. |
| `RenderSystemMixin` | `RenderSystem.setCurrentPipelineCache` | No event after the shader manager installs its pipelines. |
| `FramerateLimitTrackerMixin` | `FramerateLimitTracker.getFramerateLimit` | The frame limit has no API. |
| `DeltaTrackerTimerPrecisionMixin` | `DeltaTracker$Timer.advanceGameTime` and `advanceRealTime` | No API replaces the millisecond time vanilla feeds to the timer. |
| `PalettedContainerMixin` | `PalettedContainer` threading check | The check is a private field of every container. |
| `DataLayerStorageMapMixin`, `BlockDataLayerStorageMapMixin`, `SkyDataLayerStorageMapMixin` | `DataLayerStorageMap` writes, `copy()` of the block and sky light maps | The visible light map is always a full clone and the storage has no hook for how it is copied. |
| `VisGraphMixin`, `VisibilitySetMixin`, `CompiledSectionMeshVisibilityAccessor` | `VisGraph.resolve`, `VisibilitySet`, `CompiledSectionMesh.visibilitySet` | The opaque blocks of a section are only known inside the mesh build. |
| `LevelRendererOcclusionMixin` | `LevelRenderer.prepareChunkRenders` and `prepareChunkRendersIndirect` | No event tells which view a terrain pass is for. |
| `LevelExtractorOcclusionMixin` | `LevelExtractor.extractVisibleEntities`, `isEntityVisible`, block entity extraction | Entity visibility is only the frustum check; no event can drop an entity or block entity from a frame. |
| `QuadParticleGroupOcclusionMixin`, `ParticleAccessor` | `QuadParticleGroup.extractRenderState`, `Particle.xo/yo/zo` | Particle extraction only checks the frustum and has no hook. |
| `LevelRendererMeshWorkersMixin` | `new SectionRenderDispatcher` in `LevelRenderer.invalidateCompiledGeometry` | The chunk build executor is hard-wired to the shared background pool. |
| `SectionStoragePoiIndexMixin`, `SectionStorageAccessor`, `PoiManagerColumnSearchMixin` | `SectionStorage` constructor and fields, `PoiManager.getInChunk` | The point of interest storage is a private map with no per-column view, and every POI search goes through `getInChunk`. |
| `EntitySectionCollidableMixin`, `PersistentEntitySectionManagerAccessor`, `TransientEntitySectionManagerAccessor`, `ServerLevelEntityCollisionsMixin`, `ClientLevelEntityCollisionsMixin` | `EntitySection.add` and `remove`, persistent and transient entity manager section storage, `getEntityCollisions` overridden in `ServerLevel` and added to `ClientLevel` | Sections have no per-kind counts, the collision query is an interface default method with no hook, and the client level has no implementation. |
| `ParticleCollisionMixin` | `Entity.collideBoundingBox` in `Particle.move` | Particle movement has no API around its block collision call. |
| `EntitySectionHopperMixin`, `ServerLevelEntityManagerAccessor`, `EntityCapabilityAccessor`, `HopperBlockEntityItemSearchMixin`, `HopperEntityContainerSearchMixin` | `EntitySection.add` and `remove`, `ServerLevel.entityManager`, NeoForge `EntityCapability.providers`, `HopperBlockEntity.getItemsAtAndAbove`, NeoForge `VanillaInventoryCodeHooks.getEntityContainerOrHandler` | Sections have no per-kind counts, and whether an entity type has an item automation provider is only visible in the capability's provider map. |
| `CrashReportMixin` | `CrashReport.getDetails` | NeoForge crash callables get no access to the exception, and the section needs its stack. |
| `ChunkStatusTasksTerrainMixin` | `ChunkStatusTasks.buildTerrain` | The worldgen check needs each chunk right after its terrain step, before features from its neighbours land in it. Does nothing outside the check. |
| `ThreadedLevelLightEngineTimingMixin` | `ThreadedLevelLightEngine.runUpdate` | The server light worker has no timing of its own. |
| `SectionRenderDispatcherTimingMixin` | `SectionRenderDispatcher.lock` and `uploadTerrainBuffersToGpu` | Upload and builder lock time are invisible otherwise. |
| `ModelBlockRendererLeafCullingMixin` | `ModelBlockRenderer.shouldRenderFace` | Leaf face culling only depends on the block state, never on the leaves around it. |
| `HelionShadowCastersMixin` (pseudo, optional) | Helion `VanillaShadowCasterSource.collectSections` | Helion exposes no hook for its caster lists yet. |
| `LevelRendererSectionSliceMixin`, `RenderSectionMixin`, `CompiledSectionMeshMixin`, `UberGpuBufferMixin` | `LevelRenderer.extractSectionDrawGroups`, `CompiledSectionMesh`, `UberGpuBuffer.freeAllocation` and `close` | The terrain buffer slices of a mesh are only reachable through a map lookup and the draw list loop has no hook. |
| `RenderPipelineBuilderCompactMixin` | `RenderPipeline.Builder.build` for the vanilla solid and cutout terrain pipelines | The chunk buffers take their vertex size from these static pipelines; NeoForge pipeline modifiers only swap pipelines at draw time. |
| `SectionCompilerCompactMixin`, `CompileTaskCompactMixin`, `MeshDataCompactMixin` | `SectionCompiler.getOrBeginLayer`, the upload call in `CompileTask.doTask`, `MeshData` | No hook between the mesh build and the upload, and the builder format of a layer is fixed. |
| `ChunkSectionsToRenderCompactMixin` | the wireframe override in `ChunkSectionsToRender.renderGroup` | The debug override pipelines are chosen inside the method. |
| `CompressionEncoderLevelMixin` | `new Deflater()` in the `CompressionEncoder` constructor | The compression level of a connection has no setting or event. |
| `ChunkStatusTasksFeaturesMixin` | `ChunkStatusTasks.generateFeatures` | The feature step is a static task of the generation pyramid and always runs on vanilla's single `worldgen` executor. |
| `LevelRendererDirectDrawMixin` | `writeChunkSectionCommands` and `writeChunkSectionsInstanced` calls in `LevelRenderer.prepareChunkRendersIndirect` | Both writes only accept lists of records, one per draw and per section. |
| `SectionDirtyStateMirrorMixin`, `SectionUpdateTrackerMirrorMixin`, `LevelExtractorDirtySectionsMixin` | `SectionUpdateTracker.SectionDirtyState` changes, the tracker constructor, the `getDirtyState` call in `LevelExtractor.extract` | The dirty states live in scattered objects behind a node wrapper and the scan over every visible section has no hook. |
| `RotatingSectionStorageEdgeMixin` | `RotatingSectionStorage.repositionCenter` | The view area walks every node when the camera enters another chunk and has no hook for moving only the edges. |
| `ChunkSectionsToRenderIndirectCountMixin` | `RenderPass.drawIndexedIndirect` calls in `ChunkSectionsToRender.DrawIndirect.render` | Renderpearl has no counted indirect draw. |
| `VulkanFeatureSetsMixin`, `VulkanConstStorageMixin` | `VulkanFeatureSets.optionalFeatureSets`, `VulkanConst.bufferUsageToVk` | Renderpearl enables only the device features vanilla uses and has no storage buffer usage. |
| `VulkanDeviceAccessor`, `VulkanCommandEncoderAccessor`, `VulkanRenderPassAccessor`, `FrontendRenderPassAccessor` | enabled features, the encoder's command buffer, a render pass's command buffer and descriptor push, the frontend pass's backend | The compute pass and the counted draw record raw Vulkan commands into renderpearl's command buffers. |
| `PrepareSpawnTaskPregenerationMixin`, `PrepareSpawnTaskPreparingMixin` | `PrepareSpawnTask.keepAlive`, `spawnPlayer` and `close`; `LevelLoadListener.start` and `update` and the player chunk `isDone` check in `PrepareSpawnTask.Preparing.tick` | The spawn ticket radius and the wait for it are fixed inside the configuration task. |
| `ServerGamePacketListenerTerrainWaitMixin` | `ServerGamePacketListenerImpl.tickClientLoadTimeout` | The client load timeout has no setting. |
| `LevelLoadTrackerPregenerationMixin` | `LevelLoadTracker.setServerChunkStatusView`, `startClientLoad`, `isLevelReady`, `serverProgress` | The loading screen closes on a fixed condition and has no event to delay it. |
| `PacketProcessorPregenerationMixin` | `PacketProcessor.processQueuedPackets` | The client handles every queued packet in one frame. |
| `ClientLevelPregenerationMixin` | `ClientLevel.pollLightUpdates` | The light queue is private and runs completely past 1000 entries. |
| `LevelExtractorPregenerationMixin` | the `LevelRenderer.visibleSections()` call in `LevelExtractor.extract` | Sections are only scheduled for compiling from the visible list. |
| `RenderSectionBlockEntityMixin`, `LevelExtractorBlockEntitiesMixin` | `setSectionMesh` and `reset` of `RenderSection`, the `getSectionMesh` call in `LevelExtractor.extractVisibleBlockEntities` | Block entity extraction walks every visible section and has no hook. |
