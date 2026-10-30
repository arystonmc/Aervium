# Kinetic Changelog

All notable changes to Kinetic. Each version section is ready to paste as an update note.

Format: sections `Added`, `Changed`, `Fixed`, `Removed`. One short plain English line per change.

## Unreleased

### Fixed
- With Helion's realistic water, distant water no longer shows as a bright turquoise band with a dotted edge; it matches the deep water near you
- No longer crashes on NeoForge 26.3.0.95 and newer
- Distant terrain and distant water draw again with newer Helion versions

### Added
- The F3 screen, the overlay and every benchmark scene show how long the processor and the graphics card work on each frame and whether the game is CPU bound or GPU bound

### Changed
- Distant terrain costs the render thread much less each frame: about 10 % of its time went into scanning a nearly empty list of finished chunks
- Renamed from Aervium to Kinetic: the mod id is now `kinetic`, settings live in `config/kinetic-client.toml` and the cache in `kinetic/`
- Needs NeoForge 26.3.0.95 or newer
- Distant terrain fills in about three times faster after joining a world or teleporting: with the Balanced CPU usage it runs like High until fewer than 32 tiles are missing, then slows down again
- The log reports distant terrain progress every 10 seconds and when the area within 512 blocks and the whole distance are generated

## [1.0] - 2026-10-21

### Added
- Distant terrain draws land far beyond the render distance, with normal Minecraft rendering and Helion; in single player, unvisited land is computed from the world seed in the background, coarse horizon first, without creating or saving chunks
- Distant terrain settings: distance from 64 to 512 chunks (default 256) with 64, 128, 256 and 512 chunk buttons, Low to Ultra quality and Low to Extreme CPU usage
- With Arkea installed, Kinetic has an organized settings screen with presets, live distant-terrain memory, configuration pages and restart-required module switches; without Arkea, the NeoForge settings list remains available
- Kinetic's performance details (frame rate and lows, server tick time, memory, culling, distant terrain and chunk builds) appear on the F3 debug screen
- Smooth camera zoom while holding `C`, adjusted with the mouse wheel
- High distant terrain quality now reaches full block detail near you, and Ultra adds block texture detail to distant terrain
- `-Dkinetic.benchmark.screenshots=true` saves a screenshot at the end of every benchmark scene, and a new overlook scene circles high above the spawn looking down
- Faster entity and block entity model rendering by transforming each cube corner once
- Faster pushing in dense teamless crowds by skipping unused scoreboard team checks
- Faster particles in open air by skipping empty block collision searches
- Faster client entity collision searches in explosions and other crowded scenes
- Open scenes now skip particle and entity occlusion raster tests when no terrain section is hidden
- Benchmark scenes now remove loose item and experience-orb drops after each site
- Smoother high-frame-rate motion with nanosecond partial-tick timing
- Stress benchmark scenes for crowds, storage, particles, redstone, explosions and integrated-server terrain loading
- Benchmark reports now show hitches and motion error for every scene
- Pregenerate on join: in single player the loading screen stays until every chunk within the pregeneration radius (6 to 32, default 32, never beyond the render distance) is generated, lit and built, so the world opens without missing chunks; at render distance 12 the first 10 s in game went from a 1% low of about 39 to about 150 FPS
- While the game waits for that terrain, chunk packets and light updates are spread over frames, so the loading screen keeps moving
- The classes the first world join needs are loaded on the title screen in the background, and the GPU culling shader compiles on a worker instead of in the first frame of the world
- Join check (`-Dkinetic.joinCheck=true`): opens a world, logs every loading phase with its slow frames, the missing chunks at entry and the first 10 s in game
- The benchmark runs with no frame limit and no vsync, can set render and simulation distance, can turn every optimization off for vanilla runs, and counts frames over 33 and 50 ms
- Kinetic refuses to start next to Sodium and explains why on the loading screen; only its benchmark still runs next to Sodium to compare the two
- Occlusion culling: terrain, mobs, chests, signs and particles hidden behind solid blocks are no longer drawn; the tests run on background threads and rest where they would hide little
- With Helion, chunks the sun cannot see are left out of the shadow maps; shadows look the same
- Graphics cards that support it build their own terrain draw list and skip block faces turned away from the camera; other cards keep a fast CPU path
- Faster chunk drawing: draw commands go straight into GPU memory, chunks that need rebuilding are found from a compact list, and crossing a chunk border updates only the columns that move
- The main thread no longer waits for the chunk builders to free chunk memory, which removed freezes of up to 26 ms in caves
- Chunk builders run one priority step below normal, so they no longer hold back the graphics driver: about two thirds fewer freezes of 15 to 25 ms underground and in fast flight, 0.1% low up by about 30 %
- New chunks are unpacked on background threads before the main thread places them, with exactly the same result (1% low in fast flight up by about 6 %)
- The game asks for two more swapchain images, so short presentation hiccups no longer stall the next frame (1% low underground up by about 15 %)
- Compact chunk vertices: terrain takes 16 instead of 28 bytes per vertex, for less video memory and faster drawing (off by itself with older Helion builds)
- Chunks are prepared on Kinetic's own threads instead of waiting behind world generation in single player
- Shader and pipeline caches make later starts faster, and built-in item pipelines compile while the game loads instead of causing a hitch in game
- Lighter chunk section locks and light updates that copy only the changed parts: less memory and fewer garbage collector pauses
- Villagers, moving mobs and idle hoppers search with far less server work, with the same results as vanilla
- Dedicated servers place trees, ores and plants of new chunks on several threads, so new land appears about a quarter faster
- Servers compress packets twice as fast for a few percent more network traffic
- Dynamic frame rate in the background and while paused
- Optional leaf culling for faster forests (off by default)
- On first launch Kinetic sorts the PC into low, medium or high and lightens settings on weak machines
- Performance details on the F3 screen with frame rate, 1% and 0.1% lows, server tick time, garbage collector pauses, memory use and chunk build statistics; their text is prepared only when the numbers change
- Built-in benchmark that flies a fixed route through a fixed-seed world and writes a JSON and a readable report
- The log suggests Java settings for smoother frames
- Crash reports name the Kinetic module whose code was in the crash and how to test without it
- Every optimization can be switched off in `config/kinetic-modules.properties`, and all render optimizations stay off when Iris is installed

### Changed
- Updated the Kinetic logo on the mod list and settings screen

### Fixed
- Distant terrain is no longer hidden by fog beyond 64 chunks; fog now stretches with the distant terrain distance
- Real terrain below the camera no longer disappears when you fly far above it
- Distant water no longer shows a grid of tile edges or blue patches over land
- Distant water reflects the sky by view angle and ripples gently instead of being one flat colour
- Distant terrain costs far less render-thread time: chunks not yet covered by real terrain are rechecked a few per frame instead of all every frame
- Distant terrain no longer reads saved chunks or runs hidden world generation, which could freeze the integrated server for seconds

### Removed
- The F6 culling and F7 overlay keys; occlusion culling is set in the settings and the performance details live on the F3 screen
