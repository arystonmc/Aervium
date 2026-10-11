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
