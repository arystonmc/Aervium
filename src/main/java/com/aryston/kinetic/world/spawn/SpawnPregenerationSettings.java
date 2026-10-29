package com.aryston.kinetic.world.spawn;

public final class SpawnPregenerationSettings {
    public static final int MIN_RADIUS = 6;
    public static final int MAX_RADIUS = 32;
    public static final int DEFAULT_RADIUS = 32;
    public static final int VANILLA_RADIUS = 3;

    private static volatile boolean enabled;
    private static volatile int radius = DEFAULT_RADIUS;
    private static volatile int renderDistance = MAX_RADIUS;

    private SpawnPregenerationSettings() {
    }

    public static void apply(boolean pregenerate, int chunks) {
        radius = Math.clamp(chunks, MIN_RADIUS, MAX_RADIUS);
        enabled = pregenerate;
    }

    public static boolean enabled() {
        return enabled;
    }

    public static void useRenderDistance(int chunks) {
        renderDistance = chunks;
    }

    public static int joinRadius() {
        return Math.min(radius, renderDistance);
    }
}
