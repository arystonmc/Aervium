# Aervium Features

What the latest version of Aervium can do. Written for players and ready to reuse on websites and mod pages. This file always describes the present state; history belongs in `CHANGELOG.md`.

## Summary

Aervium makes Minecraft run smoother on the Vulkan renderer: steadier frame times, fewer stutters and a server that keeps 20 ticks per second, while redstone, farms and mobs behave exactly as in vanilla. This first version brings the measuring tools every later optimization is judged with.

## At a Glance

| | |
|---|---|
| Version | 0.1 |
| Minecraft | 26.3 |
| Loader | NeoForge |
| Side | Client and server, each optional |
| Status | In development |

## Features

### Performance Overlay
See exactly how smooth your game runs, not just how fast.

- Shows the frame rate together with the 1% and 0.1% lows, the average frame time and how much it varies, and the longest frame
- A live frame time graph draws every frame as a bar: green within your target frame rate, yellow when slower, red for spikes that take twice as long
- Shows the server tick time (MSPT) of your single player world with the number of ticks over 50 ms, garbage collector pauses, RAM and video memory, and how long each part of the game takes per frame
- Press F7 to show or hide it; the target frame rate is set in the Aervium settings (Mods, Aervium, Config)
- Since: 0.1

### Built-in Benchmark
A repeatable test run so changes can be measured instead of guessed.

- Creates a fresh world from a fixed seed and flies the camera along a fixed route: walking over the surface, elytra-speed flight over new terrain, a village with a crowd of villagers, a flight through caves and a running redstone build
- Measures every scene for 30 seconds after 10 seconds of settling: frame rate, 1% and 0.1% lows, frame time deviation, server tick time distribution, garbage collector pauses, peak RAM and video memory
- Writes `aervium/benchmarks/benchmark-<date>.json` and a readable `.txt` report into the game folder and compares the result with the previous run
- Start it with the JVM argument `-Daervium.benchmark=true`; add `-Daervium.benchmark.quit=true` to close the game when it is done
- Since: 0.1

### Compatibility Guard
Aervium never fights another renderer.

- When Sodium or Iris is installed, Aervium keeps its optimizations off; the overlay and the benchmark still work, so you can compare
- Since: 0.1

### Faster Starts with Shader and Pipeline Caches
The second start of the game skips most of the shader work.

- Keeps the compiled form of every shader and every Vulkan pipeline on disk, so later starts and resource pack reloads reuse them instead of compiling them again
- The caches check the game version, the shader sources, resource packs, the GPU and the driver, and are rebuilt by themselves when any of them changes
- Stored in `aervium/cache/` in the game folder; delete the folder to start fresh
- Since: 0.1

### No Shader Hitches in the Middle of a Game
Built-in pipelines are ready before they are needed.

- Vanilla compiles a few item pipelines only when the first such item is drawn, which causes a short hitch in game; Aervium compiles them in the background while the game loads
- Since: 0.1

### Dynamic Frame Rate
Saves power and keeps your PC cool when you are not looking at the game.

- Lowers the frame rate to 30 FPS while the game window is in the background and to 60 FPS while a single player game is paused
- The world keeps running at full speed; only drawing slows down
- Turn it off or change both limits in the Aervium settings
- Since: 0.1

### Lighter Chunk Memory
Uses less memory for loaded chunks.

- Replaces a heavy safety lock inside every chunk section with a lighter one that catches the same errors, which saved about 33 MB of memory in the benchmark at render distance 16 and server ticks were faster in the same run
- Since: 0.1

### Switchable Modules
Every optimization can be turned off.

- `config/aervium-modules.properties` lists each optimization (`shaderCache`, `pipelineCache`, `pipelineWarmup`, `compactChunkLocks`) with `true` or `false`; changes apply at the next start
- Render optimizations stay off by themselves when Sodium or Iris is installed
- Since: 0.1
