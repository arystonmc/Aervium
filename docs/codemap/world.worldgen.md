# Kinetic Code Map: `com.aryston.kinetic.world.worldgen`

Part of the [Kinetic code map](../CODEMAP.md). Every class of this package has its entry here and nowhere else (rule D1).

### `com.aryston.kinetic.world.worldgen`

#### ParallelFeatures
- Path: `src/main/java/com/aryston/kinetic/world/worldgen/ParallelFeatures.java`
- Role: Runs the feature step of a chunk (trees, ores, plants, structure pieces) on the `Kinetic Worldgen` job system instead of vanilla's single `worldgen` executor. Each step holds the 3 x 3 chunks around its chunk in `RegionLocks`, the radius vanilla lets a feature step write (`blockStateWriteRadius(1)`), so two steps that could touch the same chunk never run at once.
- Depends on: `JobSystem`, `RegionLocks`.
