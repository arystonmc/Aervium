# Kinetic Architecture: Benchmark

Part of the [Kinetic architecture](../ARCHITECTURE.md).

## Benchmark

`-Dkinetic.benchmark=true` (works with `launcher/launch.ps1 -Define`) starts the benchmark from the first screen; `-Dkinetic.benchmark.quit=true` closes the game afterwards. For a fair comparison of setups every run forces borderless fullscreen, an unlimited frame rate and vsync off, ignores the frame cap of an unfocused window, raises the game window to the front and restores the options afterwards (`BenchmarkOptions`); render distance defaults to 32 unless `-Dkinetic.benchmark.renderDistance` sets it, `-Dkinetic.benchmark.simulationDistance` sets the simulation distance, `-Dkinetic.modules=off` keeps every module off for that start (`MixinModules`), and every scene reports hitches and motion error beside the frame-time and tick summaries. Reports use format version 2, so earlier reports are not compared.

1. Deletes the previous `kinetic-benchmark` world and creates a fresh one: seed 8675309, spectator, peaceful, cheats on.
2. Freezes time at noon and the weather, turns mob spawning off and finds the spawn on the server thread.
3. Captures the environment before restoring the player's options.
4. Runs every scene: force-loads its site, builds it once its entities are loaded, settles for 200 ticks while the camera moves, then measures 600 ticks. It runs the setup's per-tick commands, clears the site, removes loose item and experience-orb drops, and releases the force load before the next scene.

| Scene | Route | Stresses |
|---|---|---|
| walk | east from the spawn at walking speed, following the ground | steady terrain rendering |
| flight | south at 4 blocks per tick, 80 blocks above the spawn | chunk generation, meshing and uploads |
| underground | west at y -30 through stone and caves | occlusion, cave visibility |
| crowd | glass pen on a platform 40 blocks above spawn -96/-96: 1600 mobs of eight passive types with AI, 800 floating never-despawning items and 200 armor stands | entities and AI |
| storage | 64 x 64, two-layer platform with 8192 block entities in rows of chests, signs, banners, furnaces, hoppers, ender chests, barrels and shulker boxes | block entities |
| particles | 32 x 32 lit campfires plus flame, end rod, smoke and explosion particles every tick | particles |
| redstone | 192 observer clocks in 12 clusters | block and light updates |
| explosion | 2048 TNT lit on the first measured tick | explosions |
| server | 12 simulated players: radius-10 ender-pearl chunk tickets 40 chunks from spawn, walking one chunk outward every 20 ticks in 12 directions | integrated-server chunk loading, generation and ticks |

The report goes to `<game folder>/kinetic/benchmarks/benchmark-<yyyyMMdd-HHmmss>.json` and `.txt` and is also printed to the log. It compares itself with the newest earlier report. Repeatability target: average FPS of every scene within ±2 % between two runs on the same settings; the frame limit, vsync, window size, render distance and installed mods are written into the report because they change the result.
