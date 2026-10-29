# Kinetic Code Map: `com.aryston.kinetic.core.culling`

Part of the [Kinetic code map](../CODEMAP.md). Every class of this package has its entry here and nowhere else (rule D1).

### `com.aryston.kinetic.core.culling`

#### ClipMatrix
- Path: `src/main/java/com/aryston/kinetic/core/culling/ClipMatrix.java`
- Role: Column-major 4x4 matrix without allocation: perspective and orthographic projections with a 0 to 1 depth range, multiplication, and `depthFromW`, which replaces the depth row of any perspective matrix with a standard 0 (near) to 1 (far) depth so the rasterizer never depends on the game's depth convention (Vulkan Minecraft uses reversed depth).
- Depends on: nothing.

#### DepthRaster
- Path: `src/main/java/com/aryston/kinetic/core/culling/DepthRaster.java`
- Role: Small software depth buffer (for example 256 x 144) for occlusion tests. `addOccluder` draws the front faces of a box, clipped at the near plane; `isVisible` tests a box against it with 8 x 8 tiles that remember their farthest depth.
- Notes: Both sides are conservative, so a visible box is never reported hidden: an occluder only covers pixels lying fully inside its face and writes the farthest depth the face reaches inside that pixel; a tested box covers every pixel it touches with its nearest depth. Boxes crossing the near plane or behind the camera are always visible. With `screenBounded` false (light space) a box reaching outside the raster is visible.
- Depends on: `ClipMatrix`.

#### OpaqueMask
- Path: `src/main/java/com/aryston/kinetic/core/culling/OpaqueMask.java`
- Role: The opaque blocks of one 16x16x16 section (the same bits vanilla's `VisGraph` collects, blocks with `isSolidRender`) turned into occluder boxes: fully opaque 2x2x2 cells merged greedily into boxes, at most 24 per section, largest first; a fully opaque section is one box.
- Depends on: `OccluderBoxes`.

#### VoxelRay
- Path: `src/main/java/com/aryston/kinetic/core/culling/VoxelRay.java`
- Role: Block-by-block ray walk (`reaches`: does a ray get from one point to another without entering an opaque block, the start block excluded) and the segment parameter where a ray enters a box (`entry`). Used by the culling audit.
- Depends on: nothing.

#### OccluderBoxes
- Path: `src/main/java/com/aryston/kinetic/core/culling/OccluderBoxes.java`
- Role: Immutable occluder boxes of a section in section-local block coordinates, packed into one int per box.
- Depends on: nothing.

#### OcclusionPass
- Path: `src/main/java/com/aryston/kinetic/core/culling/OcclusionPass.java`
- Role: One culling pass over a list of sections: orders them near to far by projected depth, tests each section box (grown by one block for geometry that reaches out of its section, plus an optional reach for camera movement) against the raster, and draws the occluder boxes only of sections that turned out visible, until a box budget is spent. `measureReveal` records for every hidden section the largest of the reach steps 1, 2, 4 and 8 blocks at which it still stays hidden (testing the largest step first, so a section deep behind a wall costs one test), and `widen` reveals the hidden sections whose recorded step is below a larger reach and lists them, without any raster test; `sectionTiles` gives the raster tiles a section box covers. Reusable arrays, no allocation once grown.
- Depends on: `DepthRaster`, `OccluderBoxes`.

#### OcclusionPipeline
- Path: `src/main/java/com/aryston/kinetic/core/culling/OcclusionPipeline.java`
- Role: Runs an `OcclusionPass` (the camera pass, or one Helion shadow cascade with a reach of 0) on a worker one frame behind the render thread. Two `Frame`s (pass, raster, clip matrix, eye position, reach and the opaque target object of each entry) alternate: the render thread fills a free frame and submits it, the worker rasterizes and tests it, and the render thread takes the finished frame at its next call of `activateFinished` while the next one is in flight. `begin` returns nothing while a frame is in flight, so the render thread never waits. `Frame.covers` says whether a later eye position is within the frame's reach; `Frame.isVisible` tests a world box grown by the reach. `Frame.widen` raises the reach of a finished frame for an eye that moved past it (up to `MAX_REVEAL_REACH`, 8 blocks): it reveals every hidden section whose reveal step, measured on the worker after the pass (`measureReveal`), lies below the new reach, and `Frame.revealed` lists them, so widening can only reveal sections and costs the render thread no raster test. Before 2026-10-16 the render thread tested every hidden section again, which was 11 % of it in the cave scene, where 16 % of the frames widen. `Frame.sameOccluders`, `beginChanges`, `markChanged` and `hiddenDespiteChanges` let a shadow cascade reuse a result after its section list changed: the tiles of every occluder that left the list or changed are marked in a `TileMask`, and a hidden section stays hidden only when none of its tiles is marked. `clear` drops the active frame and the result in flight.
- Notes: A frame's data belongs to the worker between `submit` and `finished`; the volatile `finished` flag publishes the result.
- Depends on: `OcclusionPass`, `DepthRaster`, `ClipMatrix`, `OccluderBoxes`, `TileMask`.

#### CullingYield
- Path: `src/main/java/com/aryston/kinetic/core/culling/CullingYield.java`
- Role: Decides when a culling pass is worth its cost. `result(tested, hidden, now)` counts results that hid less than the useful share; after a run of them it pauses the pass for a while and returns true; after the pause one more low result pauses again, one useful result keeps the pass running. `paused(now)`, `reset()`.
- Depends on: nothing.

#### CullingStatistics
- Path: `src/main/java/com/aryston/kinetic/core/culling/CullingStatistics.java`
- Role: Sections and hidden sections of the last camera pass and the last set of shadow cascades, and tested and hidden entities and particles of the last frame, for the overlay, the worker time of the last camera pass, the share of hidden sections since the last call of `takeCameraHiddenPercent`, the share of hidden shadow casters since the last call of `takeShadowHiddenPercent`, and the share of solid terrain quads face culling skipped since the last call of `takeFaceSkippedPercent`, for the benchmark log. `cameraResult` counts per frame whether the camera result was current, widened, missing, too old or out of reach (`CameraResult`, `takeCameraResults`).
- Depends on: nothing.

#### CameraResult, CameraResultCounts
- Path: `src/main/java/com/aryston/kinetic/core/culling/CameraResult.java`, `src/main/java/com/aryston/kinetic/core/culling/CameraResultCounts.java`
- Role: What a frame's camera culling could use (the current result, a widened older result, none, a result more than 6 frames and 60 ms old, or one whose eye is beyond 8 blocks) and the counts per scene for the benchmark log; every missing result means every section is drawn that frame.
- Depends on: nothing.

#### TileMask
- Path: `src/main/java/com/aryston/kinetic/core/culling/TileMask.java`
- Role: One flag per 8 x 8 raster tile, reused between frames: marks the tile ranges of changed shadow occluders and answers whether a range touches a marked tile (or everything is marked).
- Depends on: nothing.
