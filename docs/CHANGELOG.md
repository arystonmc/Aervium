# Aervium Changelog

All notable changes to Aervium. Each version section is ready to paste as an update note.

Format: sections `Added`, `Changed`, `Fixed`, `Removed`. One short plain English line per change.

## Unreleased

### Added
- Initial mod setup for Minecraft 26.3 on NeoForge
- Performance overlay with frame rate, 1% and 0.1% lows, a frame time graph, server tick time, garbage collector pauses and memory use (F7)
- Built-in benchmark that flies a fixed route through a fixed-seed world and writes a JSON and a readable report
- Optimizations stay off when Sodium or Iris is installed
- Shader and pipeline caches that make later game starts faster
- Built-in item pipelines compile while the game loads instead of causing a hitch in game
- Dynamic frame rate in the background and while paused
- Lighter chunk section locks: about 33 MB less memory at render distance 16 and faster server ticks
- Every optimization can be switched off in `config/aervium-modules.properties`
