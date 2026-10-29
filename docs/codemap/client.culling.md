# Kinetic Code Map: `com.aryston.kinetic.client.culling`

Part of the [Kinetic code map](../CODEMAP.md). Every class of this package has its entry here and nowhere else (rule D1).

### `com.aryston.kinetic.client.culling`

#### TerrainOcclusion
- Path: `src/main/java/com/aryston/kinetic/client/culling/TerrainOcclusion.java`
- Role: Occlusion culling for terrain and the camera raster that entity culling shares. `prepareCamera` runs once per frame before entities are extracted and never waits: it takes the camera pass the worker finished (`OcclusionPipeline`), stamps every section that pass hid with the pass generation (`CameraOcclusionHolder`), uses the result this frame unless it is both more than 6 frames and more than 60 ms old (so high frame rates do not age results out early) and while the eye is still within its reach (or widens it to the eye, up to 8 blocks, with `Frame.widen` instead of drawing every section), and then copies the visible sections (origin, computed from the section node instead of the origin object, occluder boxes, fully faded in) into the free frame and submits it. The reach is 0.125 blocks plus three times the distance the eye moved since the last frame, at most 4 blocks; test boxes grow by it. The camera raster uses a projection 25 % wider than the screen and treats boxes reaching past its edge as visible, so turning the camera cannot reveal a section the pass hid. `cullsCameraSections` and `hiddenFromCamera` let `SectionDrawLists` skip hidden sections; sections not in the pass are never hidden. The frame counter advances at `RenderFrameEvent.Post`, because vanilla extracts a frame before `RenderFrameEvent.Pre` fires. `beginTerrainPass` marks a `prepareChunkRenders` call as the camera pass when its matrix equals the camera's view rotation (vanilla and Helion both pass a copy of it; Helion's shadow cascades pass the light view). `filterShadowCasters` removes casters the light cannot reach, one orthographic pass per shadow cascade in light space (up to 8 cascades). Each cascade is a `ShadowCascadeCulling` on the `Kinetic Shadow Culling` worker, matched by section position so results stay usable while the camera moves.
- Notes: Sections still fading in never occlude. The camera pass and the shadow pass each rest for a second (`CullingYield`) after 20 results in a row hid less than 15 % of what they tested, then probe again; `cameraPaused` and `shadowsPaused` tell the overlay. The camera pass runs on the single `Kinetic Culling` worker; the render thread part (copies, stamps, cascade hashes) shows as `Occlusion culling` in the overlay, the worker time as `worker` in the culling line. `occlusionCulling` in the client config switches it at runtime; switching it off drops the result in flight.
- Depends on: `OcclusionPipeline`, `OcclusionPass`, `DepthRaster`, `ClipMatrix`, `CullingStatistics`, `JobSystem`, `CameraOcclusionHolder`, `OccluderHolder`, `CompiledSectionMeshVisibilityAccessor`, `ShadowCascadeCulling`, `CameraResult`.

#### ShadowCascadeCulling
- Path: `src/main/java/com/aryston/kinetic/client/culling/ShadowCascadeCulling.java`
- Role: Occlusion culling of one Helion shadow cascade on the `Kinetic Shadow Culling` worker. Per frame it hashes the light direction and the cascade (section, occluder boxes, fade state) and submits a changed cascade; a finished result is matched to the current list by section position (`Long2IntOpenHashMap`), not by list order. While the light direction is unchanged, a caster the result hid is dropped unless it changed itself or an occluder that left the list or changed covers one of its raster tiles, so results stay usable while the camera moves and the cascade gains and loses sections at its edges.
- Notes: Removing a caster is lossless because the shadow map keeps only the nearest depth and the occluders in front of it are still drawn. New sections are never hidden.
- Depends on: `OcclusionPipeline`, `TerrainOcclusion`.

#### EntityOcclusion
- Path: `src/main/java/com/aryston/kinetic/client/culling/EntityOcclusion.java`
- Role: Decides whether an entity or a block entity is hidden from the camera by testing its box (entities: interpolated bounding box grown by 1 block; block entities: the renderer's render box grown by half a block) against this frame's camera raster in `TerrainOcclusion`. Skips the raster test when the current camera result hid no section. Counts tested and hidden entities for the overlay.
- Notes: Only mobs, dropped items, experience orbs and armor stands are culled; glowing, named, leashed, riding or ridden entities are always drawn. With Helion loaded an entity under open sky is always drawn, because it may cast a sun shadow onto visible ground.
- Depends on: `TerrainOcclusion`, `CullingAudit`.

#### ParticleOcclusion
- Path: `src/main/java/com/aryston/kinetic/client/culling/ParticleOcclusion.java`
- Role: Decides whether a particle is hidden from the camera: the box spans its position last tick and now, grown by one and a half times its quad size plus half a block, and is tested against this frame's camera raster in `TerrainOcclusion`. Skips the raster test when the current camera result hid no section. Counts tested and hidden particles for the overlay.
- Depends on: `TerrainOcclusion`, `CullingAudit`.

#### HelionShadowCasters
- Path: `src/main/java/com/aryston/kinetic/client/culling/HelionShadowCasters.java`
- Role: Called by `HelionShadowCastersMixin` with Helion's cascades and caster lists; reads the light view of the first cascade (all cascades share it) through `lightView()` and passes the lists to `TerrainOcclusion`.
- Notes: Kinetic does not compile against Helion; if Helion changes that method the hook does nothing and the overlay shows no shadow numbers.
- Depends on: `TerrainOcclusion`.

#### CullingAudit
- Path: `src/main/java/com/aryston/kinetic/client/culling/CullingAudit.java`
- Role: Debug check of the camera pass, on with `-Dkinetic.cullingAudit=true`: every 30 finished camera passes it casts 24 rays from the current camera (not the eye the pass ran at) to random on-screen points of up to 48 hidden sections through the real blocks and counts rays that reach the section box without passing an opaque block (each one would be a wrongly hidden section). Logs totals every 20 checks and the first 20 failures.
- Depends on: `VoxelRay`, `OcclusionPass`, `OcclusionPipeline`, `ClipMatrix`.

#### OccluderHolder
- Path: `src/main/java/com/aryston/kinetic/client/culling/OccluderHolder.java`
- Role: Interface `VisibilitySetMixin` adds to `VisibilitySet` to carry the section's `OccluderBoxes` from the mesh build to the compiled mesh.
- Depends on: `OccluderBoxes`.

#### CameraOcclusionHolder
- Path: `src/main/java/com/aryston/kinetic/client/culling/CameraOcclusionHolder.java`
- Role: Interface `RenderSectionOcclusionMixin` adds to `SectionRenderDispatcher.RenderSection`: the generation of the last camera pass that hid the section, and its occluder boxes cached for a mesh version (`TerrainOcclusion.occluders(section)` reads mesh and visibility set only when the version changed).
- Depends on: nothing.
