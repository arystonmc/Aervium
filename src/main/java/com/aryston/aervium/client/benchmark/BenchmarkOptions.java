package com.aryston.aervium.client.benchmark;

import com.aryston.aervium.Aervium;
import net.minecraft.client.Options;

final class BenchmarkOptions {
    private static final String RENDER_DISTANCE_PROPERTY = "aervium.benchmark.renderDistance";
    private static final String SIMULATION_DISTANCE_PROPERTY = "aervium.benchmark.simulationDistance";

    private int framerateLimit;
    private boolean vsync;
    private int renderDistance;
    private int simulationDistance;
    private boolean applied;

    void apply(Options options) {
        framerateLimit = options.framerateLimit().get();
        vsync = options.enableVsync().get();
        renderDistance = options.renderDistance().get();
        simulationDistance = options.simulationDistance().get();
        applied = true;
        options.framerateLimit().set(Options.UNLIMITED_FRAMERATE_CUTOFF);
        options.enableVsync().set(false);
        options.renderDistance().set(Integer.getInteger(RENDER_DISTANCE_PROPERTY, renderDistance));
        options.simulationDistance().set(Integer.getInteger(SIMULATION_DISTANCE_PROPERTY, simulationDistance));
        Aervium.LOGGER.info("Aervium benchmark: frame rate unlimited, vsync off, render distance {}, simulation distance {}",
            options.renderDistance().get(), options.simulationDistance().get());
    }

    void restore(Options options) {
        if (!applied) {
            return;
        }
        applied = false;
        options.framerateLimit().set(framerateLimit);
        options.enableVsync().set(vsync);
        options.renderDistance().set(renderDistance);
        options.simulationDistance().set(simulationDistance);
    }
}
