# Aervium Code Map: `com.aryston.aervium.world.light`

Part of the [Aervium code map](../CODEMAP.md). Every class of this package has its entry here and nowhere else (rule D1).

### `com.aryston.aervium.world.light`

#### ServerLightTimings
- Path: `src/main/java/com/aryston/aervium/world/light/ServerLightTimings.java`
- Role: Thread-safe totals of the server light worker batches: count, wall time, longest batch and CPU time; `take()` returns and resets them.
- Depends on: `StallMeter`.
