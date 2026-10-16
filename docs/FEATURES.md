# Aervium Features

What the latest version of Aervium can do. Written for players and ready to reuse on websites and mod pages. This file always describes the present state; history belongs in `CHANGELOG.md`.

## Summary

Aervium makes Minecraft run smoother on the Vulkan renderer: steadier frame times, fewer stutters and a server that keeps 20 ticks per second, while redstone, farms and mobs behave exactly as in vanilla. It skips terrain, mobs and particles hidden behind blocks, lets the graphics card build its own terrain draw list, caches shaders so games start faster without hitches, speeds up villagers, mobs, hoppers and world generation on the server, and measures all of it with a built-in overlay and benchmark. Every optimization can be switched off on its own.

## At a Glance

| | |
|---|---|
| Version | 0.1 |
| Minecraft | 26.3 |
| Loader | NeoForge |
| Side | Client and server, each optional |
| Status | Ready for 0.1, not released yet |

## Features

### Performance Overlay
See exactly how smooth your game runs, not just how fast.

- Shows the frame rate together with the 1% and 0.1% lows, the average frame time and how much it varies, and the longest frame
- A live frame time graph draws every frame as a bar: green within your target frame rate, yellow when slower, red for spikes that take twice as long
- The graph is drawn as one piece, so showing the overlay no longer costs a noticeable part of the frame
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
- Writes the draw commands straight into GPU memory instead of creating thousands of small objects every frame (`directDraws`); without Helion the benchmark average rose by 6 to 9 % and the 1% low by 10 to 16 %
- Checks which chunks need rebuilding from a compact list instead of looking up every visible chunk every frame (`dirtySections`); in caves, where thousands of chunks are visible but hidden behind stone, the average rose by 7 % and the lows by 11 to 14 %
- On graphics cards that support it, the graphics card itself builds the list of solid and leafy terrain to draw and skips faces turned away from you (`gpuCulling`); the main thread only names the chunks that occlusion culling kept, and the work on the card takes about 0.02 ms per frame; without Helion the lows rose by 4 to 5 %, with Helion it changes nothing; other cards keep the CPU path
- When you cross a chunk border, only the columns of chunks that move are updated instead of all of them, about 100,000 at render distance 32 (`sectionGridMoves`); together with lighter light snapshots the 1% low rose by 6 % and by up to 15 % in the elytra flight
- Since: 0.1

### Dedicated Chunk Builders
Chunks are prepared for drawing on Aervium's own worker threads.

- In single player, vanilla prepares chunks on the same threads that generate and save the world, so new chunks wait behind world generation when you fly or run into new land; Aervium gives chunk building its own threads (all cores but two, up to eight)
- The overlay (F7) shows chunk builds per second, how busy the workers are and how many builds are waiting; the benchmark log prints the same per scene
- The overlay and the benchmark log also show how long uploading new chunks to the graphics card takes and how long the game waits for the chunk builders
- Since: 0.1

### Faster Villager Searches
Villagers find beds, job sites and bells with far less work.

- About every second, every villager without a bed or job searches 81 chunks of 24 sections each for one, and nearly all of those sections hold nothing; Aervium keeps a per-chunk list of the sections that hold points of interest and visits only those
- The villagers find exactly the same places in the same order as in vanilla; a built-in audit compares both searches and CI runs it on every change
- Cloud measurement with 600 animals and villagers on 289 loaded chunks: server tick 21.7 ms to 18.1 ms
- Module `poiSearch`
- Since: 0.1

### Faster Mob Movement
Moving mobs stop searching for entities they could never bump into.

- Every step of every mob asks which nearby entities it would collide with, and vanilla checks every entity around it although only boats, shulkers and happy ghasts can be bumped into; Aervium counts those per chunk section and skips the search where there are none
- Same results as vanilla; a built-in audit checks every skipped search and CI runs it
- Cloud profile with 600 mobs: the entity collision search went from about 6 % to under 1 % of the server tick (2 to 5 % faster ticks)
- Module `entityCollisions`
- Since: 0.1

### Faster Idle Hoppers
Hoppers waiting for items stop searching for entities that are not there.

- Every hopper with open space above or in front searches, each tick it is not busy, for dropped items and for chest minecarts or chest boats; Aervium counts dropped items and item-holding entities per chunk section and skips the search where there are none
- Same results as vanilla, including NeoForge item capabilities on entities from other mods; a built-in audit checks every skippable search and CI runs it
- Cloud load with 300 mobs and 2000 idle hoppers: server tick CPU 14.5 ms to 12.9 ms (about 11 % faster, three runs each); no change where minecarts or items sit next to most hoppers
- Module `hopperSearch`
- Since: 0.1

### Parallel World Generation
Dedicated servers give new chunks their trees, ores and plants on several threads at once.

- Vanilla builds the terrain of new chunks in parallel but places every feature (trees, ores, flowers, structure pieces) one chunk after another on a single thread, which limits how fast new land appears while flying or exploring
- Aervium places features of chunks that cannot touch each other at the same time; chunks whose feature areas overlap still wait for each other, just as vanilla lets a feature write only into the 3 x 3 chunks around its own
- Only on dedicated servers: in singleplayer the extra threads took processor time from drawing, and the elytra flight of the benchmark fell from 124 to 102 to 109 FPS (1% low 52 to 55 down to 38 to 41), so singleplayer keeps vanilla's order
- Measured on Can's PC with a dedicated server: 625 new chunks in 4.8 s instead of 6.3 s, 2401 chunks in 11.1 s instead of 15.3 s (27 % faster); the terrain is identical to vanilla; where the features of two neighbouring chunks overlap, which one lands first varies from run to run, as it already does in vanilla
- Module `parallelFeatures`
- Since: 0.1

### Faster Packet Compression
Servers compress what they send to players about twice as fast.

- On servers and LAN worlds, packets larger than the compression threshold are compressed with zlib level 4 instead of vanilla's level 6; players with or without Aervium connect as before
- Measured on Can's PC with 1024 real chunks (45 MB): 834 ms to 399 ms (2.1 times faster) for 4.4 % more bytes; singleplayer worlds never compress, so nothing changes there
- Module `networkCompression`
- Since: 0.1

### Occlusion Culling
Skips terrain you cannot see because solid blocks are in the way.

- Caves, tunnels and rooms hidden behind stone or walls are not sent to the graphics card; the picture stays exactly the same, only frames get cheaper where the GPU was the limit
- With Helion, chunks the sun cannot see (buried under other terrain) are left out of the shadow maps; shadows look the same because the terrain above already casts them
- Mobs, dropped items, armor stands, chests, signs and other block entities behind walls are skipped the same way, and so are particles like smoke, flames and drips; with Helion, entities under open sky are still drawn so their sun shadows stay
- The visibility tests for the camera and for Helion's shadows run on their own background threads, so they no longer cost frame time on the main thread
- When a frame takes longer than usual and the camera moved past the last visibility result, that result is stretched to the new position instead of drawing every chunk for a few frames, so one slow frame no longer causes the next ones
- Stretching a result costs the main thread no extra visibility tests: the background thread already measured how far each hidden chunk stays hidden
- F6 turns it on and off in game to compare; the overlay (F7) shows how many chunks, entities and particles were skipped
- Since: 0.1

### Face Culling
Sends only the block faces that can face you to the graphics card.

- Faces on the far side of every chunk are left out before they reach the graphics card (about a third to a half of all solid faces); the picture stays exactly the same
- Since: 0.1

### Compact Chunk Vertices
Stores terrain in a smaller format so the graphics card has less to read.

- Solid and cutout terrain uses 16 instead of 28 bytes per vertex: about a quarter to a third less video memory at render distance 32 and less work for every pass that draws terrain, including Helion's shadows; the picture stays the same (in Helion's visual test the terrain matched pixel for pixel)
- Measured on Can's PC with Helion: 5 % higher average frame rate, slightly higher 1% and 0.1% lows, 20 to 32 % less video memory
- On by default; stays off by itself with a Helion version that cannot read the format (Helion 0.1), so terrain never breaks; turn it off with `compactVertices=false` in `config/aervium-modules.properties`
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

- `config/aervium-modules.properties` lists each optimization (`shaderCache`, `pipelineCache`, `pipelineWarmup`, `compactChunkLocks`, `sectionSliceCache`, `lightSnapshots`, `occlusionCulling`, `meshWorkers`, `faceCulling`, `poiSearch`, `entityCollisions`, `hopperSearch`, `compactVertices`, `networkCompression`, `parallelFeatures`, `blockEntitySections`, `directDraws`, `dirtySections`, `gpuCulling`, `sectionGridMoves`) with `true` or `false`; changes apply at the next start; every module starts on
- Render optimizations stay off by themselves when Sodium or Iris is installed
- Since: 0.1
