# Aervium Code Map: `com.aryston.aervium.client.config`

Part of the [Aervium code map](../CODEMAP.md). Every class of this package has its entry here and nowhere else (rule D1).

### `com.aryston.aervium.client.config`

#### ArkeaSettings
- Path: `src/main/java/com/aryston/aervium/client/config/ArkeaSettings.java`
- Role: Registers Aervium's Arkea-backed config-screen factory only after `AerviumClient` has confirmed that Arkea is installed.
- Depends on: `AerviumConfigScreen`.

#### AerviumConfigScreen
- Path: `src/main/java/com/aryston/aervium/client/config/AerviumConfigScreen.java`
- Role: Builds the Arkea settings screen with the Aervium brand, a live distant-terrain memory meter, Configuration pages for Overview, Distant Terrain, Rendering and World, and a System Modules page.
- Depends on: `AerviumOptions`, `AerviumPresets`, `DistantTerrainMeter`.

#### AerviumOptions
- Path: `src/main/java/com/aryston/aervium/client/config/AerviumOptions.java`
- Role: Binds Arkea controls to the NeoForge client config and to module-file switches, including option descriptions, icons, dependencies and restart requirements.
- Depends on: `AerviumConfig`, `DistantTerrainLimits`, `SpawnPregenerationSettings`, `ModuleFile`, `OptimizationModule`.

#### AerviumPresets
- Path: `src/main/java/com/aryston/aervium/client/config/AerviumPresets.java`
- Role: Creates the Performance, Balanced and Quality presets for distant-terrain distance, pregeneration radius, occlusion culling and leaf culling.
- Depends on: `AerviumOptions`.

#### DistantTerrainMeter
- Path: `src/main/java/com/aryston/aervium/client/config/DistantTerrainMeter.java`
- Role: Supplies the live settings-sidebar meter for the megabytes of stored distant terrain.
- Depends on: `DistantTerrain`, `ByteSizes`.

#### ModuleFile
- Path: `src/main/java/com/aryston/aervium/client/config/ModuleFile.java`
- Role: Reads and writes `config/aervium-modules.properties` for the module switches, using immutable `ModuleSettings` updates and logging file failures.
- Depends on: `ModuleSettings`, `OptimizationModule`, `MixinModules`.
