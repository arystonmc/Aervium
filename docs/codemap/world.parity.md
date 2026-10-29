# Kinetic Code Map: `com.aryston.kinetic.world.parity`

Part of the [Kinetic code map](../CODEMAP.md). Every class of this package has its entry here and nowhere else (rule D1).

### `com.aryston.kinetic.world.parity`

#### ParityHarness
- Path: `src/main/java/com/aryston/kinetic/world/parity/ParityHarness.java`
- Role: Active only with `-Dkinetic.parity=true` (the `parity` run). On server start freezes time, weather and mob spawning and force-loads the scenario chunks; after 40 ticks seeds the level random and the random tick position generator, builds `ParityScenario`, runs its light changes every tick, waits for the light worker after every tick, force-loads `kinetic.parity.forceRadius` chunks around 0, 0 (0), logs the light worker time before and during the scenario, logs block, light and entity hashes of the area every 200 ticks and stops the server after `kinetic.parity.ticks` (1200).
- Depends on: `ParityScenario`, `StateHash`, `LevelRandomAccessor`.

#### WorldgenCheck
- Path: `src/main/java/com/aryston/kinetic/world/parity/WorldgenCheck.java`
- Role: Active with `-Dkinetic.parity.worldgen=true` instead of the scenario. Freezes ticks, force-loads `kinetic.parity.worldgenRadius` (12) chunks around 0, 0, hashes every chunk's blocks and biomes when its terrain step finishes, and once all are full logs the combined terrain hash, missing chunks, a save and load round trip of every section through the real chunk codecs, generation time, process CPU and light worker CPU, then stops the server.
- Depends on: `StateHash`, `ServerLightTimings`, `ChunkStatusTasksTerrainMixin`.

#### ParityLoad
- Path: `src/main/java/com/aryston/kinetic/world/parity/ParityLoad.java`
- Role: Optional server load for profiling (`kinetic.parity.mobs`): spawns cows, sheep, pigs, chickens and villagers at fixed random spots over the force-loaded square, measures each server tick from `ServerTickEvent.Pre` to `Post` after 100 ticks and logs the mean and longest; also logs the `PoiAudit` summary.
- Depends on: `PoiAudit`.

#### ParityScenario
- Path: `src/main/java/com/aryston/kinetic/world/parity/ParityScenario.java`
- Role: Builds the fixed parity scenario on the flat ground: observer clock with redstone, lamps and a sticky piston, hopper chain between chests with cobblestone, furnace with raw iron and coal, 9 x 9 wheat farm around water, water and lava in a stone basin, saplings, sugar cane and torches. `changeLight` places light sources and blockers in the light lab with a fixed sequence per tick. Also defines the hashed area.
- Depends on: nothing.
