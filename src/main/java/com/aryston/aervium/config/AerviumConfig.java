package com.aryston.aervium.config;

import com.aryston.aervium.core.budget.FrameBudget;
import com.aryston.aervium.core.budget.FrameRatePolicy;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class AerviumConfig {
    private static final int DEFAULT_TARGET_FPS = 60;
    private static final int MIN_TARGET_FPS = 20;
    private static final int MAX_TARGET_FPS = 1000;
    private static final int DEFAULT_UNFOCUSED_FPS = 30;
    private static final int DEFAULT_PAUSED_FPS = 60;
    private static final int MIN_THROTTLED_FPS = 1;
    private static final int MAX_THROTTLED_FPS = 240;

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue OVERLAY_VISIBLE = BUILDER
        .define("overlayVisible", false);

    public static final ModConfigSpec.IntValue TARGET_FPS = BUILDER
        .defineInRange("targetFps", DEFAULT_TARGET_FPS, MIN_TARGET_FPS, MAX_TARGET_FPS);

    public static final ModConfigSpec.BooleanValue DYNAMIC_FPS = BUILDER
        .define("dynamicFps", true);

    public static final ModConfigSpec.IntValue UNFOCUSED_FPS = BUILDER
        .defineInRange("unfocusedFps", DEFAULT_UNFOCUSED_FPS, MIN_THROTTLED_FPS, MAX_THROTTLED_FPS);

    public static final ModConfigSpec.IntValue PAUSED_FPS = BUILDER
        .defineInRange("pausedFps", DEFAULT_PAUSED_FPS, MIN_THROTTLED_FPS, MAX_THROTTLED_FPS);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private AerviumConfig() {
    }

    public static FrameBudget defaultFrameBudget() {
        return new FrameBudget(DEFAULT_TARGET_FPS);
    }

    public static FrameBudget frameBudget() {
        return new FrameBudget(TARGET_FPS.getAsInt());
    }

    public static FrameRatePolicy frameRatePolicy() {
        return new FrameRatePolicy(DYNAMIC_FPS.getAsBoolean(), UNFOCUSED_FPS.getAsInt(), PAUSED_FPS.getAsInt());
    }
}
