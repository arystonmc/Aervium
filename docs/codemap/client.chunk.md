# Kinetic Code Map: `com.aryston.kinetic.client.chunk`

Part of the [Kinetic code map](../CODEMAP.md). Every class of this package has its entry here and nowhere else (rule D1).

### `com.aryston.kinetic.client.chunk`

#### SectionSliceHolder
- Path: `src/main/java/com/aryston/kinetic/client/chunk/SectionSliceHolder.java`
- Role: Interface `CompiledSectionMeshMixin` adds to every compiled chunk section mesh: one cached terrain buffer slice per layer, or `ABSENT` when the mesh has no buffer for that layer.
- Depends on: nothing.

#### SectionDrawRecord, LayerDraw
- Path: `src/main/java/com/aryston/kinetic/client/chunk/SectionDrawRecord.java`, `src/main/java/com/aryston/kinetic/client/chunk/LayerDraw.java`
- Role: Everything `extractSectionDrawGroups` computes for one section mesh, prepared once. The numbers the draw list reads every frame (layer, group hash, index count, first index, base vertex, flags and the packed `FaceRanges` of each drawable layer, up to three slots in layer order) and the section origin sit in one `int[]` whose header also holds the mesh version, a valid flag, the section origin and the slot count. `RenderSectionMixin` keeps that array on the section next to the record, so a frame reads the section and one array instead of up to ten objects; the `LayerDraw` objects with the buffers are only read when a draw group is created.
- Notes: Valid while the section's mesh version (`RenderSectionMeshVersionMixin`) is the one it was built for and no allocation of that mesh was freed; `CompiledSectionMeshMixin` invalidates it (clears the valid flag in the array) together with the cached slices. Moving the header into the array raised the 1% low of the redstone scene by 3 to 4 % (2026-10-16). A mesh's draws never change while it is shown (they are filled in its constructor and cleared only when it is released).

#### BlockEntitySectionHolder
- Path: `src/main/java/com/aryston/kinetic/client/chunk/BlockEntitySectionHolder.java`
- Role: Interface `RenderSectionBlockEntityMixin` adds to `SectionRenderDispatcher.RenderSection`: whether the section's mesh may hold renderable block entities.
- Depends on: nothing.

#### SectionDrawRecordHolder
- Path: `src/main/java/com/aryston/kinetic/client/chunk/SectionDrawRecordHolder.java`
- Role: Interface `RenderSectionMixin` adds to `SectionRenderDispatcher.RenderSection` to keep its last `SectionDrawRecord` and that record's packed array.
- Depends on: `SectionDrawRecord`.

#### MeshVersionHolder
- Path: `src/main/java/com/aryston/kinetic/client/chunk/MeshVersionHolder.java`
- Role: Interface `RenderSectionMeshVersionMixin` adds to `SectionRenderDispatcher.RenderSection` to keep the section's current mesh version.
- Depends on: nothing inside the mod.

#### SectionDrawLists
- Path: `src/main/java/com/aryston/kinetic/client/chunk/SectionDrawLists.java`
- Role: Same result as vanilla `LevelRenderer.extractSectionDrawGroups` (section infos, draw groups by buffer hash, translucent order, largest index count) built from the section records. Every draw group gets a pooled `PackedDraws` list, and in a multi-draw pass the section infos go into `DirectDraws` as plain numbers instead of records, so a frame allocates no object per section or draw. In the camera pass (`FaceCulling.cameraPass`) a layer with `FaceRanges` gets one draw per run of facing groups that can face the camera instead of one draw for the whole layer; empty groups join runs, so a section needs at most four draws per layer. Counts drawn and skipped quads for `CullingStatistics`.
- Depends on: `FaceCulling`, `FaceRanges`, `CullingStatistics`, `SectionDrawRecord`, `SectionDrawRecordHolder`, `SectionSliceHolder`, `TerrainOcclusion` (skips sections the last finished camera pass hid); the access transformer for `LevelRenderer$ChunkDrawGroup`.
- Notes: In the cave scene it cut the share of the render thread spent building draw lists from about 42 % to 28 %; the frame rate did not change because that scene waits on the GPU (`VulkanCommandEncoder.awaitSubmitCompletion`), so the gain shows on CPU-bound PCs.

#### PackedDraws, PackedDrawPool
- Path: `src/main/java/com/aryston/kinetic/client/chunk/PackedDraws.java`, `src/main/java/com/aryston/kinetic/client/chunk/PackedDrawPool.java`
- Role: `PackedDraws` is the draw list of one draw group: a `List<IndexedDraw>` that stores the five numbers of every indirect draw command in one `int[]`, copies them into mapped memory in one block (or backwards for the reversed view vanilla asks for translucent layers) and builds `IndexedDraw` records only when vanilla code reads an element (the path without multi-draw). `PackedDrawPool` hands out the lists and reuses them in the next pass, because vanilla reads a group's list only inside the `prepareChunkRenders` call that built it.
- Depends on: nothing.

#### PackedSectionInfos, SectionInfoStorage, DrawCommandRing
- Path: `src/main/java/com/aryston/kinetic/client/chunk/PackedSectionInfos.java`, `src/main/java/com/aryston/kinetic/client/chunk/SectionInfoStorage.java`, `src/main/java/com/aryston/kinetic/client/chunk/DrawCommandRing.java`
- Role: `PackedSectionInfos` holds the origin and fade of every drawn section of a pass as four ints. `SectionInfoStorage` uploads them like vanilla's instanced section buffer (one device buffer, a staging copy per pass, `writeToBuffer`). `DrawCommandRing` writes the indirect commands into a mapped ring of three buffers like vanilla's command buffer. Both grow by doubling, keep a buffer they replaced until the frame ends and start over at `FlipFrameEvent`.
- Depends on: `PackedDraws`.

#### GpuSections
- Path: `src/main/java/com/aryston/kinetic/client/chunk/GpuSections.java`
- Role: The per-section step of the GPU path inside `SectionDrawLists`: uploads a section's solid and cutout slots to the GPU section table when its mesh version or the table epoch changed, lists the section as visible, and tells the draw list which slots stay on the CPU (none, the translucent ones, or all of them when a slot does not fit the table).
- Depends on: `GpuCulling`, `GpuSectionEntry`, `SectionDrawRecord`, `SectionDrawRecordHolder`, `SectionDrawLists`.

#### DirtySectionMirror, DirtySectionIndex
- Path: `src/main/java/com/aryston/kinetic/client/chunk/DirtySectionMirror.java`, `src/main/java/com/aryston/kinetic/client/chunk/DirtySectionIndex.java`
- Role: Interfaces the `dirtySections` mixins add to `SectionUpdateTracker.SectionDirtyState` (attach the shared dirty array and the storage index) and to `SectionUpdateTracker` (whether a section node may be dirty, by its storage index).
- Depends on: nothing.

#### DirectDraws
- Path: `src/main/java/com/aryston/kinetic/client/chunk/DirectDraws.java`
- Role: Entry point of the module `directDraws`: marks whether the current `prepareChunkRenders` call is a multi-draw pass, hands `SectionDrawLists` its pooled lists and packed section infos, writes them for vanilla's `writeChunkSectionCommands` and `writeChunkSectionsInstanced` calls, and falls back to vanilla for any list that is not packed (for example with `sectionSliceCache` off). `endFrame` runs at `FlipFrameEvent`.
- Depends on: `PackedDraws`, `PackedDrawPool`, `PackedSectionInfos`, `SectionInfoStorage`, `DrawCommandRing`.

#### FaceCulling
- Path: `src/main/java/com/aryston/kinetic/client/chunk/FaceCulling.java`
- Role: Sorts every solid and cutout layer of a new section mesh by facing (`FacingSort`) when the layer uses the vanilla block vertex format and the shared quad index buffer, and marks a `prepareChunkRenders` call as the camera pass when its matrix equals the camera's view rotation, so Helion's shadow cascades and other views keep drawing every face.
- Notes: Module `faceCulling`. Needs `sectionSliceCache` too, because the faces are skipped in `SectionDrawLists`. Translucent layers keep vanilla's sorted order. A skipped face is one the GPU would cull as a back face anyway, so the picture is unchanged; the gain is vertex work. Benchmark on lavapipe: 35 % of solid quads skipped in the village and redstone scenes, 49 % in the caves.
- Depends on: `FacingSort`, `FaceRanges`, `SectionLayers`.

#### FaceRangesHolder
- Path: `src/main/java/com/aryston/kinetic/client/chunk/FaceRangesHolder.java`
- Role: Interface `CompiledSectionMeshFacingMixin` adds to `CompiledSectionMesh`: the `FaceRanges` of a layer, or none.
- Depends on: `FaceRanges`.

#### LeafCulling
- Path: `src/main/java/com/aryston/kinetic/client/chunk/LeafCulling.java`
- Role: Optional leaf culling: a leaf face toward another leaf is skipped when the block behind that leaf is a leaf too, so only faces two or more leaves deep inside a tree are dropped. Switching it rebuilds every chunk through `LevelExtractor.allChanged` on the main thread.
- Notes: Off by default because it changes the look slightly (inner faces show through the gaps of detailed leaves); with fast leaves vanilla already drops every face between leaves. Runs on the chunk build workers.
- Depends on: nothing.

#### ChunkUploadTimings
- Path: `src/main/java/com/aryston/kinetic/client/chunk/ChunkUploadTimings.java`
- Role: Times vanilla's terrain buffer upload and how long the render thread waits for the chunk builder lock (workers hold it while they copy a finished mesh into the staging buffer); feeds `SystemTimings` for the overlay and two `StallMeter`s for the benchmark log.
- Notes: Measurement only. It decides whether uploads or the lock need their own work: vanilla only resets its linear staging buffer once every staged mesh is uploaded, so a plain per-frame upload budget would starve it.
- Depends on: `StallMeter`, `SystemTimings`.

#### MeshWorkers
- Path: `src/main/java/com/aryston/kinetic/client/chunk/MeshWorkers.java`
- Role: Owns the `Kinetic Mesh` job system (two cores left to the game threads, at most eight workers, one priority step below normal) and the `TracingExecutor` vanilla's chunk build dispatcher runs on instead of the shared background pool; gives meters for the overlay and the benchmark.
- Notes: In single player the shared background pool also generates, loads and saves chunks for the integrated server, so chunk builds waited behind world generation while flying; vanilla still picks the nearest section first. At normal priority, eight workers waking together whenever the camera entered a new section kept the graphics driver and the window compositor off the processor, and the next frame waited 15 to 25 ms for a swapchain image; one step below normal took those waits underground from 22 to 28 per scene to 4 to 9 and raised the 0.1% low there from 100 to 128 to 192 to 203 FPS at the same build rate (2026-10-17).
- Depends on: `JobSystem`, `JobLoadMeter`.

#### CompactTerrainFormat
- Path: `src/main/java/com/aryston/kinetic/client/chunk/CompactTerrainFormat.java`
- Role: The 16-byte compact terrain vertex format (`PositionLight` RGBA16_UINT, `Color` RGBA8_UNORM, `PackedUV0` RG16_UINT), the shader define contract `KINETIC_COMPACT_VERTICES=1`, the vanilla pipelines that get it (solid and cutout terrain, direct and multi-draw) and `apply`, which gives a pipeline builder the format, the define and the vertex shader `kinetic:terrain/compact`.
- Notes: Module `compactVertices`. Loaded inside the static setup of `RenderPipelines`, so it touches no chunk class (lesson L-087). Translucent terrain keeps the block format, because vanilla sorts translucent quads from their float positions.
- Depends on: nothing of this mod.

#### CompactMeshUpload, CompactMeshHolder
- Path: `src/main/java/com/aryston/kinetic/client/chunk/CompactMeshUpload.java`, `src/main/java/com/aryston/kinetic/client/chunk/CompactMeshHolder.java`
- Role: `buildFormat` keeps chunk builds in the vanilla block format (so face sorting and NeoForge geometry renderers see vanilla data); `uploadView` encodes a compact layer's mesh once (`CompactMeshHolder` marks the `MeshData`, because vanilla retries the upload while the staging buffer is full) and hands the upload a view of 16 bytes per vertex. Warns once per session when a mesh had vertices outside the compact range.
- Depends on: `CompactTerrainFormat`, `CompactVertexEncoder`.

#### CompactWireframe
- Path: `src/main/java/com/aryston/kinetic/client/chunk/CompactWireframe.java`
- Role: Compact copies of the vanilla wireframe debug pipelines, used for the opaque layer group so the wireframe view still reads compact meshes.
- Depends on: `CompactTerrainFormat`.

#### SectionOwnership
- Path: `src/main/java/com/aryston/kinetic/client/chunk/SectionOwnership.java`
- Role: Module `lockFreeSections`: the render thread owns the terrain buffer allocations. Meshes that mesh workers release (empty result, cancelled build) wait in a lock-free queue and the render thread releases them right before the next upload; the render thread takes the section dispatcher lock only for the upload, with `tryLock`, and waits for it only after four skipped frames in a row; workers that find the staging buffer full park for 0.2 ms instead of spinning.
- Notes: Workers still stage a whole layer inside one hold of the lock, and an upload needs the same lock, so a mesh the render thread resets can never be published after the upload that clears its skip mark. On 2026-10-16 the longest lock wait of the render thread fell from 19.5 ms to 0 and the 1% low rose by 12 % at render distance 12.
- Depends on: `RenderSectionAccessor`, `ChunkUploadTimings`, `FrameTrace`, `MixinModules`.

#### ChunkPacketDecoding, DecodedChunk, DecodedChunkHolder
- Path: `src/main/java/com/aryston/kinetic/client/chunk/ChunkPacketDecoding.java`, `src/main/java/com/aryston/kinetic/client/chunk/DecodedChunk.java`, `src/main/java/com/aryston/kinetic/client/chunk/DecodedChunkHolder.java`
- Role: Module `chunkPacketDecoding`: when a chunk packet reaches the client off the main thread, two `Kinetic Chunk Decode` workers (below normal priority) read its block and biome sections into new `LevelChunkSection` objects and compute the sky light sources of the columns through a `ProtoChunk` with those sections. When the main thread builds a new chunk from the packet and the result is ready, the chunk takes the decoded sections and sky light sources instead of reading the packet again; block entities, heightmaps and everything else run as in vanilla. A chunk that already existed, a result that is not ready or that belongs to another level, and a failed decode all take the vanilla path. `DecodedChunkHolder` keeps the pending result on the packet data.
- Notes: Both steps only read the packet's own bytes and the chunk's own sections, so the chunk is the same as vanilla's. In the slowest frames of the benchmark the chunk packet handling was 21 to 29 % of the render thread time, about three quarters of it reading sections and finding the sky light sources. With the module on, 89 % of new chunks were decoded ahead, `-Dkinetic.chunkDecodeAudit=true` found 0 of 1.4 million columns different from vanilla, the elytra 1% low rose by about 6 % and slow frames caused by the main thread in the elytra flight fell by a third in alternating runs (2026-10-17).
- Depends on: `JobSystem`, `ChunkAccessLightSourcesAccessor`.

#### SectionLayers
- Path: `src/main/java/com/aryston/kinetic/client/chunk/SectionLayers.java`
- Role: One shared `ChunkSectionLayer.values()` array.
- Notes: Lives in its own class so the enum is first loaded at the first frame; a static field in a mixin would load it inside the static setup of `LevelRenderer`, before `RenderPipelines` exists, and every layer would get a null pipeline.
