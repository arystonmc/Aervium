# Aervium Code Map: `com.aryston.aervium.client.pregeneration`

Part of the [Aervium code map](../CODEMAP.md). Every class of this package has its entry here and nowhere else (rule D1).

### `com.aryston.aervium.client.pregeneration`

#### JoinPregeneration
- Path: `src/main/java/com/aryston/aervium/client/pregeneration/JoinPregeneration.java`
- Role: Client half of pregeneration on join. Adopts the load tracker of a single player world when it opens, then, from the moment the player exists until the terrain is ready, keeps vanilla's "level ready" false, blends server and terrain progress into the tracker's progress, gives the section build loop a batch of at most 64 dirty sections per frame from every direction (all sections of the occlusion graph, not only the ones in view, built first when they have no mesh yet), and spreads chunk packets (10 ms) and client light updates (6 ms) over frames so no single frame freezes. Logs how long the wait took or why it gave up.
- Depends on: `TerrainReadiness`, `PregenerationHold`, `TerrainProgress`, `JoinProgress`, `OmniFrustum`, `PendingLightUpdates`, `SpawnPregenerationSettings`, `OwnerTerrainWait`.

#### TerrainReadiness
- Path: `src/main/java/com/aryston/aervium/client/pregeneration/TerrainReadiness.java`
- Role: Counts, over a grid of the chunks within the join radius, which chunks have arrived and how many non-empty sections of the occlusion graph within one chunk less have a mesh. Each frame it revisits as many chunks as fit in 1.5 ms and keeps running totals, so a radius of 32 chunks (about 3500 chunks) never costs one frame more than that.
- Depends on: `TerrainProgress`.

#### OmniFrustum
- Path: `src/main/java/com/aryston/aervium/client/pregeneration/OmniFrustum.java`
- Role: A frustum that contains everything, so the octree of the occlusion graph lists sections in every direction.
- Depends on: nothing.

#### PendingLightUpdates
- Path: `src/main/java/com/aryston/aervium/client/pregeneration/PendingLightUpdates.java`
- Role: Interface `ClientLevelPregenerationMixin` adds to `ClientLevel`: the number of queued client light updates.
- Depends on: nothing.
