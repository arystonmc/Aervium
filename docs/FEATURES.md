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

### Lighter Light Updates
Light changes no longer copy the whole light map of the world.

- After every batch of light updates the game copied the light data of all loaded sections so other threads could read it safely; Aervium copies only the parts that changed and shares the rest, with exactly the same result
- With the default garbage collector this cut garbage collection time and memory peaks in the benchmark (heap peak 4.0 GB to 3.0 GB, 0.1% low 14.6 to 17.9 FPS)
- Since: 0.1

### Faster Chunk Drawing
Prepares the list of chunks to draw with much less work every frame.

- Remembers where each chunk's geometry sits on the GPU instead of searching for it again in every pass, which matters most with Helion's shadows; in the benchmark with Helion at render distance 32 the 1% low rose from 36 to 51 FPS and caves from 58 to 78 FPS
- Since: 0.1

### Dedicated Chunk Builders
Chunks are prepared for drawing on Aervium's own worker threads.

- In single player, vanilla prepares chunks on the same threads that generate and save the world, so new chunks wait behind world generation when you fly or run into new land; Aervium gives chunk building its own threads (all cores but two, up to eight)
- The overlay (F7) shows chunk builds per second, how busy the workers are and how many builds are waiting; the benchmark log prints the same per scene
- The overlay and the benchmark log also show how long uploading new chunks to the graphics card takes and how long the game waits for the chunk builders
- Since: 0.1

### Occlusion Culling
Skips terrain you cannot see because solid blocks are in the way.

- Caves, tunnels and rooms hidden behind stone or walls are not sent to the graphics card; the picture stays exactly the same, only frames get cheaper where the GPU was the limit
- With Helion, chunks the sun cannot see (buried under other terrain) are left out of the shadow maps; shadows look the same because the terrain above already casts them
- Mobs, dropped items, armor stands, chests, signs and other block entities behind walls are skipped the same way, and so are particles like smoke, flames and drips; with Helion, entities under open sky are still drawn so their sun shadows stay
- F6 turns it on and off in game to compare; the overlay (F7) shows how many chunks, entities and particles were skipped
- Since: 0.1

### Leaf Culling (Optional)
Draws fewer hidden leaf faces inside trees.

- With detailed leaves, vanilla also draws the faces between leaves deep inside a tree; with this setting on, faces two or more leaves deep are skipped and forests draw faster
- Off by default, because trees look slightly less dense through the gaps in the leaves; turn on `leafCulling` in the Aervium settings, the chunks rebuild right away
- Since: 0.1

### Garbage Collector Advice
Tells you which Java settings give the smoothest frames.

- The log names the garbage collector and the memory limit at start and suggests `-XX:+UseZGC` when the game runs with a collector that pauses the game; a mod cannot change this itself, it goes into the launcher's JVM arguments
- Benchmark reports record the collector, because it changes the lows: in the benchmark ZGC raised the 1% low from 34 to 47 FPS and the 0.1% low from 12 to 32 FPS against the default G1
- Since: 0.1

### First Launch Hardware Setup
On its first launch Aervium looks at the PC and lightens settings a weak machine cannot carry.

- Sorts the PC into low, medium or high from its CPU threads, Java memory limit, RAM, graphics card type (dedicated, integrated or software) and video memory; the weakest part decides
- Low: render distance at most 8, simulation distance at most 6, leaf culling on. Medium: render distance at most 12, simulation distance at most 8. High: nothing changes
- Only lowers, never raises, and only once; a toast says when something changed. `config/aervium-hardware.properties` records the class and the changes; delete it to detect again
- Since: 0.1

### Crash Report Help
When the game crashes, the crash report says whether Aervium was involved.

- Every crash report gets an "Aervium" entry in its System Details: the version, which modules were on, and the Aervium code that ran in the crashing stack, or "none"
- When a module's code was in the crash, the report names it on a fixed line (`Aervium module in crash: occlusionCulling`) and says how to test without it
- The crash helper on the site reads these lines, so a known problem can point straight to its fix
- Since: 0.1

### Switchable Modules
Every optimization can be turned off.

- `config/aervium-modules.properties` lists each optimization (`shaderCache`, `pipelineCache`, `pipelineWarmup`, `compactChunkLocks`, `sectionSliceCache`, `lightSnapshots`, `occlusionCulling`, `meshWorkers`) with `true` or `false`; changes apply at the next start
- Render optimizations stay off by themselves when Sodium or Iris is installed
- Since: 0.1
