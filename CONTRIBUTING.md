# Contributing to Aervium

Thank you for your interest in contributing to Aervium.

## Building from Source

### Prerequisites
- Java 25 JDK
- Vulkan-compatible graphics hardware and drivers

### Build Command

```bash
./gradlew build
```

The build also runs the unit tests in `src/test/java/`. The finished jar is written to `build/libs/`.

## Running the Client in Development

```bash
./gradlew runClient
```

The game client automatically runs with the `--graphicsBackend vulkan` argument. Add `-Daervium.benchmark=true` to the JVM arguments to run the built-in benchmark.

### Troubleshooting Development Starts
If the client closes immediately upon startup, open `run/config/fml.toml` and verify `earlyWindowControl = false`. The NeoForge loading screen uses OpenGL, and handoff to Vulkan can fail on certain display configurations.

## Architecture and Guidelines
- Read [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for the layers, the performance rules and the phase plan before proposing changes.
- Read [docs/CODEMAP.md](docs/CODEMAP.md) for a map of every class. Update this document whenever adding, modifying, or removing files.
- Every change must keep vanilla game behavior identical: redstone, farms and mob behavior work exactly as without Aervium.
- Never copy code from other performance mods. Techniques are implemented from public descriptions only.
- Code style: clean, intention-revealing, no dead code, and no comments in code files.

## Pull Requests and License Agreement
All contributions submitted via Pull Requests are subject to the [Aryston Source-Available License](LICENSE.md). By submitting a pull request, you agree that your contribution may be licensed and distributed under these terms.
