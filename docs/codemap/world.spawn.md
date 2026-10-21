# Aervium Code Map: `com.aryston.aervium.world.spawn`

Part of the [Aervium code map](../CODEMAP.md). Every class of this package has its entry here and nowhere else (rule D1).

### `com.aryston.aervium.world.spawn`

#### SpawnPregeneration
- Path: `src/main/java/com/aryston/aervium/world/spawn/SpawnPregeneration.java`
- Role: Loads, and in new land generates, every chunk within the join radius around the joining player's spawn chunk while the integrated server prepares the spawn: one loading-only `PLAYER_SPAWN` ticket with one ring more than the radius (a chunk is only sent once its neighbours are loaded too), refreshed every tick until the player is placed, and a `ChunkLoadCounter` for the progress. Only for the owner of a single player world, never on a dedicated server or for LAN guests.
- Depends on: `SpawnPregenerationSettings`, `Durations`.

#### SpawnPregenerationSettings
- Path: `src/main/java/com/aryston/aervium/world/spawn/SpawnPregenerationSettings.java`
- Role: The switch and radius from the client config (6 to 32 chunks, default 32) and the render distance the client had when the world opened; `joinRadius()` is the smaller of radius and render distance, because chunks beyond the render distance are never shown on entry. Common code, so the integrated server reads it without touching client classes.
- Depends on: nothing.

#### SpawnPregenerationHolder
- Path: `src/main/java/com/aryston/aervium/world/spawn/SpawnPregenerationHolder.java`
- Role: Interface `PrepareSpawnTaskPregenerationMixin` adds to `PrepareSpawnTask`: starts the pregeneration for its player and keeps it alive until the player is placed.
- Depends on: `SpawnPregeneration`.

#### OwnerTerrainWait
- Path: `src/main/java/com/aryston/aervium/world/spawn/OwnerTerrainWait.java`
- Role: Flag the client sets while it holds the loading screen for terrain; the integrated server then keeps the owner's client-loaded timer from running out, so the player cannot be hurt behind the loading screen.
- Depends on: nothing.
