# Aervium Changelog

All notable changes to Aervium. Each version section is ready to paste as an update note.

Format: sections `Added`, `Changed`, `Fixed`, `Removed`. One short plain English line per change.

## Unreleased

### Added
- Occlusion culling: terrain, mobs, chests, signs and particles hidden behind solid blocks are no longer drawn; the tests run on background threads (F6 to compare)
- With Helion, chunks the sun cannot see are left out of the shadow maps; shadows look the same
- Graphics cards that support it build their own terrain draw list and skip block faces turned away from the camera; other cards keep a fast CPU path
- Faster chunk drawing: draw commands go straight into GPU memory, chunks that need rebuilding are found from a compact list, and crossing a chunk border updates only the columns that move
- Compact chunk vertices: terrain takes 16 instead of 28 bytes per vertex, for less video memory and faster drawing (off by itself with Helion 0.1)
- Chunks are prepared on Aervium's own threads instead of waiting behind world generation in single player
- Shader and pipeline caches make later starts faster, and built-in item pipelines compile while the game loads instead of causing a hitch in game
- Lighter chunk section locks and light updates that copy only the changed parts: less memory and fewer garbage collector pauses
- Villagers, moving mobs and idle hoppers search with far less server work, with the same results as vanilla
- Dedicated servers place trees, ores and plants of new chunks on several threads, so new land appears about a quarter faster
- Servers compress packets twice as fast for a few percent more network traffic
- Dynamic frame rate in the background and while paused
- Optional leaf culling for faster forests (off by default)
- On first launch Aervium sorts the PC into low, medium or high and lightens settings on weak machines
- Performance overlay with frame rate, 1% and 0.1% lows, a frame time graph, server tick time, garbage collector pauses, memory use and chunk build statistics (F7); its last line names the keys that hide it and switch occlusion culling
- Built-in benchmark that flies a fixed route through a fixed-seed world and writes a JSON and a readable report
- The log suggests Java settings for smoother frames
- Crash reports name the Aervium module whose code was in the crash and how to test without it
- Every optimization can be switched off in `config/aervium-modules.properties`, and all render optimizations stay off when Sodium or Iris is installed
