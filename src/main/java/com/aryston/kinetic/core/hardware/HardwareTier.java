package com.aryston.kinetic.core.hardware;

import java.util.Locale;

public enum HardwareTier {
    LOW(8, 6, true),
    MEDIUM(12, 8, false),
    HIGH(Integer.MAX_VALUE, Integer.MAX_VALUE, false);

    private final int renderDistanceCap;
    private final int simulationDistanceCap;
    private final boolean leafCulling;

    HardwareTier(int renderDistanceCap, int simulationDistanceCap, boolean leafCulling) {
        this.renderDistanceCap = renderDistanceCap;
        this.simulationDistanceCap = simulationDistanceCap;
        this.leafCulling = leafCulling;
    }

    public int renderDistance(int current) {
        return Math.min(current, renderDistanceCap);
    }

    public int simulationDistance(int current) {
        return Math.min(current, simulationDistanceCap);
    }

    public boolean leafCulling() {
        return leafCulling;
    }

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }
}
