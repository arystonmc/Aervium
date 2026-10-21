# Aervium Code Map: `com.aryston.aervium.world.collision`

Part of the [Aervium code map](../CODEMAP.md). Every class of this package has its entry here and nowhere else (rule D1).

### `com.aryston.aervium.world.collision`

#### CollidableEntities, CollisionAudit
- Path: `src/main/java/com/aryston/aervium/world/collision/CollidableEntities.java`, `src/main/java/com/aryston/aervium/world/collision/CollisionAudit.java`
- Role: `CollidableEntities` tells per class (cached in a `ClassValue`) whether an entity class overrides `canBeCollidedWith` below `Entity` (it may be bumped into) and whether a source overrides `canCollideWith` (it has its own rule); defines the per-section counter interface. `CollisionAudit` (`-Daervium.collisionAudit=true`) counts skipped searches and the ones where vanilla found an entity.
- Depends on: nothing.

#### EntityCollisionShapes
- Path: `src/main/java/com/aryston/aervium/world/collision/EntityCollisionShapes.java`
- Role: Shared client and server implementation of `getEntityCollisions`. Skips the entity search only when the sections vanilla would inspect have no collidable entity and the level has no dragon parts; otherwise builds vanilla-equivalent collision shapes and audits each skipped search.
- Depends on: `CollidableEntities`, `CollisionAudit`, `SectionCounts`.
