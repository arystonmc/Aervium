# Aervium Code Map: `com.aryston.aervium.world`

Part of the [Aervium code map](../CODEMAP.md). Every class of this package has its entry here and nowhere else (rule D1).

### `com.aryston.aervium.world`

#### TickTimeRecorder
- Path: `src/main/java/com/aryston/aervium/world/TickTimeRecorder.java`
- Role: Records the duration of every server tick of the integrated or dedicated server into a ring buffer and the `SERVER_TICK` timing scope.
- Members:
  - `SERVER`: the instance the server events write to.
  - `mark()`, `since(long)`, `latest(int)`: copies for the overlay and the benchmark.
- Depends on: `SampleRing`, `SystemTimings`.
- Notes: Written on the server thread and read on the client thread, so the ring is accessed under a lock.
