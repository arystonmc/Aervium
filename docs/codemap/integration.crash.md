# Kinetic Code Map: `com.aryston.kinetic.integration.crash`

Part of the [Kinetic code map](../CODEMAP.md). Every class of this package has its entry here and nowhere else (rule D1).

### `com.aryston.kinetic.integration.crash`

#### CrashReportSection
- Path: `src/main/java/com/aryston/kinetic/integration/crash/CrashReportSection.java`
- Role: Text of the "Kinetic" System Details entry: collects the frames of the exception and its causes and runs `CrashSummary` with the mod version and `MixinModules` lookups.
- Depends on: `CrashSummary`, `MixinModules`.
