# Kinetic Code Map: `com.aryston.kinetic.client.config`

Part of the [Kinetic code map](../CODEMAP.md). Every class of this package has its entry here and nowhere else (rule D1).

### `com.aryston.kinetic.client.config`

#### ArkeaSettings
- Path: `src/main/java/com/aryston/kinetic/client/config/ArkeaSettings.java`
- Role: Registers Kinetic's Arkea-backed config-screen factory only after `KineticClient` has confirmed that Arkea is installed.
- Depends on: `KineticConfigScreen`.

#### KineticConfigScreen
- Path: `src/main/java/com/aryston/kinetic/client/config/KineticConfigScreen.java`
- Role: Builds the Arkea settings screen with the Kinetic brand, a live distant-terrain memory meter, Configuration pages for Overview, Distant Terrain, Rendering and World, and a System Modules page.
- Depends on: `KineticOptions`, `KineticPresets`, `DistantTerrainMeter`.

#### KineticOptions
- Path: `src/main/java/com/aryston/kinetic/client/config/KineticOptions.java`
- Role: Binds Arkea controls to the NeoForge client config and to module-file switches, including option descriptions, icons, dependencies and restart requirements.
- Depends on: `KineticConfig`, `DistantTerrainLimits`, `SpawnPregenerationSettings`, `ModuleFile`, `OptimizationModule`.

#### KineticPresets
- Path: `src/main/java/com/aryston/kinetic/client/config/KineticPresets.java`
- Role: Creates the Performance, Balanced and Quality presets for distant-terrain distance, pregeneration radius, occlusion culling and leaf culling.
- Depends on: `KineticOptions`.

#### DistantTerrainMeter
- Path: `src/main/java/com/aryston/kinetic/client/config/DistantTerrainMeter.java`
- Role: Supplies the live settings-sidebar meter for the megabytes of stored distant terrain.
- Depends on: `DistantTerrain`, `ByteSizes`.

#### ModuleFile
- Path: `src/main/java/com/aryston/kinetic/client/config/ModuleFile.java`
- Role: Reads and writes `config/kinetic-modules.properties` for the module switches, using immutable `ModuleSettings` updates and logging file failures.
- Depends on: `ModuleSettings`, `OptimizationModule`, `MixinModules`.
