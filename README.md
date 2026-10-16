<p align="center">
  <img src="branding/logo.png" alt="Aervium logo" width="160">
</p>

<h1 align="center">Aervium</h1>

<p align="center">
  <strong>Smooth, stutter-free Minecraft on the Vulkan renderer, with vanilla game behavior kept exactly.</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-26.3-5B8C5A?style=flat-square" alt="Minecraft 26.3">
  <img src="https://img.shields.io/badge/Loader-NeoForge-E87B35?style=flat-square" alt="NeoForge">
  <img src="https://img.shields.io/badge/Backend-Vulkan-ED2224?style=flat-square&logo=vulkan&logoColor=white" alt="Vulkan">
  <img src="https://img.shields.io/badge/License-Aryston%20Custom-3C8527?style=flat-square" alt="License">
</p>

---

## Overview

Aervium is the performance engine of the Aryston series. Its goal is not only a high frame rate but steady frame times: higher 1% and 0.1% lows, fewer stutters, and a server that keeps 20 ticks per second. Redstone, farms, mobs and world generation behave exactly as in vanilla; Aervium only changes how fast the game computes, never what it computes.

It replaces a stack of separate performance mods with built-in modules that can each be switched off, and it is the only optimizer that works together with [Helion](https://github.com/arystonmc/Helion).

## Features

- 🧱 **Occlusion culling:** Caves, rooms and terrain hidden behind solid blocks are no longer drawn, and neither are the mobs, chests and particles behind them. The tests run on background threads. F6 compares it in game.
- 🛰️ **GPU-driven terrain:** On graphics cards that support it, the card builds its own list of terrain to draw and skips block faces turned away from you. Other cards keep a fast CPU path.
- 📉 **Lighter frames:** Draw lists without per-frame garbage, a compact check for chunks that need rebuilding, smaller terrain vertices (20 to 32 % less video memory) and its own chunk build threads.
- ⚡ **No shader hitches:** Shader and pipeline caches make later starts faster and stop the stutter of shaders compiling mid-game.
- 🌍 **Faster worlds and servers:** Faster villager, mob collision and hopper searches with identical results, parallel world generation on dedicated servers, faster packet compression.
- 🔋 **Dynamic frame rate:** Lower frame rates in the background and while paused.
- 📈 **Performance overlay (F7):** Frame rate with 1% and 0.1% lows, a live frame time graph, server tick time, garbage collector pauses, RAM and video memory.
- 🧪 **Built-in benchmark:** A repeatable flight through a fixed world (walk, elytra, village crowd, caves, redstone) that writes a readable report and compares it with the last run.
- 🧩 **Plays well with others:** With Sodium or Iris installed, Aervium keeps every optimization off and only measures.

See the [Aervium Feature Guide](docs/FEATURES.md) for every module and its measured effect.

## Benchmark

The built-in benchmark on one PC (RTX 4050 Laptop GPU, 16 threads, 1920x1009 window, render distance 32), mean of two runs:

| Setup | Average FPS | 1% low | 0.1% low |
|---|---|---|---|
| Aervium | 326 | 130 | 71 |
| Sodium 0.9.3 | 296 | 124 | 67 |
| Vanilla | 218 | 65 | 43 |
| Aervium with Helion | 143 | 65 | 41 |
| Vanilla with Helion | 112 | 39 | 25 |

Run it yourself with the JVM property `-Daervium.benchmark=true`; reports land in `aervium/benchmarks/` in the game folder.

## Modules

Every optimization is a module listed in `config/aervium-modules.properties` with `true` or `false`. Changes apply at the next start. If something looks wrong, switch the module named in the crash report or the one you suspect off and try again.

## Requirements

| Requirement | Supported Version |
|---|---|
| **Minecraft** | 26.3 |
| **Loader** | NeoForge 26.3.0.51-beta or newer |
| **Java** | 25 |
| **Graphics API** | Vulkan (`Video Settings` → `Graphics API` → `Prefer Vulkan`) |
| **Side** | Client and server, each optional |

## Building

- **Java:** JDK 25. If it is not installed, Gradle downloads it automatically.
- **Build:** Run the Gradle wrapper from the project folder (use `gradlew.bat build` on Windows):

```bash
./gradlew build
```

- **Output:** The jar is written to `build/libs/aervium-neoforge-<minecraft_version>-<mod_version>.jar`, currently `build/libs/aervium-neoforge-26.3-0.1.jar`.

## Changelog

See [docs/CHANGELOG.md](docs/CHANGELOG.md).

## License

[Aryston Source-Available License](LICENSE.md)
