# Aervium Code Map: `com.aryston.aervium.core.crash`

Part of the [Aervium code map](../CODEMAP.md). Every class of this package has its entry here and nowhere else (rule D1).

### `com.aryston.aervium.core.crash`

#### CrashSummary
- Path: `src/main/java/com/aryston/aervium/core/crash/CrashSummary.java`
- Role: Finds Aervium code in a crash stack (merged mixin handlers by their `aervium$` name, frames in `com.aryston.aervium`) and builds the lines of the crash report entry: version, module states, the code found or "Aervium code in crash: none", one fixed `Aervium module in crash: <key>` line per module and how to test without it.
- Depends on: nothing; the mixin lookup and module map come in as functions.
