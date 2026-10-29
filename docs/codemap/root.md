# Kinetic Code Map: `com.aryston.kinetic`

Part of the [Kinetic code map](../CODEMAP.md). Every class of this package has its entry here and nowhere else (rule D1).

### `com.aryston.kinetic`

#### Kinetic
- Path: `src/main/java/com/aryston/kinetic/Kinetic.java`
- Role: Entry point annotated with `@Mod` for both sides; registers the common events.
- Members:
  - `MOD_ID`: the mod id.
  - `LOGGER`: shared logger for the whole mod.
- Depends on: `CommonEvents`.
