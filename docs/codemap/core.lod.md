# Aervium Code Map: `com.aryston.aervium.core.lod`

Part of the [Aervium code map](../CODEMAP.md). Every class of this package has its entry here and nowhere else (rule D1).

### `com.aryston.aervium.core.lod`

#### DistantTerrainLimits
- Path: `src/main/java/com/aryston/aervium/core/lod/DistantTerrainLimits.java`
- Role: Shared distances and periodic residency targets of 4096 tiles and 256 MiB including vertical layers.
- Depends on: nothing.

#### LodSample
- Path: `src/main/java/com/aryston/aervium/core/lod/LodSample.java`
- Role: Packs height, RGB24, sky/block light, water depth and provenance/coverage into eight bytes. Orders client, saved, seeded and preview sources and encodes four sky/three block light bits plus water in the vertex metadata byte.
- Depends on: nothing.

#### LodTile
- Path: `src/main/java/com/aryston/aervium/core/lod/LodTile.java`
- Role: Holds 64-square samples, optional immutable vertical layers, version, completeness/exactness counters and payload byte accounting.
- Depends on: `LodSample`.

#### LodStore
- Path: `src/main/java/com/aryston/aervium/core/lod/LodStore.java`
- Role: Six-level store that prioritizes complete footprints before source authority, keeps complete preview and seeded samples stable against equal-priority rewrites, preserves complete client edits, near layers, mixed-level borders and periodic tile/byte residency trimming.
- Depends on: `LodTile`, `LodBorders`, `LodSelection`, `LodCombine`, `LodSample`.

#### LodCombine
- Path: `src/main/java/com/aryston/aervium/core/lod/LodCombine.java`
- Role: Combines highest visible height, averaged colour/skylight, strongest block light and majority water while propagating source/partial/void coverage.
- Depends on: `LodSample`.

#### LodMesher
- Path: `src/main/java/com/aryston/aervium/core/lod/LodMesher.java`
- Role: Merges heightfield surfaces/walls, delegates optional layers, emits opaque then water ranges with unshaded RGB and packed light in 16-byte vertices, retains vertical bounds and falls back to heightfields above 65536 layered quads.
- Depends on: `LodTile`, `LodSample`, `LodBorders`, `LodMesh`.

#### LodBorders
- Path: `src/main/java/com/aryston/aervium/core/lod/LodBorders.java`
- Role: Immutable worker-owned snapshot of the lowest known neighbor tops at all four tile edges, with exact array comparison for border-change remeshing.
- Depends on: `LodTile`.

#### LodMesh
- Path: `src/main/java/com/aryston/aervium/core/lod/LodMesh.java`
- Role: Worker-owned mesh snapshot with source version, vertices, opaque/total quad counts, vertical bounds and build-time completeness.
- Depends on: nothing.

#### LodPlanner
- Path: `src/main/java/com/aryston/aervium/core/lod/LodPlanner.java`
- Role: Plans coarse-first horizon previews, nearest first within a level, without requiring stored tiles; also counts the missing tiles and the distance of the nearest one.
- Depends on: `LodSelection`, `LodStore`, `LodQuality`.

#### LodQuality
- Path: `src/main/java/com/aryston/aervium/core/lod/LodQuality.java`
- Role: Distant terrain quality levels, each with a split factor, the finest tile level that is drawn and generated, the height grid used for generated tiles and whether block texture detail is drawn (Ultra). High and Ultra reach one block per sample near the player.
- Depends on: nothing.

#### GenerationPower
- Path: `src/main/java/com/aryston/aervium/core/lod/GenerationPower.java`
- Role: CPU usage levels mapped to worker counts with reserved processors and idle durations; Low targets one worker at 25% activity and Balanced targets 50% activity per allowed worker.
- Depends on: nothing.

#### LodSelection
- Path: `src/main/java/com/aryston/aervium/core/lod/LodSelection.java`
- Role: Quadtree selection by distance and quality; uploaded detail remains selectable after CPU sample eviction, parents stay until complete uploaded children are ready, missing multiplayer areas are optional and wanted/drawn tiles respect the real-terrain clip box.
- Depends on: `LodStore`.

#### LodLayers
- Path: `src/main/java/com/aryston/aervium/core/lod/LodLayers.java`
- Role: Owns validated column offsets and up to eight descending nonoverlapping spans per column, each with packed surface, signed bottom and side RGB; builder transfers owned arrays across worker boundaries.

#### LodData
- Path: `src/main/java/com/aryston/aervium/core/lod/LodData.java`
- Role: Owns one complete-sized sample array and optional matching layer snapshot, with exactness validation for cache separation.

#### LodLayerMesher
- Path: `src/main/java/com/aryston/aervium/core/lod/LodLayerMesher.java`
- Role: Merges layer tops/undersides and wall runs, subtracts neighboring occupied intervals, preserves air gaps and underwater ground, and emits separate side colours and water ranges.

#### LodColor
- Path: `src/main/java/com/aryston/aervium/core/lod/LodColor.java`
- Role: Multiplies texture RGB by biome/material tint without affecting metadata.

#### LodDiskCache
- Path: `src/main/java/com/aryston/aervium/core/lod/LodDiskCache.java`
- Role: Versioned keyed CRC-checked DEFLATE cache with bounded inflation, atomic publication, concurrent-writer tolerance, corrupt-entry repair and a periodic 512 MiB per-identity trim.

#### LodAllocator
- Path: `src/main/java/com/aryston/aervium/core/lod/LodAllocator.java`
- Role: Aligns shared-page allocations, coalesces freed intervals and enforces the page count limit.

#### LodTransitions
- Path: `src/main/java/com/aryston/aervium/core/lod/LodTransitions.java`
- Role: Partitions incoming tile regions against previous coverage, fills previously missing areas and retains complementary parent/child transitions for 250 ms before releasing old keys.

#### LodCoverage
- Path: `src/main/java/com/aryston/aervium/core/lod/LodCoverage.java`
- Role: Tracks known height requirements and uploaded surface sections independently from layered capture, delays then fades coverage, handles negative ring coordinates and removes unloaded/distant state.
