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
  <img src="https://img.shields.io/badge/Status-In%20development-8A8A8A?style=flat-square" alt="In development">
</p>

---

## Overview

Aervium is the performance mod of the Aryston series. Its goal is not the highest peak frame rate but steady frame times: higher 1% and 0.1% lows, fewer stutters, and a server that always keeps 20 ticks per second. Redstone, farms and mobs behave exactly as in vanilla.

Aervium is in its first phase. This version measures; the optimizations follow in the next phases.

## Features

- 📈 **Performance overlay:** Frame rate, 1% and 0.1% lows, frame time deviation, a live frame time graph that shows every spike, server tick time, garbage collector pauses, RAM and VRAM. Press F7.
- 🧪 **Built-in benchmark:** A repeatable run through a fixed world (walking, elytra flight, a village crowd, caves, a redstone build) that writes a JSON and a readable report and compares it with the previous run.
- 🧩 **Plays well with others:** Aervium keeps its optimizations off when Sodium or Iris is installed.

See [docs/FEATURES.md](docs/FEATURES.md) for details.

## Requirements

| | |
|---|---|
| Minecraft | 26.3 |
| Loader | NeoForge 26.3.0.51-beta or newer |
| Java | 25 |
| Graphics | Vulkan |
| Side | Client and server, each optional |

## Changelog

See [docs/CHANGELOG.md](docs/CHANGELOG.md).

## Building

```
./gradlew build
```

The jar is written to `build/libs/`.

## License

[Aryston Source-Available License](LICENSE.md)
