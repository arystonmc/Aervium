# Aervium Code Map: `com.aryston.aervium.core.module`

Part of the [Aervium code map](../CODEMAP.md). Every class of this package has its entry here and nowhere else (rule D1).

### `com.aryston.aervium.core.module`

#### OptimizationModule
- Path: `src/main/java/com/aryston/aervium/core/module/OptimizationModule.java`
- Role: Optimizations that are switched on or off before the game starts, with their key in `config/aervium-modules.properties`, their default and whether they touch the renderer (off when Sodium or Iris is installed).
- Depends on: nothing.

#### ModuleSettings
- Path: `src/main/java/com/aryston/aervium/core/module/ModuleSettings.java`
- Role: Reads `key=true|false` lines (unknown lines are ignored, a missing line takes the module default), creates changed immutable settings with `with`, and writes every module back so players see all switches.
- Depends on: `OptimizationModule`.
