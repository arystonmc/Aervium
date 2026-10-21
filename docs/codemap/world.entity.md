# Aervium Code Map: `com.aryston.aervium.world.entity`

Part of the [Aervium code map](../CODEMAP.md). Every class of this package has its entry here and nowhere else (rule D1).

### `com.aryston.aervium.world.entity`

#### SectionCounts
- Path: `src/main/java/com/aryston/aervium/world/entity/SectionCounts.java`
- Role: Walks the entity sections an entity search in a box would visit (same range as `EntitySectionStorage.forEachAccessibleNonEmptySection`) and answers whether a per-section count is zero in all of them. Used by `entityCollisions` and `hopperSearch`.
- Depends on: nothing.

#### PushRules
- Path: `src/main/java/com/aryston/aervium/world/entity/PushRules.java`
- Role: Detects a scoreboard without teams and supplies the vanilla-equivalent pushable predicate without team lookups.
- Depends on: Minecraft entity and player classes.
