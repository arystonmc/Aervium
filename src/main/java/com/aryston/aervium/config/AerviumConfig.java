package com.aryston.aervium.config;

import com.aryston.aervium.core.budget.FrameBudget;
import com.aryston.aervium.core.budget.FrameRatePolicy;
import com.aryston.aervium.core.lod.DistantTerrainLimits;
import com.aryston.aervium.core.lod.GenerationPower;
import com.aryston.aervium.core.lod.LodQuality;
import com.aryston.aervium.world.spawn.SpawnPregenerationSettings;
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
        .define("overlayVisible", true);

    public static final ModConfigSpec.IntValue TARGET_FPS = BUILDER
        .defineInRange("targetFps", DEFAULT_TARGET_FPS, MIN_TARGET_FPS, MAX_TARGET_FPS);

    public static final ModConfigSpec.BooleanValue DYNAMIC_FPS = BUILDER
        .define("dynamicFps", true);

    public static final ModConfigSpec.IntValue UNFOCUSED_FPS = BUILDER
        .defineInRange("unfocusedFps", DEFAULT_UNFOCUSED_FPS, MIN_THROTTLED_FPS, MAX_THROTTLED_FPS);

    public static final ModConfigSpec.IntValue PAUSED_FPS = BUILDER
        .defineInRange("pausedFps", DEFAULT_PAUSED_FPS, MIN_THROTTLED_FPS, MAX_THROTTLED_FPS);

    public static final ModConfigSpec.BooleanValue OCCLUSION_CULLING = BUILDER
        .define("occlusionCulling", true);

    public static final ModConfigSpec.BooleanValue LEAF_CULLING = BUILDER
        .define("leafCulling", false);

    public static final ModConfigSpec.BooleanValue DISTANT_TERRAIN = BUILDER
        .define("distantTerrain", true);

    public static final ModConfigSpec.IntValue DISTANT_TERRAIN_DISTANCE = BUILDER
        .defineInRange("distantTerrainDistance", DistantTerrainLimits.DEFAULT_CHUNKS, DistantTerrainLimits.MIN_CHUNKS, DistantTerrainLimits.MAX_CHUNKS);

    public static final ModConfigSpec.EnumValue<LodQuality> DISTANT_TERRAIN_QUALITY = BUILDER
        .defineEnum("distantTerrainQuality", LodQuality.HIGH);

    public static final ModConfigSpec.EnumValue<GenerationPower> DISTANT_TERRAIN_POWER = BUILDER
        .defineEnum("distantTerrainPower", GenerationPower.BALANCED);

    public static final ModConfigSpec.BooleanValue PREGENERATE = BUILDER
        .define("pregenerate", true);

    public static final ModConfigSpec.IntValue PREGENERATION_RADIUS = BUILDER
        .defineInRange("pregenerationRadius", SpawnPregenerationSettings.DEFAULT_RADIUS, SpawnPregenerationSettings.MIN_RADIUS, SpawnPregenerationSettings.MAX_RADIUS);

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
