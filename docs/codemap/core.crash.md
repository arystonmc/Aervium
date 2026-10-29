# Kinetic Code Map: `com.aryston.kinetic.core.crash`

Part of the [Kinetic code map](../CODEMAP.md). Every class of this package has its entry here and nowhere else (rule D1).

### `com.aryston.kinetic.core.crash`

#### CrashSummary
- Path: `src/main/java/com/aryston/kinetic/core/crash/CrashSummary.java`
- Role: Finds Kinetic code in a crash stack (merged mixin handlers by their `kinetic$` name, frames in `com.aryston.kinetic`) and builds the lines of the crash report entry: version, module states, the code found or "Kinetic code in crash: none", one fixed `Kinetic module in crash: <key>` line per module and how to test without it.
- Depends on: nothing; the mixin lookup and module map come in as functions.
