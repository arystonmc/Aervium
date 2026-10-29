package com.aryston.kinetic.core.pregeneration;

public final class JoinProgress {
    public static final float SERVER_SHARE = 0.6F;

    private JoinProgress() {
    }

    public static float blend(float server, float terrain) {
        return SERVER_SHARE * clamp(server) + (1.0F - SERVER_SHARE) * clamp(terrain);
    }

    private static float clamp(float value) {
        return Float.isNaN(value) ? 0.0F : Math.clamp(value, 0.0F, 1.0F);
    }
}
