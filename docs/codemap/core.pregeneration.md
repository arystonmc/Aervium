# Aervium Code Map: `com.aryston.aervium.core.pregeneration`

Part of the [Aervium code map](../CODEMAP.md). Every class of this package has its entry here and nowhere else (rule D1).

### `com.aryston.aervium.core.pregeneration`

#### TerrainProgress
- Path: `src/main/java/com/aryston/aervium/core/pregeneration/TerrainProgress.java`
- Role: Record of the terrain around the joining player: chunks arrived and required, sections built and required, client light updates still queued. `fraction()` gives the first half to chunks and the second to built sections; `complete()` needs every chunk, every section and no queued light update; `advancedFrom` tells the stall timer that something moved.
- Depends on: nothing.

#### PregenerationHold
- Path: `src/main/java/com/aryston/aervium/core/pregeneration/PregenerationHold.java`
- Role: Decides when the loading screen may close: `READY` once the terrain stayed complete for the chunk fade-in time, `STALLED` when nothing advanced for 15 s, `TIMED_OUT` after 90 s, otherwise `WAITING`.
- Depends on: `TerrainProgress`.

#### JoinProgress
- Path: `src/main/java/com/aryston/aervium/core/pregeneration/JoinProgress.java`
- Role: Blends the server's spawn area progress (first 60 %) with the client terrain progress (last 40 %) into the one value the loading screen shows.
- Depends on: nothing.
