# Aervium Code Map: `com.aryston.aervium.world.hopper`

Part of the [Aervium code map](../CODEMAP.md). Every class of this package has its entry here and nowhere else (rule D1).

### `com.aryston.aervium.world.hopper`

#### HopperEntities, HopperAudit
- Path: `src/main/java/com/aryston/aervium/world/hopper/HopperEntities.java`, `src/main/java/com/aryston/aervium/world/hopper/HopperAudit.java`
- Role: `HopperEntities` tells which entities count as items and which may hold items for a hopper (`Container` entities and entity types with a NeoForge `Capabilities.Item.ENTITY_AUTOMATION` provider, read through `EntityCapabilityAccessor`), answers whether the sections around a search box hold none (server levels without part entities only), and defines the per-section counter interface. `HopperAudit` (`-Daervium.hopperAudit=true`) counts skippable searches, all searches, and skippable ones where vanilla found an entity.
- Depends on: `SectionCounts`, `ServerLevelEntityManagerAccessor`, `PersistentEntitySectionManagerAccessor`.
