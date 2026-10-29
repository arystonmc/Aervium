# Kinetic Code Map: `com.aryston.kinetic.client.lod`

Part of the [Kinetic code map](../CODEMAP.md). Every class of this package has its entry here and nowhere else (rule D1).

### `com.aryston.kinetic.client.lod`

#### ColumnSampler
- Path: `src/main/java/com/aryston/kinetic/client/lod/ColumnSampler.java`
- Role: Creates client-owned column views for budgeted surface, layer, material and live-light capture; no client chunk is handed to a worker.
- Depends on: Minecraft client chunk, block and biome-colour classes.

#### DistantTerrain
- Path: `src/main/java/com/aryston/kinetic/client/lod/DistantTerrain.java`
- Role: Captures client surfaces/layers within a 1 ms column budget and 32 pending chunks, queues lightweight real-mesh requirements on load/discovery, recaptures dirtiness, manages sources, trim/statistics, reload epochs and shutdown. Disabled ticks skip material/source work.
- Depends on: `ColumnSampler`, `TerrainWorker`, `TileMeshing`, `LodStore`, `DistantTerrainLimits`.

#### TerrainGenerator
- Path: `src/main/java/com/aryston/kinetic/client/lod/TerrainGenerator.java`
- Role: Plans coarse-first seed previews on workers, applies CPU concurrency and idle targets (Balanced uses the High targets while more than 32 tiles are missing), cancellation, retries and shutdown cleanup. Logs progress every 10 s and when everything within 512 blocks and every tile are generated.
- Depends on: `GeneratedColumns`, `TerrainWorker`, `TileMeshing`, `LodPlanner`, `LodStore`, `GenerationPower`, `LodQuality`.

#### GeneratedColumns
- Path: `src/main/java/com/aryston/kinetic/client/lod/GeneratedColumns.java`
- Role: Computes seed tiles from sparse noise heights and biome colours, using the disk cache when a tile was computed before; never reads saved chunks or runs world generation stages, so the server thread and its chunk storage stay untouched.
- Depends on: `TerrainCache`, `LodSample`, `LodTile`, vanilla `ChunkGenerator`, `RandomState`, `BiomeResolver`.

#### SavedColumnView
- Path: `src/main/java/com/aryston/kinetic/client/lod/SavedColumnView.java`
- Role: Samples client chunk sections on the main thread, retaining up to eight surface/overhang spans while clamping continuous ground to the level floor, top/side materials, water, provenance and light.
- Depends on: `LodSample`, `LodStore`, vanilla section palettes and `BlockAndTintGetter`.

#### TerrainWorker
- Path: `src/main/java/com/aryston/kinetic/client/lod/TerrainWorker.java`
- Role: Single minimum-priority daemon that exclusively owns the store; gracefully drains accepted cleanup on shutdown and rejects late shutdown submissions without waiting.
- Depends on: `LodStore`.

#### TileMeshing
- Path: `src/main/java/com/aryston/kinetic/client/lod/TileMeshing.java`
- Role: Meshes changed tile versions/borders on the store worker, bounds ready data at 48 meshes and 32 MiB under one publication lock, rejects stale epochs and releases evicted metadata.
- Depends on: `TerrainWorker`, `LodStore`, `LodBorders`, `LodMesher`, `LodMesh`.

#### DistantTerrainRenderer
- Path: `src/main/java/com/aryston/kinetic/client/lod/DistantTerrainRenderer.java`
- Role: Selects uploaded coverage while marking all visited quadtree ancestors and waiting children as used, budgets uploads by 12 meshes/4 MiB/1.5 ms, retires replaced GPU allocations after in-flight frames complete, tests vertical frustum bounds, and batches pooled geometry into indirect opaque/sorted-water draws. Real-chunk masking uses uploaded surface sections. Handles pressure eviction, epochs, one-time inactive resource release and GPU shutdown.
- Depends on: `DistantTerrain`, `TileMeshing`, `LodSelection`, `DistantTerrainPipeline`, RenderPearl buffers and passes.

#### DistantTerrainPipeline
- Path: `src/main/java/com/aryston/kinetic/client/lod/DistantTerrainPipeline.java`
- Role: Builds instanced forward opaque and water pipelines and the four-target Helion geometry pipeline, binding coverage and lightmap samplers; distant water is opaque so tile edges never show through it. `WATER_SURFACE_PIPELINE` (`lod/distant_water_surface`) writes Helion's water surface targets (RGBA16F surface: octahedral up normal, a 64 block thickness so Helion treats it as deep water with no bottom showing through, sky light; RGBA8 tint: the water color, alpha 0.545 for the small distant roughness in Helion's tint encoding) and discards behind Helion's `SceneDepthSampler`; it clips by tile bounds and chunk coverage only (`terrainCoverageClip`), without the dithered tile fade, so the water surface has no holes at tile transitions.
- Depends on: RenderPearl pipeline API, `distant_terrain` shaders.

#### DistantTerrainFog
- Path: `src/main/java/com/aryston/kinetic/client/lod/DistantTerrainFog.java`
- Role: On `ViewportEvent.RenderFog`, stretches the atmospheric environmental fog (1024 blocks by default) and the render-distance fog to the distant terrain range, so distant terrain stays visible up to 512 chunks.
- Depends on: `DistantTerrain`, NeoForge viewport events.

#### TerrainCache
- Path: `src/main/java/com/aryston/kinetic/client/lod/TerrainCache.java`
- Role: Lazily fingerprints generation revision, seed, world/dimension, generator, structures option, data version, quality, mods, the immutable resolved palette and server/client resources on workers; revision 4 invalidates older cached tiles that retained deep underground cave intervals. Adapts the bounded disk cache and disables failing cache I/O for the level.
- Depends on: `LodDiskCache`, `LodData`.

#### TerrainMaterials
- Path: `src/main/java/com/aryston/kinetic/client/lod/TerrainMaterials.java`
- Role: Builds an immutable top/side texture-average and tint-index palette under a 1 ms/256-state tick budget and supplies a frozen fingerprint snapshot. Samples alpha-weighted linear colours on an eight-square grid; workers use copied integers, never native images or live models. Unsupported isolated tint callbacks use the base colour.
- Depends on: `LodColor`, `SavedColumnView`.

#### DistantTerrainCoverage
- Path: `src/main/java/com/aryston/kinetic/client/lod/DistantTerrainCoverage.java`
- Role: Coalesces mesh readiness notifications, queues heightmap-only requirements on load/discovery/upload independently of layered capture, processes at most 64 columns/1 ms per frame, rechecks at most 16 not-yet-covered chunks per frame round robin (rechecking every one each frame took about 29 % of the render thread), seeds existing meshes and uploads a 256-square R8 mask at most every 25 ms; clears unloaded/world state and closes its texture before device shutdown.
- Depends on: `LodCoverage`, `DistantTerrain`.

#### TerrainGeometry
- Path: `src/main/java/com/aryston/kinetic/client/lod/TerrainGeometry.java`
- Role: Suballocates immutable tile geometry in up to eight 16 MiB GPU pages, uploads only changed ranges and returns released ranges for reuse.
- Depends on: `LodAllocator`, `LodMesher`.

#### TerrainDraws
- Path: `src/main/java/com/aryston/kinetic/client/lod/TerrainDraws.java`
- Role: Writes 20-byte indexed indirect commands and 36-byte draw instances, groups consecutive shared pages and recycles staging slots only after zero-timeout fence polls. Caps staging at 32 MiB and defers submissions on exhaustion instead of waiting.
- Depends on: `TerrainGeometry`, `LodAllocator`.
