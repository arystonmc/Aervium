package com.aryston.kinetic.debug.benchmark;

import java.util.List;

public record BenchmarkEnvironment(
    String kineticVersion,
    String minecraftVersion,
    String javaVersion,
    String operatingSystem,
    int cpuThreads,
    double maxHeapMb,
    String garbageCollector,
    String gpu,
    String graphicsBackend,
    int windowWidth,
    int windowHeight,
    int renderDistance,
    int simulationDistance,
    int framerateLimit,
    boolean vsync,
    long seed,
    List<String> mods
) {
}
