package com.aryston.aervium.core.budget;

public record FrameRatePolicy(boolean enabled, int unfocusedFps, int pausedFps) {
    public int limit(int vanillaLimit, WindowState state) {
        if (!enabled) {
            return vanillaLimit;
        }
        return switch (state) {
            case ACTIVE -> vanillaLimit;
            case PAUSED -> Math.min(vanillaLimit, pausedFps);
            case UNFOCUSED -> Math.min(vanillaLimit, unfocusedFps);
        };
    }

    public enum WindowState {
        ACTIVE,
        PAUSED,
        UNFOCUSED
    }
}
