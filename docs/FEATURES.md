# Kinetic Features

What the latest version of Kinetic can do. Written for players and ready to reuse on websites and mod pages. This file always describes the present state; history belongs in `CHANGELOG.md`.

## Summary

Kinetic makes Minecraft run smoother on the Vulkan renderer: steadier frame times, fewer stutters and a server that keeps 20 ticks per second, while redstone, farms and mobs behave exactly as in vanilla. It draws terrain far beyond the normal render distance, computing unvisited land from the world seed in single player, skips terrain, mobs and particles hidden behind blocks, lets the graphics card build its own terrain draw list, caches shaders so games start faster without hitches, opens single player worlds with the land around you already built, speeds up villagers, mobs, hoppers and world generation on the server, and measures all of it with a built-in overlay and benchmark. Every optimization can be switched off on its own.

## At a Glance

| | |
|---|---|
| Version | 1.0 |
| Minecraft | 26.3 |
| Loader | NeoForge |
| Side | Client and server, each optional |
| Status | Released |

## Features

### Distant Terrain
The landscape stays visible far past Minecraft's normal render distance, including places you have never visited.

- Draws simplified terrain up to 512 chunks away and caches seed results on disk for later visits
- In single player, land you have not visited is computed from the world seed in the background (heights and biome colours, without creating, loading or saving chunks): the whole horizon appears coarse first, then the area near you gets finer
- Chunks you see replace the computed land with their real look, and block or light changes refresh them
- Keeps up to eight vertical layers for seen chunks, so canopies, ground and overhangs stay apart, with texture-average colours, biome tint and light; distant water is drawn solid
- On servers the seed is unknown, so only terrain seen in the current session is drawn
- Works on both Minecraft's normal renderer and Helion, matching the fog beyond the real terrain
- Fog stretches with the distant terrain distance, so even 512 chunks stay visible; real terrain below you stays drawn when you fly high above it
- Distant Terrain is on by default; set its distance from 64 to 512 chunks in the Kinetic settings or with the 64, 128, 256 and 512 chunk preset buttons, with 256 chunks as the default
- Quality (Low, Medium, High, Ultra) sets how fine the terrain near you is: High and Ultra reach full block detail near you with denser computed heights, and Ultra adds block texture detail to the terrain; CPU Usage (Low, Balanced, High, Extreme) limits generation threads (Balanced works like High while more than 32 tiles are missing, so a new area fills in quickly), with extra idle time on Low and Balanced to leave room for the game
- Complete coarse terrain stays until its finer replacement is uploaded; uploaded detail survives sample-memory cleanup. Changes dissolve over a short transition and actual uploaded real-chunk coverage controls masking without waiting for full layered capture. Neighboring height differences get deeper edge walls, and flat surfaces use fewer triangles
- Capturing and uploading share work over frames. Sample/layer storage is periodically trimmed toward 128 MiB; shared geometry pages are capped at 128 MiB, and queues/staging have separate limits. These are component limits, not total memory measurements
- Rejects tiles outside the camera view and batches shared GPU buffers into indirect draws; resource reloads rebuild colours and discard stale work
- The F3 debug screen and overlay show recorded chunks, generated tiles, tiles, sample memory, meshes and drawn tiles
- Since: 1.0

### Smooth Zoom
Focus on distant terrain and structures with a smooth, controllable zoom.

- Hold `C` to smoothly zoom in; release to return to normal field of view
- Scroll the mouse wheel while holding `C` to dynamically zoom closer or further away (from 1.2x up to 30x) without changing your hotbar slot
- Mouse sensitivity scales proportionally with zoom level for steady and accurate aiming
- Hand and viewmodel remain at normal field of view
- Since: 1.0

### Performance Diagnostics & F3 Screen
See exactly how smooth your game runs, directly in the F3 debug screen without cluttering your view.

- Displays compact, non-intrusive performance lines in the F3 screen: frame rate, 1% lows, frame time, MSPT, memory, occlusion culling stats, and distant terrain metrics
- Shows hitches, motion error, garbage collector pauses, RAM and video memory
- Full telemetry overlay can also be enabled in the Kinetic configuration
- Since: 1.0

### Built-in Benchmark
A repeatable test run so changes can be measured instead of guessed.

- Creates a fresh world from a fixed seed and runs fixed stress scenes for walking, flight, caves, a large mob crowd, dense storage, particles, redstone, explosions and integrated-server chunk loading
- Uses render distance 32 unless `kinetic.benchmark.renderDistance` changes it, prepares each scene before measuring it and records the actual benchmark settings before restoring yours
- Reports hitches and motion error per scene as well as frame rate, frame time and server tick time
- Measures every scene for 30 seconds after 10 seconds of settling: frame rate, 1% and 0.1% lows, frame time deviation, median, p99 and p99.9 frame time, frames over 33 and 50 ms, server tick time distribution, garbage collector pauses, peak RAM and video memory
- Writes `kinetic/benchmarks/benchmark-<date>.json` and a readable `.txt` report into the game folder and compares format 2 reports with the previous run
- Start it with the JVM argument `-Dkinetic.benchmark=true`; add `-Dkinetic.benchmark.quit=true` to close the game when it is done
- Add `-Dkinetic.benchmark.screenshots=true` to save a screenshot at the end of every scene
- Every run gives Minecraft all the power: no frame limit and no vsync while it runs (your settings come back afterwards); `-Dkinetic.benchmark.renderDistance=N` and `-Dkinetic.benchmark.simulationDistance=N` set the distances for the run, and `-Dkinetic.modules=off` turns every Kinetic optimization off for that start, to measure vanilla without editing `kinetic-modules.properties`
- Since: 1.0

### Compatibility Guard
Kinetic never fights another renderer.

- Kinetic and Sodium are both performance mods for the same parts of the game, so Kinetic refuses to start next to Sodium and the loading screen says why, in English or Turkish after the game language; remove one of the two
- Only the built-in benchmark (`-Dkinetic.benchmark=true`) starts next to Sodium, with every Kinetic optimization off, so Sodium can be measured on the same route
- When Iris is installed, Kinetic keeps its optimizations off; the overlay and the benchmark still work
- Since: 1.0

### Faster Starts with Shader and Pipeline Caches
The second start of the game skips most of the shader work.

- Keeps the compiled form of every shader and every Vulkan pipeline on disk, so later starts and resource pack reloads reuse them instead of compiling them again
- The caches check the game version, the shader sources, resource packs, the GPU and the driver, and are rebuilt by themselves when any of them changes
- Stored in `kinetic/cache/` in the game folder; delete the folder to start fresh
- Since: 1.0

### No Shader Hitches in the Middle of a Game
Built-in pipelines are ready before they are needed.

- Vanilla compiles a few item pipelines only when the first such item is drawn, which causes a short hitch in game; Kinetic compiles them in the background while the game loads
- Since: 1.0

### Dynamic Frame Rate
Saves power and keeps your PC cool when you are not looking at the game.

- Lowers the frame rate to 30 FPS while the game window is in the background and to 60 FPS while a single player game is paused
- The world keeps running at full speed; only drawing slows down
- Turn it off or change both limits in the Kinetic settings
- Since: 1.0

### Lighter Chunk Memory
Uses less memory for loaded chunks.

- Replaces a heavy safety lock inside every chunk section with a lighter one that catches the same errors, which saved about 33 MB of memory in the benchmark at render distance 16 and server ticks were faster in the same run
- Since: 1.0

### Lighter Light Updates
Light changes no longer copy the whole light map of the world.

- After every batch of light updates the game copied the light data of all loaded sections so other threads could read it safely; Kinetic copies only the parts that changed and shares the rest, with exactly the same result
- With the default garbage collector this cut garbage collection time and memory peaks in the benchmark (heap peak 4.0 GB to 3.0 GB, 0.1% low 14.6 to 17.9 FPS)
- Since: 1.0

### Faster Chunk Drawing
Prepares the list of chunks to draw with much less work every frame.

- Remembers where each chunk's geometry sits on the GPU instead of searching for it again in every pass, which matters most with Helion's shadows; in the benchmark with Helion at render distance 32 the 1% low rose from 36 to 51 FPS and caves from 58 to 78 FPS
- Writes the draw commands straight into GPU memory instead of creating thousands of small objects every frame (`directDraws`); without Helion the benchmark average rose by 6 to 9 % and the 1% low by 10 to 16 %
- Checks which chunks need rebuilding from a compact list instead of looking up every visible chunk every frame (`dirtySections`); in caves, where thousands of chunks are visible but hidden behind stone, the average rose by 7 % and the lows by 11 to 14 %
- On graphics cards that support it, the graphics card itself builds the list of solid and leafy terrain to draw and skips faces turned away from you (`gpuCulling`); the main thread only names the chunks that occlusion culling kept, and the work on the card takes about 0.02 ms per frame; without Helion the lows rose by 4 to 5 %, with Helion it changes nothing; other cards keep the CPU path
- When you cross a chunk border, only the columns of chunks that move are updated instead of all of them, about 100,000 at render distance 32 (`sectionGridMoves`); together with lighter light snapshots the 1% low rose by 6 % and by up to 15 % in the elytra flight
- The main thread no longer waits for the chunk builders to free chunk memory (`lockFreeSections`): in vanilla it shares one lock with them for every frame's draw list, every chunk border crossing and every upload, and a builder that Windows paused while holding it froze the frame for up to 26 ms; now the main thread owns that memory and only the upload takes the lock, without waiting for it
- Since: 1.0

### Dedicated Chunk Builders
Chunks are prepared for drawing on Kinetic's own worker threads.

- In single player, vanilla prepares chunks on the same threads that generate and save the world, so new chunks wait behind world generation when you fly or run into new land; Kinetic gives chunk building its own threads (all cores but two, up to eight), one priority step below normal so they never hold back the graphics driver and the window compositor; this cut the long freezes underground and in the elytra flight by two thirds and lifted the 0.1% low from about 157 to 205 FPS over the whole benchmark, with chunks built just as fast
- The F3 screen shows chunk builds per second, how busy the workers are and how many builds are waiting; the benchmark log prints the same per scene
- The overlay and the benchmark log also show how long uploading new chunks to the graphics card takes and how long the game waits for the chunk builders
- Since: 1.0

### Chunks Unpacked in the Background
New chunks from the server are unpacked before the game needs them.

- When a chunk arrives, two background threads read its blocks and find where sky light starts in each column; the main thread then only places the finished chunk, instead of doing all of that between two frames (`chunkPacketDecoding`)
- The result is exactly the chunk vanilla builds: a built-in audit compared 1.4 million columns and found no difference
- In the elytra flight the 1% low rose by about 6 % and slow frames caused by the main thread fell by a third
- Since: 1.0

### Finished World on Entry
In single player the world is ready when the loading screen closes: no chunks popping in, no holes at the horizon.

- While the loading screen is up, the server generates and loads every chunk within the pregeneration radius around the spawn, and the game waits until those chunks have arrived, are lit and are built for drawing before it lets you in; vanilla prepares only 3 chunks around you and builds the rest while you already play
- Set it in the Kinetic settings (Mods, Kinetic, Config): Pregenerate on Join turns it on or off, Pregeneration Radius goes from 6 to 32 chunks (default 32, vanilla's largest render distance); the radius never goes beyond your render distance, so at render distance 12 the first 12 chunks are ready
- Chunk packets and light updates are worked off in small slices per frame while the game waits (at most 10 and 6 ms per frame), so the loading screen keeps moving, and its progress bar includes this terrain work
- Measured on Can's PC on an existing world at render distance 12: vanilla opened the world after 4.3 to 5.2 s with 365 to 432 chunks still missing, and the first 10 s in game had a 1% low of 35 to 41 FPS; with Kinetic the world opens after 8.2 to 9.1 s with no chunk missing and every visible section built, and the first 10 s run at 373 to 389 FPS with a 1% low of 139 to 165; at render distance 32 the world opens after about 18 to 21 s, also with no chunk missing
- Joining takes a few seconds longer, because the work vanilla does while you already play happens before you enter; if something stops the terrain from arriving, the game lets you in after 15 s without progress or 90 s in total
- Only for the owner of a single player or LAN world; servers and other players join as before (`spawnPregeneration`)
- Since: 1.0

### Smoother First Join
The first world you open after starting the game no longer stalls on loading code.

- On the title screen a low-priority background thread loads the 1,721 game classes the first world join needs, so the render thread no longer stops to load them in the middle of the loading screen (`joinWarmup`); the longest class loading stalls inside a join frame fell from 70 to 156 ms to 4 to 50 ms
- The compute shader of GPU-driven culling is compiled on a worker while the game loads instead of in the first frame of the world; until it is ready, the CPU draw list draws the terrain
- Since: 1.0

### Spare Swapchain Images
The game keeps two more finished-frame images ready for the screen.

- With vsync off, Windows sometimes holds on to an image for a few milliseconds before giving it back; vanilla has only three, so the next frame waited for it (`swapchainImages`)
- With two more images those short holds no longer stall the game: the 1% low underground rose by about 15 % and frames over 4 ms fell by a third, at the same average frame rate; what you see on screen is unchanged
- Since: 1.0

### Faster Villager Searches
Villagers find beds, job sites and bells with far less work.

- About every second, every villager without a bed or job searches 81 chunks of 24 sections each for one, and nearly all of those sections hold nothing; Kinetic keeps a per-chunk list of the sections that hold points of interest and visits only those
- The villagers find exactly the same places in the same order as in vanilla; a built-in audit compares both searches and CI runs it on every change
- Cloud measurement with 600 animals and villagers on 289 loaded chunks: server tick 21.7 ms to 18.1 ms
- Module `poiSearch`
- Since: 1.0

### Faster Mob Movement
Moving mobs stop searching for entities they could never bump into.

- Every step of every mob asks which nearby entities it would collide with, and vanilla checks every entity around it although only boats, shulkers and happy ghasts can be bumped into; Kinetic counts those per chunk section and skips the search where there are none, on both the server and your game client
- Same results as vanilla; a built-in audit checks every skipped search and CI runs it
- Cloud profile with 600 mobs: the entity collision search went from about 6 % to under 1 % of the server tick (2 to 5 % faster ticks)
- Module `entityCollisions`
- Since: 1.0

### Faster Particles in Open Air
Particles do not build a block collision list when there is nothing for them to hit.

- When a particle moves through loaded air, Kinetic checks the small area vanilla would search and keeps its movement unchanged without building the empty collision list; particles near blocks or unloaded chunks use vanilla's path
- In the stress particles scene, this removes nearly all of the particle-movement work that made up 40 % of slow frames
- Module `particleCollisions`
- Since: 1.0

### Faster Model Rendering
Entity and block entity models reuse each transformed cube corner.

- A normal cube has 24 vertices but only 8 corners. Kinetic transforms each corner once, then writes the same vertices, normals and picture as vanilla
- In the stress crowd and storage scenes, this removed work that took about 10 % of render-thread time in model rendering
- Module `modelCorners`
- Since: 1.0

### Faster Pushing in Teamless Worlds
Dense crowds skip team checks when the world's scoreboard has no teams.

- Pushing keeps vanilla's spectator, pushable and local-player rules, so behavior is unchanged
- This removes the hottest server-thread predicate in dense crowd profiles
- Module `teamlessPushing`
- Since: 1.0

### Faster Idle Hoppers
Hoppers waiting for items stop searching for entities that are not there.

- Every hopper with open space above or in front searches, each tick it is not busy, for dropped items and for chest minecarts or chest boats; Kinetic counts dropped items and item-holding entities per chunk section and skips the search where there are none
- Same results as vanilla, including NeoForge item capabilities on entities from other mods; a built-in audit checks every skippable search and CI runs it
- Cloud load with 300 mobs and 2000 idle hoppers: server tick CPU 14.5 ms to 12.9 ms (about 11 % faster, three runs each); no change where minecarts or items sit next to most hoppers
- Module `hopperSearch`
- Since: 1.0

### Parallel World Generation
Dedicated servers give new chunks their trees, ores and plants on several threads at once.

- Vanilla builds the terrain of new chunks in parallel but places every feature (trees, ores, flowers, structure pieces) one chunk after another on a single thread, which limits how fast new land appears while flying or exploring
- Kinetic places features of chunks that cannot touch each other at the same time; chunks whose feature areas overlap still wait for each other, just as vanilla lets a feature write only into the 3 x 3 chunks around its own
- Only on dedicated servers: in singleplayer the extra threads took processor time from drawing, and the elytra flight of the benchmark fell from 124 to 102 to 109 FPS (1% low 52 to 55 down to 38 to 41), so singleplayer keeps vanilla's order
- Measured on Can's PC with a dedicated server: 625 new chunks in 4.8 s instead of 6.3 s, 2401 chunks in 11.1 s instead of 15.3 s (27 % faster); the terrain is identical to vanilla; where the features of two neighbouring chunks overlap, which one lands first varies from run to run, as it already does in vanilla
- Module `parallelFeatures`
- Since: 1.0

### Faster Packet Compression
Servers compress what they send to players about twice as fast.

- On servers and LAN worlds, packets larger than the compression threshold are compressed with zlib level 4 instead of vanilla's level 6; players with or without Kinetic connect as before
- Measured on Can's PC with 1024 real chunks (45 MB): 834 ms to 399 ms (2.1 times faster) for 4.4 % more bytes; singleplayer worlds never compress, so nothing changes there
- Module `networkCompression`
- Since: 1.0

### Occlusion Culling
Skips terrain you cannot see because solid blocks are in the way.

- Caves, tunnels and rooms hidden behind stone or walls are not sent to the graphics card; the picture stays exactly the same, only frames get cheaper where the GPU was the limit
- With Helion, chunks the sun cannot see (buried under other terrain) are left out of the shadow maps; shadows look the same because the terrain above already casts them
- Mobs, dropped items, armor stands, chests, signs and other block entities behind walls are skipped the same way, and so are particles like smoke, flames and drips; with Helion, entities under open sky are still drawn so their sun shadows stay
- The visibility tests for the camera and for Helion's shadows run on their own background threads, so they no longer cost frame time on the main thread
- When a frame takes longer than usual and the camera moved past the last visibility result, that result is stretched to the new position instead of drawing every chunk for a few frames, so one slow frame no longer causes the next ones
- Stretching a result costs the main thread no extra visibility tests: the background thread already measured how far each hidden chunk stays hidden
- Where nothing is hidden, for example on flat land, culling rests and costs nothing; it checks again every second, so it starts working as soon as you enter a cave or walk behind a hill. The overlay shows when it rests
- In open scenes, particles and entities skip their visibility test when the terrain test hid nothing, so Kinetic spends no time asking whether they are behind a wall
- It can be turned off in the settings to compare; the F3 screen shows how many chunks, entities and particles were skipped
- Since: 1.0

### Face Culling
Sends only the block faces that can face you to the graphics card.

- Faces on the far side of every chunk are left out before they reach the graphics card (about a third to a half of all solid faces); the picture stays exactly the same
- Since: 1.0

### Compact Chunk Vertices
Stores terrain in a smaller format so the graphics card has less to read.

- Solid and cutout terrain uses 16 instead of 28 bytes per vertex: about a quarter to a third less video memory at render distance 32 and less work for every pass that draws terrain, including Helion's shadows; the picture stays the same (in Helion's visual test the terrain matched pixel for pixel)
- Measured on Can's PC with Helion: 5 % higher average frame rate, slightly higher 1% and 0.1% lows, 20 to 32 % less video memory
- On by default; stays off by itself with an older Helion build that cannot read the format, so terrain never breaks; turn it off with `compactVertices=false` in `config/kinetic-modules.properties`
- Since: 1.0

### Leaf Culling (Optional)
Draws fewer hidden leaf faces inside trees.

- With detailed leaves, vanilla also draws the faces between leaves deep inside a tree; with this setting on, faces two or more leaves deep are skipped and forests draw faster
- Off by default, because trees look slightly less dense through the gaps in the leaves; turn on `leafCulling` in the Kinetic settings, the chunks rebuild right away
- Since: 1.0

### Garbage Collector Advice
Tells you which Java settings give the smoothest frames.

- The log names the garbage collector and the memory limit at start and suggests `-XX:+UseZGC` when the game runs with a collector that pauses the game; a mod cannot change this itself, it goes into the launcher's JVM arguments
- Benchmark reports record the collector, because it changes the lows: in the benchmark ZGC raised the 1% low from 34 to 47 FPS and the 0.1% low from 12 to 32 FPS against the default G1
- Since: 1.0

### First Launch Hardware Setup
On its first launch Kinetic looks at the PC and lightens settings a weak machine cannot carry.

- Sorts the PC into low, medium or high from its CPU threads, Java memory limit, RAM, graphics card type (dedicated, integrated or software) and video memory; the weakest part decides
- Low: render distance at most 8, simulation distance at most 6, leaf culling on. Medium: render distance at most 12, simulation distance at most 8. High: nothing changes
- Only lowers, never raises, and only once; a toast says when something changed. `config/kinetic-hardware.properties` records the class and the changes; delete it to detect again
- Since: 1.0

### Crash Report Help
When the game crashes, the crash report says whether Kinetic was involved.

- Every crash report gets an "Kinetic" entry in its System Details: the version, which modules were on, and the Kinetic code that ran in the crashing stack, or "none"
- When a module's code was in the crash, the report names it on a fixed line (`Kinetic module in crash: occlusionCulling`) and says how to test without it
- The crash helper on the site reads these lines, so a known problem can point straight to its fix
- Since: 1.0

### Switchable Modules
Every optimization can be turned off.

- `config/kinetic-modules.properties` lists each optimization (`shaderCache`, `pipelineCache`, `pipelineWarmup`, `compactChunkLocks`, `sectionSliceCache`, `lightSnapshots`, `occlusionCulling`, `meshWorkers`, `faceCulling`, `poiSearch`, `entityCollisions`, `hopperSearch`, `compactVertices`, `networkCompression`, `parallelFeatures`, `blockEntitySections`, `directDraws`, `dirtySections`, `gpuCulling`, `sectionGridMoves`, `lockFreeSections`, `swapchainImages`, `chunkPacketDecoding`, `spawnPregeneration`, `joinWarmup`, `preciseFrameTime`, `particleCollisions`, `modelCorners`, `teamlessPushing`) with `true` or `false`; changes apply at the next start; every module starts on
- Render optimizations stay off by themselves when Iris is installed, or next to Sodium in the benchmark
- Since: 1.0

### Settings Screen with Arkea
When Arkea is installed, Kinetic's settings use the same organized screen style as Helion.

- The sidebar shows distant-terrain memory live: recorded chunks, tiles and MB; Configuration pages offer Performance, Balanced and Quality presets, overlay and frame-rate controls, distant terrain, rendering culling and world pregeneration
- The System Modules page lets you switch every optimization and saves it to `config/kinetic-modules.properties`; module changes apply after a restart
- Without Arkea installed, Kinetic keeps the normal NeoForge settings list
- Since: 1.0
