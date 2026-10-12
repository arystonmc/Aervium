# Aervium Changelog

All notable changes to Aervium. Each version section is ready to paste as an update note.

Format: sections `Added`, `Changed`, `Fixed`, `Removed`. One short plain English line per change.

## Unreleased

### Added
- Villagers find beds and job sites with far less server work (same results as vanilla)
- On first launch Aervium sorts the PC into low, medium or high and lightens settings on weak machines
- Crash reports name the Aervium module whose code was in the crash and how to test without it
- Initial mod setup for Minecraft 26.3 on NeoForge
- Performance overlay with frame rate, 1% and 0.1% lows, a frame time graph, server tick time, garbage collector pauses and memory use (F7)
- Built-in benchmark that flies a fixed route through a fixed-seed world and writes a JSON and a readable report
- Optimizations stay off when Sodium or Iris is installed
- Shader and pipeline caches that make later game starts faster
- Built-in item pipelines compile while the game loads instead of causing a hitch in game
- Dynamic frame rate in the background and while paused
- Lighter chunk section locks: about 33 MB less memory at render distance 16 and faster server ticks
- Light updates copy only the changed parts of the light map: less garbage and lower memory peaks
- Faster chunk draw lists: much higher lows with Helion's shadows and in caves
- The log suggests Java settings for smoother frames, and benchmark reports record the garbage collector
- Every optimization can be switched off in `config/aervium-modules.properties`
- Occlusion culling: terrain hidden behind solid blocks, like caves behind stone, is no longer drawn (F6 to compare)
- With Helion, chunks the sun cannot see are left out of the shadow maps; shadows look the same
- Mobs, items, chests, signs and other entities hidden behind solid blocks are no longer drawn
- Particles hidden behind solid blocks are no longer drawn
- Chunks are prepared on Aervium's own threads, no longer waiting behind world generation in single player
- Optional leaf culling for faster forests (off by default)
- The overlay and benchmark show chunk upload time and how long the game waits for the chunk builders
