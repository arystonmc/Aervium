# Aervium Code Map: `com.aryston.aervium.mixin` (terrain-data)

Part of the [Aervium code map](../CODEMAP.md). The package is split by topic; this part covers compact vertices, chunk ownership and decoding, distant terrain. Every class of the package has its entry in exactly one of these files (rule D1).

### `com.aryston.aervium.mixin`

#### SectionRenderDispatcherOwnershipMixin, CompileTaskOwnershipMixin, ResortTransparencyTaskOwnershipMixin, RenderSectionOwnershipMixin, RenderSectionAccessor
- Path: `src/main/java/com/aryston/aervium/mixin/SectionRenderDispatcherOwnershipMixin.java`, `src/main/java/com/aryston/aervium/mixin/CompileTaskOwnershipMixin.java`, `src/main/java/com/aryston/aervium/mixin/ResortTransparencyTaskOwnershipMixin.java`, `src/main/java/com/aryston/aervium/mixin/RenderSectionOwnershipMixin.java`, `src/main/java/com/aryston/aervium/mixin/RenderSectionAccessor.java`
- Role: Apply `SectionOwnership`: the dispatcher's `lock` on the render thread becomes a `tryLock` for the upload, the upload runs only while the lock is held and first releases the retired meshes, `dispose` releases them too; compile tasks on workers retire meshes instead of releasing them under the lock; both task kinds park instead of `Thread.onSpinWait` while the staging buffer is full; `RenderSection.reset` on the render thread skips the lock. The accessor exposes `releaseSectionMesh`.
- Notes: Module `lockFreeSections` (accessor always). Vanilla shares one lock between eight mesh workers and the render thread for the draw list, every section reset on a chunk crossing and the upload; a worker preempted while holding it stalled the render thread for up to 26 ms. No API covers chunk section memory.

#### ClientPacketListenerChunkDecodeMixin, ClientChunkCacheChunkDecodeMixin, LevelChunkChunkDecodeMixin, ClientboundLevelChunkPacketDataDecodeMixin, ChunkAccessLightSourcesAccessor
- Path: `src/main/java/com/aryston/aervium/mixin/ClientPacketListenerChunkDecodeMixin.java`, `src/main/java/com/aryston/aervium/mixin/ClientChunkCacheChunkDecodeMixin.java`, `src/main/java/com/aryston/aervium/mixin/LevelChunkChunkDecodeMixin.java`, `src/main/java/com/aryston/aervium/mixin/ClientboundLevelChunkPacketDataDecodeMixin.java`, `src/main/java/com/aryston/aervium/mixin/ChunkAccessLightSourcesAccessor.java`
- Role: Apply `ChunkPacketDecoding`: `handleLevelChunkWithLight` starts the decode on its first, off-thread call (before vanilla moves the packet to the main thread); `ClientChunkCache.replaceWithPacketData` marks the fill of a newly created chunk; `LevelChunk.replaceWithPacketData` then copies the decoded sections in, skips `LevelChunkSection.read` and sets the decoded sky light sources instead of `initializeLightSources`. The packet data mixin holds the pending result; the accessor sets `ChunkAccess.skyLightSources`.
- Notes: Module `chunkPacketDecoding` (packet data mixin and accessor always). No API runs work between receiving a chunk packet and applying it.

#### RenderPipelineBuilderCompactMixin
- Path: `src/main/java/com/aryston/aervium/mixin/RenderPipelineBuilderCompactMixin.java`
- Role: At `RenderPipeline.Builder.build`, gives the vanilla solid and cutout terrain pipelines the compact vertex format, define and vertex shader (`CompactTerrainFormat.apply`).
- Notes: Module `compactVertices`. No API: the chunk buffers take their vertex size from `ChunkSectionLayer.pipeline(false)`, a static vanilla pipeline built in the static setup of `RenderPipelines`; NeoForge pipeline modifiers only swap pipelines at draw time, after the buffers were sized. Helion's geometry pipelines inherit the change through `toBuilder()`.

#### SectionCompilerCompactMixin
- Path: `src/main/java/com/aryston/aervium/mixin/SectionCompilerCompactMixin.java`
- Role: In `SectionCompiler.getOrBeginLayer`, builds compact layers in the vanilla block format (`CompactMeshUpload.buildFormat`), because the vertex consumers only write the block format.
- Notes: Module `compactVertices`. No API for the builder format of a chunk layer.

#### CompileTaskCompactMixin
- Path: `src/main/java/com/aryston/aervium/mixin/CompileTaskCompactMixin.java`
- Role: Wraps the upload of each layer in `SectionRenderDispatcher.RenderSection.CompileTask.doTask` and hands it the compact view of the mesh (`CompactMeshUpload.uploadView`).
- Notes: Module `compactVertices`. Runs after `CompiledSectionMesh` was created, so face culling has sorted the float positions already. No API between the mesh build and the upload.

#### MeshDataCompactMixin
- Path: `src/main/java/com/aryston/aervium/mixin/MeshDataCompactMixin.java`
- Role: Adds `CompactMeshHolder` to `MeshData`: whether its vertices were encoded already.
- Notes: Module `compactVertices`.

#### ChunkSectionsToRenderCompactMixin
- Path: `src/main/java/com/aryston/aervium/mixin/ChunkSectionsToRenderCompactMixin.java`
- Role: In `ChunkSectionsToRender.renderGroup`, replaces the wireframe debug pipelines of the opaque group with `CompactWireframe`.
- Notes: Module `compactVertices`. The override pipelines are chosen inside `renderGroup` with no hook.

#### CameraDistantTerrainMixin
- Path: `src/main/java/com/aryston/aervium/mixin/CameraDistantTerrainMixin.java`
- Role: Extends the camera far plane to 1.5 times the distant terrain distance while distant terrain draws.
- Notes: Module `distantTerrain`, renderer module. No camera far-plane API exists.

#### LevelRendererDistantTerrainMixin
- Path: `src/main/java/com/aryston/aervium/mixin/LevelRendererDistantTerrainMixin.java`
- Role: Draws distant terrain at the return of `LevelRenderer.executeSolid` in the vanilla main pass.
- Notes: Module `distantTerrain`, renderer module. No event exposes this open vanilla render pass.

#### HelionDistantTerrainMixin
- Path: `src/main/java/com/aryston/aervium/mixin/HelionDistantTerrainMixin.java`
- Role: `@Pseudo` Helion hook for opaque LOD after `renderOpaque` and `renderOpaqueGeometry`; water is submitted after deferred lighting through its own optional hook.
- Notes: Module `distantTerrain`, renderer module. Helion offers no hook, so this preserves Helion lighting, TAA jitter and fog.

#### HelionDistantWaterMixin
- Path: `src/main/java/com/aryston/aervium/mixin/HelionDistantWaterMixin.java`
- Role: Optional one-target water pass at the start of Helion's transparent stage, after deferred opaque lighting, including its order-independent path.
- Notes: Module `distantTerrain`; `@Pseudo`, `require = 0`. No Helion event exposes this stage; no Helion classes are linked by the adapter.

#### RenderSectionDistantTerrainMixin
- Path: `src/main/java/com/aryston/aervium/mixin/RenderSectionDistantTerrainMixin.java`
- Role: Reports `RenderSection.setSectionMesh` completion and reset to the distant coverage mask.
- Notes: Module `distantTerrain`. No NeoForge event exposes uploaded section replacement/reset; hooks only enqueue coordinate/readiness values.

#### SectionOcclusionGraphDistantTerrainMixin
- Path: `src/main/java/com/aryston/aervium/mixin/SectionOcclusionGraphDistantTerrainMixin.java`
- Role: Removes vanilla's vertical render-distance limit from the section graph while distant terrain draws, so real terrain below a camera high above it is still drawn instead of leaving a hole where distant terrain is clipped.
- Notes: Module `distantTerrain`.

#### SectionUpdateTrackerDistantTerrainMixin
- Path: `src/main/java/com/aryston/aervium/mixin/SectionUpdateTrackerDistantTerrainMixin.java`
- Role: Queues client chunk recapture from section dirtiness, including block and light updates.
- Notes: Module `distantTerrain`. Chunk load events do not cover later edits; there is no general equivalent notification API.
