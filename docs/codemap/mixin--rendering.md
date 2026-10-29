# Kinetic Code Map: `com.aryston.kinetic.mixin` (rendering)

Part of the [Kinetic code map](../CODEMAP.md). The package is split by topic; this part covers draw lists, dirty sections, occlusion, face and leaf culling, mesh workers, models. Every class of the package has its entry in exactly one of these files (rule D1).

### `com.aryston.kinetic.mixin`

#### LevelRendererSectionSliceMixin
- Path: `src/main/java/com/aryston/kinetic/mixin/LevelRendererSectionSliceMixin.java`
- Role: Replaces `LevelRenderer.extractSectionDrawGroups` with `SectionDrawLists` (under the dispatcher lock, as vanilla) when the game has at most three section layers; the redirects below remain for the vanilla path. In that path it answers `getRenderSectionSlice` from the slice cached in the mesh and asks vanilla only on a miss, and replaces the `ChunkSectionLayer.values()` copy for every section with `SectionLayers.ALL`.
- Notes: Module `sectionSliceCache`, renderer module. Vanilla looks up two `HashMap` entries per section and layer in every pass (the camera and each of Helion's shadow cascades) and allocates a new slice record each time; with Helion at render distance 32 that lookup was about 27 % of the render thread. Turning the module on raised the benchmark 1% low from 36.1 to 50.9 FPS and the cave scene from 58 to 78 FPS average (ZGC, Helion and Arkea, 2026-10-12).

#### RotatingSectionStorageEdgeMixin
- Path: `src/main/java/com/aryston/kinetic/mixin/RotatingSectionStorageEdgeMixin.java`
- Role: When the camera enters another chunk column, vanilla `RotatingSectionStorage.repositionCenter` walks every node of the view area and of the section update tracker (about 100,000 each at render distance 32) to find the few that moved. The mixin compares only the grid columns: a column's sections move only when its x or z coordinate wraps, so it touches the columns at the leading and trailing edges and leaves the rest alone. The first placement and jumps of a whole grid width or more take the vanilla path.
- Notes: Module `sectionGridMoves`, renderer module. Needs the access transformer entry for `RotatingSectionStorage$Node`. In the slowest frames of the benchmark the vanilla walk was 5 % of the render thread against 0.4 % in normal frames; with this module and 1024 light snapshot shards the 1% low rose by 6 % and the elytra 1% low from 115 to 126 to 132 FPS (2026-10-16).

#### SectionDirtyStateMirrorMixin, SectionUpdateTrackerMirrorMixin, LevelExtractorDirtySectionsMixin
- Path: `src/main/java/com/aryston/kinetic/mixin/SectionDirtyStateMirrorMixin.java`, `src/main/java/com/aryston/kinetic/mixin/SectionUpdateTrackerMirrorMixin.java`, `src/main/java/com/aryston/kinetic/mixin/LevelExtractorDirtySectionsMixin.java`
- Role: Every frame vanilla `LevelExtractor.extract` asks the `SectionUpdateTracker` for the dirty state of every visible section, two scattered objects per section (the storage node and the state). The tracker mixin gives each state its storage index and one shared `boolean[]` when the tracker is created (`DirtySectionMirror`), the state mixin copies `isDirty` into that array after `setDirty`, `setNotDirty` and `setSectionNode`, and the extractor mixin answers clean sections from the array (`DirtySectionIndex`) without touching their state; dirty or unknown sections take the vanilla lookup.
- Notes: Module `dirtySections`, renderer module. In the cave scene the lookup was 17 % of the render thread (thousands of visible sections behind cave walls). The tracker iterates its storage with `forEach`, because the storage's own iterator skips the last value.

#### ChunkSectionsToRenderIndirectCountMixin
- Path: `src/main/java/com/aryston/kinetic/mixin/ChunkSectionsToRenderIndirectCountMixin.java`
- Role: Sends every indirect terrain draw of `ChunkSectionsToRender.DrawIndirect.render` through `GpuCulling.drawIndexedIndirect`, which draws GPU-built groups with `vkCmdDrawIndexedIndirectCount` and passes every other draw on unchanged.
- Notes: Module `gpuCulling`. Renderpearl has no counted indirect draw.

#### VulkanFeatureSetsMixin, VulkanConstStorageMixin
- Path: `src/main/java/com/aryston/kinetic/mixin/VulkanFeatureSetsMixin.java`, `src/main/java/com/aryston/kinetic/mixin/VulkanConstStorageMixin.java`
- Role: Adds `IndirectCountFeature` to the optional device feature sets, so the device enables `drawIndirectCount` where the GPU supports it, and maps `GpuBufferUsage.STORAGE` to Vulkan storage buffer usage.
- Notes: Module `gpuCulling`. Renderpearl enables only the features vanilla uses and has no storage buffer usage.

#### VulkanDeviceAccessor, VulkanCommandEncoderAccessor, VulkanRenderPassAccessor, FrontendRenderPassAccessor
- Path: `src/main/java/com/aryston/kinetic/mixin/VulkanDeviceAccessor.java`, `src/main/java/com/aryston/kinetic/mixin/VulkanCommandEncoderAccessor.java`, `src/main/java/com/aryston/kinetic/mixin/VulkanRenderPassAccessor.java`, `src/main/java/com/aryston/kinetic/mixin/FrontendRenderPassAccessor.java`
- Role: Read the enabled device features, the encoder's current command buffer outside render passes, a Vulkan render pass's command buffer and descriptor push, and the backend behind a frontend render pass.
- Notes: Applied always; only `GpuCulling` uses them, and only after its module check.

#### LevelRendererDirectDrawMixin
- Path: `src/main/java/com/aryston/kinetic/mixin/LevelRendererDirectDrawMixin.java`
- Role: Tells `DirectDraws` at the start of `prepareChunkRendersIndirect` and `prepareChunkRenders` which kind of pass runs, and redirects the two writes of `prepareChunkRendersIndirect` (indirect commands, instanced section infos) to it.
- Notes: Module `directDraws`, renderer module. Vanilla's writes take one record per draw and per section; the draw list built them by the ten thousand per frame.

#### RenderSectionBlockEntityMixin, LevelExtractorBlockEntitiesMixin
- Path: `src/main/java/com/aryston/kinetic/mixin/RenderSectionBlockEntityMixin.java`, `src/main/java/com/aryston/kinetic/mixin/LevelExtractorBlockEntitiesMixin.java`
- Role: `RenderSectionBlockEntityMixin` keeps a flag on each section: set before `setSectionMesh` swaps in a mesh with renderable block entities, set to the new mesh's state right after the swap and cleared after `reset`, so it is never false while the current mesh has block entities. `LevelExtractorBlockEntitiesMixin` wraps the `getSectionMesh` call of `LevelExtractor.extractVisibleBlockEntities` and hands vanilla the shared empty mesh for flagged-off sections, so the frame no longer reads the mesh of every visible section to find the few with chests, signs or beds.
- Notes: Module `blockEntitySections`. No event exposes the block entity scan.

#### RenderSectionMixin
- Path: `src/main/java/com/aryston/kinetic/mixin/RenderSectionMixin.java`
- Role: Implements `SectionDrawRecordHolder` on `SectionRenderDispatcher.RenderSection`.
- Notes: Module `sectionSliceCache`.

#### RenderSectionMeshVersionMixin
- Path: `src/main/java/com/aryston/kinetic/mixin/RenderSectionMeshVersionMixin.java`
- Role: Gives each section a new mesh version from `SectionDrawRecord.nextMeshVersion` right after `setSectionMesh` and `reset` swap its mesh, so the draw list and the occlusion snapshots check a field of the section instead of reading the mesh through its `AtomicReference` every frame. Versions are unique, so a lost write between two threads never matches an older cached value, and a reader that reads the version before the mesh never keeps a stale mesh.
- Notes: Belongs to no module and always applies, because draw records (`sectionSliceCache`) and cached occluders (`occlusionCulling`) both depend on it.

#### RenderSectionOcclusionMixin
- Path: `src/main/java/com/aryston/kinetic/mixin/RenderSectionOcclusionMixin.java`
- Role: Implements `CameraOcclusionHolder` on `SectionRenderDispatcher.RenderSection`: the generation that hid the section and the section's occluder boxes cached for one mesh version.
- Notes: Module `occlusionCulling`.

#### CompiledSectionMeshMixin
- Path: `src/main/java/com/aryston/kinetic/mixin/CompiledSectionMeshMixin.java`
- Role: Implements `SectionSliceHolder` on `CompiledSectionMesh` with one field per vanilla layer (an array only for layers beyond three), so a lookup reads the mesh object the draw loop has just touched.
- Notes: Module `sectionSliceCache`.

#### UberGpuBufferMixin
- Path: `src/main/java/com/aryston/kinetic/mixin/UberGpuBufferMixin.java`
- Role: Clears a mesh's cached slices whenever `UberGpuBuffer` frees its allocation and clears every cached slice when the buffer closes.
- Notes: Module `sectionSliceCache`. Every new allocation of a mesh is preceded by `freeAllocation` of that mesh in `uploadStagedAllocations`, and allocations never move inside a heap, so a cached slice (or `ABSENT`) is always what vanilla would return.

#### VisGraphMixin
- Path: `src/main/java/com/aryston/kinetic/mixin/VisGraphMixin.java`
- Role: At the start of `VisGraph.resolve` (before its flood fill overwrites the bits) turns the opaque block bits into `OccluderBoxes`, and attaches them to the returned `VisibilitySet`.
- Notes: Module `occlusionCulling`. Runs on the mesh build threads; no API exposes the opaque blocks of a section.

#### VisibilitySetMixin
- Path: `src/main/java/com/aryston/kinetic/mixin/VisibilitySetMixin.java`
- Role: Implements `OccluderHolder` on `VisibilitySet`.
- Notes: Module `occlusionCulling`.

#### CompiledSectionMeshVisibilityAccessor
- Path: `src/main/java/com/aryston/kinetic/mixin/CompiledSectionMeshVisibilityAccessor.java`
- Role: Accessor for the private `visibilitySet` of `CompiledSectionMesh`.
- Notes: Module `occlusionCulling`.

#### LevelRendererOcclusionMixin
- Path: `src/main/java/com/aryston/kinetic/mixin/LevelRendererOcclusionMixin.java`
- Role: Around `prepareChunkRenders` and `prepareChunkRendersIndirect` tells `TerrainOcclusion` which matrix the pass uses and the section fade-in time.
- Notes: Module `occlusionCulling`. Camera culling itself runs in `SectionDrawLists`, so it also needs module `sectionSliceCache`.

#### LevelExtractorOcclusionMixin
- Path: `src/main/java/com/aryston/kinetic/mixin/LevelExtractorOcclusionMixin.java`
- Role: At the start of `extractVisibleEntities` takes the finished camera pass and submits the next one through `TerrainOcclusion.prepareCamera`; drops entities from `isEntityVisible` and block entities from `extractVisibleBlockEntities` that `EntityOcclusion` finds hidden.
- Notes: Module `occlusionCulling`. Globally rendered block entities (drawn regardless of where the camera looks, like beacon beams) are never dropped.

#### QuadParticleGroupOcclusionMixin
- Path: `src/main/java/com/aryston/kinetic/mixin/QuadParticleGroupOcclusionMixin.java`
- Role: Wraps the frustum check in `QuadParticleGroup.extractRenderState`; a particle inside the frustum that `ParticleOcclusion` finds hidden is not extracted.
- Notes: Module `occlusionCulling`. Runs after `extractVisibleEntities`, so the frame's camera raster is ready.

#### ParticleAccessor
- Path: `src/main/java/com/aryston/kinetic/mixin/ParticleAccessor.java`
- Role: Accessor for the protected last-tick position (`xo`, `yo`, `zo`) of `Particle`.
- Notes: Module `occlusionCulling`.

#### ModelBlockRendererLeafCullingMixin
- Path: `src/main/java/com/aryston/kinetic/mixin/ModelBlockRendererLeafCullingMixin.java`
- Role: At the start of `ModelBlockRenderer.shouldRenderFace` (the overload with the level and both positions) returns false for faces `LeafCulling` hides, only while face culling is on.
- Notes: Not tied to a module; the `leafCulling` client setting switches it.

#### CompiledSectionMeshFacingMixin
- Path: `src/main/java/com/aryston/kinetic/mixin/CompiledSectionMeshFacingMixin.java`
- Role: At the end of the `CompiledSectionMesh` constructor (on the chunk build worker, before the layers are uploaded) sorts the layers through `FaceCulling.sortLayers` and keeps the ranges; implements `FaceRangesHolder`.
- Notes: Module `faceCulling`, renderer module.

#### LevelRendererFaceCullingMixin
- Path: `src/main/java/com/aryston/kinetic/mixin/LevelRendererFaceCullingMixin.java`
- Role: Around `prepareChunkRenders` and `prepareChunkRendersIndirect` tells `FaceCulling` which matrix the pass uses.
- Notes: Module `faceCulling`, renderer module.

#### LevelRendererMeshWorkersMixin
- Path: `src/main/java/com/aryston/kinetic/mixin/LevelRendererMeshWorkersMixin.java`
- Role: Replaces the executor argument of `new SectionRenderDispatcher` in `LevelRenderer.invalidateCompiledGeometry` with `MeshWorkers.executor()`.
- Notes: Module `meshWorkers`, renderer module.

#### HelionShadowCastersMixin
- Path: `src/main/java/com/aryston/kinetic/mixin/HelionShadowCastersMixin.java`
- Role: Pseudo mixin on Helion's `VanillaShadowCasterSource.collectSections`; filters the returned per-cascade caster lists with `HelionShadowCasters`.
- Notes: Module `occlusionCulling`. Optional (`require = 0`): without Helion nothing is applied. Selected by name, arguments through `@Local(argsOnly = true)` (lesson L-145). Lossless: a caster the light cannot see is behind other casters in the same cascade, which already write a nearer depth into that shadow map.

#### ModelCubeCornersMixin
- Path: `src/main/java/com/aryston/kinetic/mixin/ModelCubeCornersMixin.java`
- Role: Replaces `ModelPart.Cube.compile` with shared-corner model rendering and caches the corner map per cube.
- Notes: Module `modelCorners`. No API replaces `ModelPart.Cube.compile`.
