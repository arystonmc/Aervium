# Kinetic Architecture: Porting Checklist for a New Minecraft Version

Part of the [Kinetic architecture](../ARCHITECTURE.md).

## Porting Checklist for a New Minecraft Version

1. Update `gradle.properties` and build; fix compile errors only in `integration/`, `client/`, `mixin/` and `config/`.
2. Check the mixin target of `FrontendGpuDeviceAccessor` and the VMA calls in `VulkanMemoryProbe`.
3. Check the gamerule names and command syntax used by `BenchmarkCommands` and the world creation in `BenchmarkWorld`.
4. Check the targets of the pregeneration mixins (`PrepareSpawnTask` and its `Preparing` state, `LevelLoadTracker`, `PacketProcessor`, `ClientLevel.pollLightUpdates`) and run the join check.
5. Regenerate `assets/kinetic/warmup/join_classes.txt`: record a join with JFR and the `jdk.ClassLoad` event enabled, list the classes loaded on the render thread (JFR names it `main`) between leaving the title screen and entering the world, leave out lambdas, hidden classes, `java.lang.invoke`, `jdk.internal` and the classes of other mods, and sort the list.
6. Run the unit tests, then a benchmark, and compare it with the last report of the previous version.
