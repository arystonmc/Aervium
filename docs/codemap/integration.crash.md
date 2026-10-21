# Aervium Code Map: `com.aryston.aervium.integration.crash`

Part of the [Aervium code map](../CODEMAP.md). Every class of this package has its entry here and nowhere else (rule D1).

### `com.aryston.aervium.integration.crash`

#### CrashReportSection
- Path: `src/main/java/com/aryston/aervium/integration/crash/CrashReportSection.java`
- Role: Text of the "Aervium" System Details entry: collects the frames of the exception and its causes and runs `CrashSummary` with the mod version and `MixinModules` lookups.
- Depends on: `CrashSummary`, `MixinModules`.
