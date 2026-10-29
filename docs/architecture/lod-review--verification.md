# Distant Terrain Review (historical): Verification and remaining work

Historical record, kept unchanged: the distant terrain review of 2026-10-19 and its follow-up of 2026-10-20. Part of the [Kinetic architecture](../ARCHITECTURE.md); the current design is in [the distant terrain section](engine-systems--terrain-and-join.md).

## Verification and remaining work

Ranks 1 through 8 are implemented, with the bounded approximations described below. Rank 9 has uploaded-chunk masking and complementary dissolves; rank 10 has conservative frustum bounds, shared geometry and indirect draws. Vertex geomorphing and GPU occlusion are not implemented. Visual parity and resource superiority remain unmeasured.

- Sample provenance, complete footprints and known voids now survive downsampling. Complete coarse data cannot be replaced by sparse children. The selection gate uses the completeness of the **uploaded mesh**, including zero-geometry coverage, rather than the current worker tile.
- Vertices remain 16 bytes, with the former alpha byte carrying sky4/block3/water1 metadata. Equal top rectangles and side runs merge. The uniform heightfield fixture falls from 4,352 quads / 278,528 vertex bytes to 5 quads / 320 bytes. The two-layer uniform canopy/ground fixture is 12 quads, retaining its gap and underside. These are exact fixtures, not average performance claims.
- At most 24 ready meshes and 32 MiB of queued vertices plus one deferred upload are retained. Uploads stop between meshes at six meshes, 2 MiB or 1 ms. One indivisible driver call may exceed the target. Client capture checks 1 ms between columns and admits 32 copied chunks; native textures and live chunks never reach the store worker.
- Saved FULL chunks supply actual surfaces, layers, builds, biomes and sky/block light at levels 0 to 2. Saved data is tried first; complete patches avoid seed generation. Reads use `ChunkMap.read` / `SimpleRegionStorage.read` / `IOWorker.loadAsync`; `RegionFileStorage.read` opens existing files without creating missing ones. Old tags are copied and upgraded privately with dimension/generator context, never written back; future formats are ignored. Only generation workers wait, with a two-second timeout. No tickets, live chunk deserialization, entity loading or chunk saves are added.
- Complete footprints outrank incomplete ones; among equally complete samples priority is client > saved > seeded > approximate. Same-source saved refreshes are allowed and complete client edits remain authoritative. Nearby completed tiles become eligible for saved refresh after 30 seconds when missing preview work is satisfied, with first-time exact refinement taking priority. Client section dirtiness queues fresh capture. Isolated tint callbacks receive private sections and an empty engine; unsupported custom callbacks retain base colour.
- Exact seed refinement uses original private 26.3 neighborhoods: 64-block output patches, 27-square structure-start holders, 10-square biome, eight-square terrain and six-square feature regions. Structure references and required heightmaps are prepared explicitly. Terrain runs the actual combined density/surface/carver stage, followed by decoration. No LIGHT, SPAWN, live FULL conversion, entities, live POI update or chunk save runs. One exact job is permitted at a time, and its stage requests are sequential. Vanilla biome/terrain futures execute on its background executor; only the generation worker joins. Cancellation occurs between stages/chunks/rows, not within one vanilla stage.
- Exact seed support is intentionally conservative: vanilla noise/flat generator classes, with no non-vanilla block, feature-type, density-function, biome-source or structure-type registration. Other worlds retain preview plus real saved/client data. This does not sandbox arbitrary modded world generation. Low quality and distant levels retain preview heights/biome colours, including the approximate canopy offset.
- The persistent cache hashes world path, seed, dimension, serialized generator, structure option, quality, data version, separately delimited mod identifiers/versions, the frozen material palette, server JSON/structure NBT and client block textures/models/blockstates/biome colour maps. Versioned key/CRC headers protect level-1 DEFLATE payloads. Inflation, column counts and span counts are bounded. Corrupt entries are removed for regeneration; unique temporary writes publish atomically and tolerate an already-published concurrent winner on Windows. Each identity trims toward 512 MiB once per minute on writes. Mutable saved/client edits are never persisted as deterministic seed data.
- Near columns keep at most eight nonoverlapping vertical spans with 14 bytes per span plus offsets. Meshing subtracts adjacent solid intervals, emits undersides and retains opaque ground under water. Side materials sample the actual middle of the retained interval; this is a representative cliff colour, not all geological strata. A result over 65,536 layered quads falls back to the heightfield, limiting one vertex payload to 4 MiB.
- Resource-pack top/side texture averages and tint indices are copied into an immutable palette on the client in a separate 1 ms/256-state budget. An alpha-weighted linear eight-square sample grid bounds high-resolution texture cost. Workers use integers only. Actual saved/client light is retained; seed skylight is an exposed/shaded estimate, including no-sky dimensions, rather than a generated light engine. Forward shading uses the current lightmap, while Helion receives unshaded albedo and real light coordinates. Water blends without depth writes; Helion water is drawn after opaque deferred lighting through a separate optional one-target pass.
- Generation cancellation is checked within tiles and between saved reads. Request keys are released after every exit, failures have a cooldown, and old epochs cannot supply captured samples or queued/uploaded meshes after a world/quality reset. Already loaded chunks are rediscovered gradually after a reset, including multiplayer where no seed source exists.
- Both the live feature switch and restart-required module gate source/palette work. Inactive frames release GPU state once and return without rebuilding empty resource containers.
- Low targets one generator at 25% active time; Balanced targets 50% per permitted worker. Since 2026-10-23 Balanced uses the High targets while more than 32 tiles of the current distance are missing: on a 12-thread CPU at the default 256 chunks and High quality the area within 512 blocks filled in 78 s instead of more than 265 s, and all 575 tiles in 160 s instead of about 8.5 minutes. High/Extreme add no idle time. These are wall-time-based targets including I/O, not a hard process-CPU percentage. At 16 logical processors the worker limits are 1 / 3 / 7 / 15. Quality retains its original split factors (1.25 / 1.5 / 2 / 2.5), finest levels (3 / 2 / 1 / 0) and height grids (8 / 16 / 16 / 32); distance buttons still only select distance and enable distant terrain.
- Once per second the store trims toward 2,048 tiles and 128 MiB including layers, favoring nearby/coarse coverage. Work between trims can temporarily exceed this. GPU geometry has eight 16 MiB pages, with coalesced suballocation and changed-range uploads. Draw instances/indirect commands use at most 32 MiB of staging, reused after zero-timeout fence checks; exhaustion defers submissions instead of waiting. Temporary generation neighborhoods, palettes, metadata and native-driver allocations are additional memory.
- Adaptive mixed-level skirts remain. Conservative frustum boxes include all retained layers and skirts. Opaque draws are batched by geometry page with indexed indirect commands; water is sorted at tile granularity. Per-draw instances carry camera-relative offsets, region bounds and complementary fade intervals, avoiding absolute-world float precision loss.
- The unconditional real-distance clip square is replaced by a 256-square R8 readiness mask: captured top-surface sections must have uploaded meshes, then coverage waits 250 ms and fades over 250 ms. Mask uploads occur at most every 25 ms. Parent/child and replaced tile meshes use complementary screen-stable 250 ms dissolves; old keys/buffers are retained until completion, and sparse previous coverage is partitioned so newly available regions are filled. This is not vertex morphing or a proof of gap-free fast flight. Model reload resets palette/cache epochs, and shutdown cancels generation and releases drawing resources without joining workers.

Pre-video validation (2026-10-20): `gradlew.bat build` passed with 220 tests, zero failures and zero skipped tests, without new warnings. `bash scripts/verify.sh` passed for the workspace and all three mod repositories. Verification used temporary Git long-path/excludes settings for an existing unrelated Helion build cache, preserving existing ignore patterns; no tracked source was excluded and no Helion file or persistent Git setting was changed. Engine tests cover source priority, complete/void footprints, layered gaps and water ordering, metadata, pathological mesh size, byte residency, allocation/coalescing, negative coverage coordinates, sparse transitions, cache compression/corruption/concurrent publication and preview/refinement order. A native compiler test compiles both fragment paths and the vertex shader to SPIR-V with actual game/resource includes. This does not apply mixins or exercise a Vulkan device. The game was not launched; no commit or push was made.

### Video regression repair (2026-10-20)

The supplied 12-second recording from 01:29:40 shows coarse green terrain intermittently covering more detailed terrain. The existing game log also repeatedly reports missing dungeon spawners on the distant generation worker. This is runtime evidence that the earlier compilation and engine tests did not establish correct appearance.

The audit found and repaired five independent failure mechanisms:

1. Selection depended on CPU sample residency even for complete uploaded meshes. Sample trimming could therefore remove visible detail or select a coarse parent. Selection now recognizes uploaded meshes at roots and children independently from CPU tiles. A regression test failed on the old code and passes with CPU tiles removed after upload.
2. Partial high-authority client ancestors rejected complete lower-authority previews. Complete footprints now take priority over partial ones; provenance remains authoritative among equally complete samples, preserving full client columns. A second regression test failed before the repair and now verifies complete coverage through all six levels.
3. Real-terrain masking waited for full layered capture. Chunk loads, rediscovery and uploaded sections now queue independent heightmap-only requirements, processed at most 16 columns/1 ms per frame. Mesh readiness still waits for actual required sections and verifies rotating-section coordinates.
4. Periodic saved refresh had priority over unfinished exact refinement, so a long refresh cycle could keep nearby terrain approximate indefinitely. Missing previews still run first; exact refinement now precedes refresh, and other capacity can refresh while one exact job runs.
5. Isolated features could place spawners/chests but never retrieve their block entities. Requested block entities are now retained only in private chunks, remain detached from any live level, and are removed when replaced. Nothing ticks, spawns, activates or saves them. Cache identity revision 3 prevents reuse of earlier feature output.

These mechanisms are supported by code inspection and the two reproduced engine failures; the supplied video alone does not establish the contribution of each to every flickering frame. The corrected build still needs visual confirmation in the same scene with both render paths. No game was launched during this repair.

Repair validation: `gradlew.bat build` passed with 222 tests, zero failures, errors or skipped tests, and no new warnings. This includes the two new regression tests and the coverage lifetime assertions. `bash scripts/verify.sh` passed for the workspace and all three mod repositories after correcting a documentation phrase caught by the literal word filter. The same temporary long-path/excludes settings described above were used. The corrected scene, private block entities and both render paths remain unverified in game.

### Work left undone

1. Run the forbidden-for-this-task runtime checks later: first repeat the supplied flickering scene with this repair, then exact seed patches against real terrain, boundaries/fast flight, all dimensions, changed packs/quality/worlds, reopened and old saves, shutdown during generation, vanilla/Helion with and without improved transparency, and an unmodified multiplayer server. The owner's earlier session exposed failures; the repaired adapters have not been run in game here.
2. Rank 9 vertex geomorphing and measured GPU occlusion from rank 10 remain unimplemented. The present approach is complementary dissolve plus conservative frustum culling. GPU occlusion must first demonstrate a net gain and avoid hiding visible overhangs; existing real-chunk GPU culling remains separate.
3. Exact support for custom generators/registered worldgen, full generated skylight, unlimited vertical detail, per-stratum cliff colours, exact biome blending, water per-face sorting/refraction and fully coordinated real-terrain fade remain beyond this bounded representation. Top-surface section readiness is a conservative transition heuristic, not per-pixel real-mesh coverage. Further than level 2, and at Low quality, approximation is deliberate.
4. Client observations remain authoritative until fresh client capture; server-side edits outside that view may not supersede stale client-origin samples through saved refresh alone. Cache quotas are per identity with periodic soft trimming; obsolete identities are not globally pruned. A continuous world-generation stage or native driver call cannot be preempted by the time target.
5. Equal-area/equal-quality CPU, heap/native RAM, VRAM and GPU measurements against both supplied references remain undone. No claim of matching their appearance or consuming fewer total resources than both is supported by the current tests.

### Files changed in this task

Paths in the table are relative to `C:/Users/Can/Desktop/Aryston/mods/kinetic/`, except the explicitly named workspace document. Other uncommitted Phase 6 and settings work was already present at entry and is not attributed to this task.

| File | Change |
|---|---|
| `build.gradle` | Native shader compiler test access flags. |
| `src/main/java/com/aryston/kinetic/core/lod/DistantTerrainLimits.java` | Tile and layer-byte residency targets. |
| `src/main/java/com/aryston/kinetic/core/lod/LodAllocator.java` | New bounded, coalescing shared-page allocator. |
| `src/main/java/com/aryston/kinetic/core/lod/LodColor.java` | New texture/tint RGB multiplication. |
| `src/main/java/com/aryston/kinetic/core/lod/LodCoverage.java` | New uploaded-surface readiness, known-requirement checks and ring-mask logic. |
| `src/main/java/com/aryston/kinetic/core/lod/LodData.java` | New owned tile payload and exactness checks. |
| `src/main/java/com/aryston/kinetic/core/lod/LodDiskCache.java` | New compressed, validated, repaired and periodically trimmed cache. |
| `src/main/java/com/aryston/kinetic/core/lod/LodLayers.java` | New capped vertical intervals and side material snapshots. |
| `src/main/java/com/aryston/kinetic/core/lod/LodLayerMesher.java` | New greedy layered surfaces and exposed interval walls. |
| `src/main/java/com/aryston/kinetic/core/lod/LodPlanner.java` | Fine-first exact refinement alongside coarse-first previews. |
| `src/main/java/com/aryston/kinetic/core/lod/LodTransitions.java` | New complementary transition region partitioning. |
| `src/main/java/com/aryston/kinetic/core/lod/GenerationPower.java` | Reserved processors and idle targets. |
| `src/main/java/com/aryston/kinetic/core/lod/LodBorders.java` | New immutable edge snapshot. |
| `src/main/java/com/aryston/kinetic/core/lod/LodCombine.java` | Provenance, partial/void coverage and light aggregation. |
| `src/main/java/com/aryston/kinetic/core/lod/LodMesh.java` | Coverage, opaque/water ranges and vertical bounds. |
| `src/main/java/com/aryston/kinetic/core/lod/LodMesher.java` | Merged geometry, adaptive skirts, packed light and byte fallback. |
| `src/main/java/com/aryston/kinetic/core/lod/LodSample.java` | Coverage/source flags, light and vertex metadata. |
| `src/main/java/com/aryston/kinetic/core/lod/LodSelection.java` | Uploaded-child completeness gate and selection independent of CPU eviction. |
| `src/main/java/com/aryston/kinetic/core/lod/LodStore.java` | Completeness before source precedence, coverage-safe aggregation, edge sampling and residency trimming. |
| `src/main/java/com/aryston/kinetic/core/lod/LodTile.java` | Exact/complete counters, layers and byte accounting. |
| `src/main/java/com/aryston/kinetic/client/ClientEvents.java` | Reload/unload/frame/shutdown lifecycle wiring. |
| `src/main/java/com/aryston/kinetic/client/lod/ColumnSampler.java` | Main-thread client view construction. |
| `src/main/java/com/aryston/kinetic/client/lod/DistantTerrain.java` | Budgeted layered capture, independent readiness queues on load/discovery, edits, epochs and lifecycle. |
| `src/main/java/com/aryston/kinetic/client/lod/DistantTerrainCoverage.java` | New upload readiness adapter, independently budgeted height requirements and mask texture. |
| `src/main/java/com/aryston/kinetic/client/lod/DistantTerrainPipeline.java` | Instanced opaque/water/geometry pipelines and samplers. |
| `src/main/java/com/aryston/kinetic/client/lod/DistantTerrainRenderer.java` | Pooled indirect draws, frustum bounds, coverage, fades, water and pressure limits. |
| `src/main/java/com/aryston/kinetic/client/lod/GeneratedColumns.java` | Preview provenance, cancellation and saved overlays. |
| `src/main/java/com/aryston/kinetic/client/lod/IsolatedTerrainRegion.java` | New private generation region, detached feature block entities and holders without live writes or entity activation. |
| `src/main/java/com/aryston/kinetic/client/lod/SavedColumnView.java` | Private/client layers, material/tint and dimension-aware light sampling. |
| `src/main/java/com/aryston/kinetic/client/lod/SavedTerrain.java` | Asynchronous saved source and private old-tag upgrading. |
| `src/main/java/com/aryston/kinetic/client/lod/SeedTerrain.java` | New isolated actual terrain/structure/feature generation. |
| `src/main/java/com/aryston/kinetic/client/lod/TerrainCache.java` | New worker-only identity/resource fingerprint adapter; generation revision 3 invalidates earlier feature output. |
| `src/main/java/com/aryston/kinetic/client/lod/TerrainDraws.java` | New bounded, nonblocking indirect/instance staging. |
| `src/main/java/com/aryston/kinetic/client/lod/TerrainGeometry.java` | New shared vertex pages and changed-range uploads. |
| `src/main/java/com/aryston/kinetic/client/lod/TerrainMaterials.java` | New budgeted texture-average/tint palette. |
| `src/main/java/com/aryston/kinetic/client/lod/TerrainGenerator.java` | Preview/refinement/refresh planning without refresh starvation, cancellation, power and shutdown. |
| `src/main/java/com/aryston/kinetic/client/lod/TerrainWorker.java` | Graceful store cleanup and late-submission handling. |
| `src/main/java/com/aryston/kinetic/client/lod/TileMeshing.java` | Epoch-tagged count/byte-bounded ready queue and border remeshing. |
| `src/main/java/com/aryston/kinetic/integration/MixinModules.java` | Gate new distant hooks with the existing module. |
| `src/main/java/com/aryston/kinetic/mixin/HelionDistantTerrainMixin.java` | Keep opaque draws separate from post-lighting water. |
| `src/main/java/com/aryston/kinetic/mixin/HelionDistantWaterMixin.java` | New optional transparent-stage water hook. |
| `src/main/java/com/aryston/kinetic/mixin/RenderSectionDistantTerrainMixin.java` | New uploaded/reset section notifications. |
| `src/main/java/com/aryston/kinetic/mixin/SectionUpdateTrackerDistantTerrainMixin.java` | New block/light edit capture notifications. |
| `src/main/resources/kinetic.mixins.json` | Register new client/optional hooks. |
| `src/main/resources/assets/kinetic/shaders/include/lod_common.glsl` | New shared coverage/dither/light/fog/water logic. |
| `src/main/resources/assets/kinetic/shaders/lod/distant_terrain.vsh` | Instanced offsets/fades and packed metadata decoding. |
| `src/main/resources/assets/kinetic/shaders/lod/distant_terrain.fsh` | Lightmap-lit forward distant terrain. |
| `src/main/resources/assets/kinetic/shaders/lod/distant_terrain_geometry.fsh` | Unshaded albedo and retained light for Helion. |
| `src/test/java/com/aryston/kinetic/core/lod/GenerationPowerTest.java` | New CPU-setting tests. |
| `src/test/java/com/aryston/kinetic/core/lod/LodMesherTest.java` | Geometry, void, child coverage, uploaded detail after sample eviction and mixed-level edge tests. |
| `src/test/java/com/aryston/kinetic/core/lod/LodStoreTest.java` | Provenance, complete preview repair of sparse ancestors, void coverage and residency tests. |
| `src/test/java/com/aryston/kinetic/core/lod/LodAllocatorTest.java` | New allocation/coalescing/limits tests. |
| `src/test/java/com/aryston/kinetic/core/lod/LodCoverageTest.java` | New readiness, known-requirement, mask-coordinate and lifetime tests. |
| `src/test/java/com/aryston/kinetic/core/lod/LodDiskCacheTest.java` | New compression, corruption, identity and concurrent-write tests. |
| `src/test/java/com/aryston/kinetic/core/lod/LodLayersTest.java` | New overhang, water, light, propagation and mesh-budget tests. |
| `src/test/java/com/aryston/kinetic/core/lod/LodPlannerTest.java` | New coarse-preview/fine-refinement ordering test. |
| `src/test/java/com/aryston/kinetic/core/lod/LodTransitionsTest.java` | New region completeness and fade-lifetime tests. |
| `src/test/java/com/aryston/kinetic/client/lod/LodShaderTest.java` | New actual-include SPIR-V compilation test. |
| `docs/ARCHITECTURE.md` | Current Phase 6 invariants and limits. |
| `docs/CHANGELOG.md` | Player-facing change entries. |
| `docs/CODEMAP.md` | Updated entries for every changed/new source and test. |
| `docs/FEATURES.md` | Current saved-terrain behavior and remaining preview limits. |
| `docs/LOD_REVIEW.md` | Reference evidence, gap analysis, ranked work and this implementation audit. |
| `C:/Users/Can/Desktop/Aryston/docs/RULES.md` | Lessons on coverage, bounded work, epoch lifetime and the video regression repairs. |

Remaining after the video repair: in-game confirmation of the corrected scene and adapters, the runtime/performance comparisons, geometric morphing, GPU occlusion and representation/compatibility limits listed above. No commit or push was made.
