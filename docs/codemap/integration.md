# Kinetic Code Map: `com.aryston.kinetic.integration`

Part of the [Kinetic code map](../CODEMAP.md). Every class of this package has its entry here and nowhere else (rule D1).

### `com.aryston.kinetic.integration`

#### CommonEvents
- Path: `src/main/java/com/aryston/kinetic/integration/CommonEvents.java`
- Role: Wires `ServerTickEvent.Pre` and `Post` and `ServerStoppedEvent` to `TickTimeRecorder` on both sides.
- Depends on: `TickTimeRecorder`.

#### SodiumClash, LoadingText
- Path: `src/main/java/com/aryston/kinetic/integration/SodiumClash.java`, `src/main/java/com/aryston/kinetic/integration/LoadingText.java`
- Role: `SodiumClash` runs first in the mod constructor and throws a NeoForge `ModLoadingException` with one error issue when Sodium is loaded, unless `-Dkinetic.benchmark=true` is set. `LoadingText` resolves the message from the mod's own language file for the language in `options.txt` (falling back to English), because the loading screen only knows FML's own translations, and doubles apostrophes for the message format.
- Depends on: nothing inside the mod.

#### CompatibilityGuard
- Path: `src/main/java/com/aryston/kinetic/integration/CompatibilityGuard.java`
- Role: Lists installed renderer mods (Sodium, which only loads next to Kinetic in the benchmark, and Iris) that turn Kinetic's optimizations off; `REPLACED_RENDERERS` is shared with `MixinModules`.
- Depends on: nothing inside the mod.

#### MixinModules
- Path: `src/main/java/com/aryston/kinetic/integration/MixinModules.java`
- Distant terrain hooks: gates uploaded-section coverage, dirty-chunk recapture and optional Helion water alongside the terrain/camera hooks.
- Role: Mixin config plugin: reads and rewrites `config/kinetic-modules.properties` before any mixin applies, skips the mixins of switched-off modules, skips renderer modules when Sodium or Iris is in the mod list, keeps `compactVertices` off when Helion is installed without its `CompactTerrainVertices` class (Helion 0.1) and skips mixins into Helion classes when Helion is not installed. `-Dkinetic.modules=off` keeps every module off for that start without touching the file. Its public `SETTINGS_FILE` identifies the module file for the settings screen.
- Depends on: `ModuleSettings`, `OptimizationModule`, `CompatibilityGuard`.
- Notes: Runs before mods are constructed, so it reads the mod list through `FMLLoader.getCurrent().getLoadingModList()`. Also keeps, for the crash report, the active state of every module (`activeModules`), the module of each mixin (`moduleOf`) and which Kinetic mixins were applied to each class with their method names (`mixinsDeclaring`, filled in `postApply`).
