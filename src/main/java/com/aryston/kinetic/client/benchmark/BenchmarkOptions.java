package com.aryston.kinetic.client.benchmark;

import com.aryston.kinetic.Kinetic;
import net.minecraft.client.Options;

final class BenchmarkOptions {
    private static final String RENDER_DISTANCE_PROPERTY = "kinetic.benchmark.renderDistance";
    private static final String SIMULATION_DISTANCE_PROPERTY = "kinetic.benchmark.simulationDistance";
    private static final int STRESS_RENDER_DISTANCE = 32;

    private int framerateLimit;
    private boolean vsync;
    private boolean fullscreen;
    private boolean exclusiveFullscreen;
    private int renderDistance;
    private int simulationDistance;
    private boolean applied;

    void apply(Options options) {
        framerateLimit = options.framerateLimit().get();
        vsync = options.enableVsync().get();
        fullscreen = options.fullscreen().get();
        exclusiveFullscreen = options.exclusiveFullscreen().get();
        renderDistance = options.renderDistance().get();
        simulationDistance = options.simulationDistance().get();
        applied = true;
        options.framerateLimit().set(Options.UNLIMITED_FRAMERATE_CUTOFF);
        options.enableVsync().set(false);
        options.exclusiveFullscreen().set(false);
        options.fullscreen().set(true);
        options.renderDistance().set(Integer.getInteger(RENDER_DISTANCE_PROPERTY, STRESS_RENDER_DISTANCE));
        options.simulationDistance().set(Integer.getInteger(SIMULATION_DISTANCE_PROPERTY, simulationDistance));
        Kinetic.LOGGER.info("Kinetic benchmark: borderless fullscreen, frame rate unlimited, vsync off, render distance {}, simulation distance {}",
            options.renderDistance().get(), options.simulationDistance().get());
    }

    void restore(Options options) {
        if (!applied) {
            return;
        }
        applied = false;
        options.framerateLimit().set(framerateLimit);
        options.enableVsync().set(vsync);
        options.fullscreen().set(fullscreen);
        options.exclusiveFullscreen().set(exclusiveFullscreen);
        options.renderDistance().set(renderDistance);
        options.simulationDistance().set(simulationDistance);
    }
}
