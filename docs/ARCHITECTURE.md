# Kinetic Architecture

How Kinetic is built, which rules every optimization must keep and in which order the engine grows. Read this before changing anything, and before starting a new phase.

The sections live in `docs/architecture/`, one file per topic. Read the invariants below, then open only the section your task needs.

## Invariants

1. No engine class imports Minecraft or NeoForge; the adapter layer is the only place that does.
2. Nothing allocates per frame in recording or drawing code; copies and summaries happen at most every five ticks.
3. Every job result reaches Minecraft state only through `MainThreadQueue` on the main thread.
4. The benchmark restores every option it changes (pause on lost focus) and only deletes its own `kinetic-benchmark` world.
5. Every optimization is a module: mixins of a switched-off module are never applied (`MixinModules`), and runtime features read their switch from the config. A module that changes what the player sees or is not yet measured on Can's PC lands on a branch until it is measured; every module on `main` starts on.
6. Caches are content addressed: anything that changes the result (game version, sources, includes, defines, GPU, driver) changes the key or invalidates the data, so a stale cache can never be used.

## Sections

| Section | File | Covers |
|---|---|---|
| Goal | [goal.md](architecture/goal.md) | Kinetic is the performance engine of the Aryston series for Minecraft 26.3 on NeoForge with the Vulkan backend. |
| Rules | [rules.md](architecture/rules.md) | These rules decide every design question. |
| Layers | [layers.md](architecture/layers.md) | Engine code never imports `net.minecraft` or `net.neoforged` classes (Gson and SLF4J are allowed). |
| Configuration and Modules | [configuration-and-modules.md](architecture/configuration-and-modules.md) | Kinetic otherwise depends on no other mod, but its optional client-side Arkea dependency provides the enhanced settings screen when ... |
| Frame and Tick Flow | [frame-and-tick-flow.md](architecture/frame-and-tick-flow.md) | Raw data is copied on the thread that writes it, statistics are computed on a worker (`TELEMETRY` scope), and results come back through ... |
| Engine Systems | [engine-systems--core-and-culling.md](architecture/engine-systems--core-and-culling.md) | Telemetry, Frame Budget, Job System, Shader and Pipeline Caches, Occlusion Culling, Face Culling, Terrain Draw Paths |
| Engine Systems | [engine-systems--terrain-and-join.md](architecture/engine-systems--terrain-and-join.md) | Distant Terrain, Compact Terrain Vertices, Parallel Feature Generation, Packet Compression, Pregeneration on Join, Join Warm-up, Modules |
| Benchmark | [benchmark.md](architecture/benchmark.md) | `-Dkinetic.benchmark=true` (works with `launcher/launch.ps1 -Define`) starts the benchmark from the first screen; ... |
| Join Check | [join-check.md](architecture/join-check.md) | `-Dkinetic.joinCheck=true` measures a world join: it opens the save folder named by `kinetic.joinCheck.world`, creates a fresh world on ... |
| Vanilla Parity Check | [vanilla-parity-check.md](architecture/vanilla-parity-check.md) | `./gradlew runParity` starts a dedicated server in `runs/parity` with a fresh flat world, runs a fixed scenario and stops; ... |
| Mixins | [mixins.md](architecture/mixins.md) | Every mixin with its target and why no API is enough. |
| Phases | [phases.md](architecture/phases.md) | Phase table with content and state of each phase. |
| Porting Checklist for a New Minecraft Version | [porting-checklist.md](architecture/porting-checklist.md) | Update `gradle.properties` and build; fix compile errors only in `integration/`, `client/`, `mixin/` and `config/`. |
| Measuring | [measuring.md](architecture/measuring.md) | Before and after an optimization: |
| Distant terrain review, 2026-10-19 (historical) | [lod-review--research.md](architecture/lod-review--research.md) | License boundary and evidence, Voxy, Distant Horizons, Kinetic gap analysis, Ranked implementation plan |
| Distant terrain review follow-up, 2026-10-20 (historical) | [lod-review--verification.md](architecture/lod-review--verification.md) | Video regression repair (2026-10-20), Work left undone, Files changed in this task |
