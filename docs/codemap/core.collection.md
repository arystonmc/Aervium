# Aervium Code Map: `com.aryston.aervium.core.collection`

Part of the [Aervium code map](../CODEMAP.md). Every class of this package has its entry here and nowhere else (rule D1).

### `com.aryston.aervium.core.collection`

#### ShardedLongMap
- Path: `src/main/java/com/aryston/aervium/core/collection/ShardedLongMap.java`
- Role: Read-only `Long2ObjectOpenHashMap` view over 1024 immutable shards (by mixed key hash); answers `get`, `getOrDefault`, `containsKey`, `size` and `isEmpty`, throws on writes and iteration, `clone()` returns itself.
- Depends on: nothing (fastutil).

#### ShardedLongMapBuilder
- Path: `src/main/java/com/aryston/aervium/core/collection/ShardedLongMapBuilder.java`
- Role: Keeps a mutable mirror of a map in 1024 shards and publishes `ShardedLongMap` snapshots that copy only the shards written since the last snapshot; unchanged shards are shared between snapshots. 1024 shards instead of 128 (2026-10-16): when chunks arrive, a snapshot copies about an eighth as many entries, which showed in the slowest frames of the elytra flight.
- Members: `copyOf(Long2ObjectMap)`, `put(long, V)`, `remove(long)`, `publish()`.
- Depends on: `ShardedLongMap`.

#### ColumnIndexedLongMap
- Path: `src/main/java/com/aryston/aervium/core/collection/ColumnIndexedLongMap.java`
- Role: A `Long2ObjectOpenHashMap` of sections that also counts, per column, how many sections are stored and keeps a 64-bit mask of the sections whose value is occupied; kept by `put(long, V)` and `remove(long)`. `complete(column)` says every section is stored, `occupiedMask(column)` which ones hold a value.
- Depends on: nothing; the key layout and the occupied test come in as functions.
