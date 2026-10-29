# Kinetic Code Map: `com.aryston.kinetic.mixin` (world-server)

Part of the [Kinetic code map](../CODEMAP.md). The package is split by topic; this part covers chunk memory, worldgen, POI, collisions, hoppers, compression, light storage, pushing. Every class of the package has its entry in exactly one of these files (rule D1).

### `com.aryston.kinetic.mixin`

#### PalettedContainerMixin
- Path: `src/main/java/com/aryston/kinetic/mixin/PalettedContainerMixin.java`
- Role: Replaces the per-container `ThreadingDetector` (a semaphore, a lock and their syncs, about 128 bytes in every chunk section container) with one `AtomicBoolean`; a second thread entering a container in use still ends in the vanilla "Accessing PalettedContainer from multiple threads" crash report, now with the stack of the thread that entered second.
- Notes: Module `compactChunkLocks`, both sides. Saves about 100 bytes per container: 48.3 MB of lock objects became 15.5 MB with 332000 containers (client and integrated server) in the benchmark village at render distance 16, and the compare-and-set is cheaper than a semaphore and a lock per access (average MSPT was 12 % lower with all modules on in the first comparison; to be confirmed with more runs). Game behavior is unchanged; only the diagnostic of a threading bug differs.

#### ChunkStatusTasksFeaturesMixin
- Path: `src/main/java/com/aryston/kinetic/mixin/ChunkStatusTasksFeaturesMixin.java`
- Role: Wraps `ChunkStatusTasks.generateFeatures` (`@WrapMethod`) and returns the future of `ParallelFeatures.place`; the vanilla body runs unchanged on the worker.
- Notes: Module `parallelFeatures`, listed under `server` in the mixin configuration so it applies on dedicated servers only. The step is a static method of the generation pyramid and has no event.

#### SectionStoragePoiIndexMixin
- Path: `src/main/java/com/aryston/kinetic/mixin/SectionStoragePoiIndexMixin.java`
- Role: When a `PoiManager` is constructed, swaps its empty section map for a `ColumnIndexedLongMap` from `PoiColumns` (levels taller than 64 sections keep the vanilla map).
- Notes: Module `poiSearch`.

#### PoiManagerColumnSearchMixin
- Path: `src/main/java/com/aryston/kinetic/mixin/PoiManagerColumnSearchMixin.java`
- Role: At the head of `PoiManager.getInChunk`, answers from the occupied sections of a fully loaded column; columns not fully in memory fall through to vanilla, which loads them. With `PoiAudit.ENABLED` also runs the vanilla search and compares.
- Notes: Module `poiSearch`. Uses `SectionStorageAccessor` for the map and the level height.

#### EntitySectionCollidableMixin
- Path: `src/main/java/com/aryston/kinetic/mixin/EntitySectionCollidableMixin.java`
- Role: Counts the entities in an entity section that may be collided with, on `add` and on successful `remove`.
- Notes: Module `entityCollisions`.

#### ServerLevelEntityCollisionsMixin
- Path: `src/main/java/com/aryston/kinetic/mixin/ServerLevelEntityCollisionsMixin.java`
- Role: Overrides `getEntityCollisions` in `ServerLevel` and delegates its collision-shape search to `EntityCollisionShapes`, using `PersistentEntitySectionManagerAccessor` for the section storage.
- Notes: Module `entityCollisions`.

#### ClientLevelEntityCollisionsMixin
- Path: `src/main/java/com/aryston/kinetic/mixin/ClientLevelEntityCollisionsMixin.java`
- Role: Adds `getEntityCollisions` to `ClientLevel` and delegates its collision-shape search to `EntityCollisionShapes`, using `TransientEntitySectionManagerAccessor` for the section storage.
- Notes: Module `entityCollisions`; a mixin is required because `ClientLevel` does not implement this collision query.

#### ParticleCollisionMixin
- Path: `src/main/java/com/aryston/kinetic/mixin/ParticleCollisionMixin.java`
- Role: Wraps `Entity.collideBoundingBox` in `Particle.move` and returns the unchanged movement when `ParticleCollisions` proves that the swept area contains only air and has no entity collider.
- Notes: Module `particleCollisions`; a mixin is required because `Particle.move` has no API hook around the block collision call.

#### EntitySectionHopperMixin
- Path: `src/main/java/com/aryston/kinetic/mixin/EntitySectionHopperMixin.java`
- Role: Counts item entities and entities that may hold items in an entity section, on `add` and on successful `remove`.
- Notes: Module `hopperSearch`.

#### HopperBlockEntityItemSearchMixin, HopperEntityContainerSearchMixin
- Path: `src/main/java/com/aryston/kinetic/mixin/HopperBlockEntityItemSearchMixin.java`, `src/main/java/com/aryston/kinetic/mixin/HopperEntityContainerSearchMixin.java`
- Role: At the head of `HopperBlockEntity.getItemsAtAndAbove` and NeoForge `VanillaInventoryCodeHooks.getEntityContainerOrHandler`, return an empty result when the section counts show nothing to find; with the audit on they let vanilla run and check its result at return.
- Notes: Module `hopperSearch`.

#### CompressionEncoderLevelMixin
- Path: `src/main/java/com/aryston/kinetic/mixin/CompressionEncoderLevelMixin.java`
- Role: Replaces the `new Deflater()` (level 6) in the `CompressionEncoder` constructor with `PacketCompression.newDeflater()`. Only network bytes change: the client inflates any level, and the decompressed packets are the same.
- Notes: Module `networkCompression`. Vanilla offers no way to choose the compression level.

#### EntityCapabilityAccessor
- Path: `src/main/java/com/aryston/kinetic/mixin/EntityCapabilityAccessor.java`
- Role: Accessor for the NeoForge `providers` map of an `EntityCapability`, read by `HopperEntities`.
- Notes: Module `hopperSearch`.

#### ServerLevelEntityManagerAccessor
- Path: `src/main/java/com/aryston/kinetic/mixin/ServerLevelEntityManagerAccessor.java`
- Role: Accessor for the private `entityManager` of `ServerLevel`, used by the entity section searches.
- Notes: Modules `entityCollisions` and `hopperSearch`.

#### PersistentEntitySectionManagerAccessor
- Path: `src/main/java/com/aryston/kinetic/mixin/PersistentEntitySectionManagerAccessor.java`
- Role: Accessor for the private `sectionStorage` of `PersistentEntitySectionManager`.
- Notes: Modules `entityCollisions` and `hopperSearch`.

#### TransientEntitySectionManagerAccessor
- Path: `src/main/java/com/aryston/kinetic/mixin/TransientEntitySectionManagerAccessor.java`
- Role: Accessor for the private `sectionStorage` of `TransientEntitySectionManager`.
- Notes: Module `entityCollisions`; a mixin is required because the client entity section storage has no public accessor.

#### SectionStorageAccessor
- Path: `src/main/java/com/aryston/kinetic/mixin/SectionStorageAccessor.java`
- Role: Accessors for the section map (`storage`) and the level height of a `SectionStorage`, used by `SectionStoragePoiIndexMixin`.
- Notes: Module `poiSearch`.

#### DataLayerStorageMapMixin
- Path: `src/main/java/com/aryston/kinetic/mixin/DataLayerStorageMapMixin.java`
- Role: Follows every write of a light storage map (`setLayer`, `copyDataLayer`, `removeLayer`) in a `ShardedLongMapBuilder` and implements `LightSnapshotSource`; the builder starts at the first copy, and a map that already is a snapshot returns itself.
- Notes: Module `lightSnapshots`, both sides.

#### BlockDataLayerStorageMapMixin, SkyDataLayerStorageMapMixin
- Path: `src/main/java/com/aryston/kinetic/mixin/BlockDataLayerStorageMapMixin.java`, `src/main/java/com/aryston/kinetic/mixin/SkyDataLayerStorageMapMixin.java`
- Role: In `copy()` of the block and sky light maps, replace the `clone()` of the whole section map with `LightSnapshotSource.kinetic$snapshot()`.
- Notes: Module `lightSnapshots`, both sides. Vanilla `LayerLightSectionStorage.swapSectionMap` cloned the full map (several MB at render distance 32) after every light batch so other threads read a stable copy; the snapshot is still a complete, never-changing copy published at the same moment, so readers see exactly the vanilla data. It was about 18 % of all allocation; with G1 the benchmark spent 9 % less time in GC pauses, the 0.1% low rose from 14.6 to 17.9 FPS and the heap peak fell from 4014 to 3046 MB (1% low unchanged); with ZGC the difference was within noise.

#### EntitySelectorPushMixin
- Path: `src/main/java/com/aryston/kinetic/mixin/EntitySelectorPushMixin.java`
- Role: Returns the teamless pushable predicate from `EntitySelector.pushableBy` when the level scoreboard has no teams.
- Notes: Module `teamlessPushing`. No API avoids the team lookups inside `EntitySelector.pushableBy`.
