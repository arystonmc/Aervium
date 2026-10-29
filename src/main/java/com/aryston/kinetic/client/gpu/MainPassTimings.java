package com.aryston.kinetic.client.gpu;

import com.mojang.renderpearl.api.commands.RenderPass;

public final class MainPassTimings {
    public static final String OPAQUE_TERRAIN = "main: opaque terrain";
    public static final String SOLID_FEATURES = "main: solid entities and block entities";
    public static final String TRANSLUCENT_FEATURES = "main: translucent entities";
    public static final String TRANSLUCENT_TERRAIN = "main: translucent terrain";
    public static final String PARTICLES = "main: particles";
    public static final String CLOUDS_WEATHER_BORDER = "main: clouds, weather, border";
    public static final String IMPROVED_TRANSPARENCY = "main: improved transparency";
    public static final String IMPROVED_TRANSPARENCY_TERRAIN = "main: improved transparency terrain";

    private static int segment = GpuStageTimer.NOT_MEASURED;
    private static int transparency = GpuStageTimer.NOT_MEASURED;
    private static int transparencyTerrain = GpuStageTimer.NOT_MEASURED;

    private MainPassTimings() {
    }

    public static void next(String stage, RenderPass pass) {
        if (!GpuStageTimer.enabled()) {
            return;
        }
        GpuStageTimer.end(segment, pass);
        segment = GpuStageTimer.begin(GpuStageTimer.stage(stage), pass);
    }

    public static void finish(RenderPass pass) {
        GpuStageTimer.end(segment, pass);
        segment = GpuStageTimer.NOT_MEASURED;
    }

    public static void transparencyStarted() {
        transparency = begin(IMPROVED_TRANSPARENCY);
    }

    public static void transparencyFinished() {
        GpuStageTimer.end(transparency, null);
        transparency = GpuStageTimer.NOT_MEASURED;
    }

    public static void transparencyTerrainStarted() {
        transparencyTerrain = begin(IMPROVED_TRANSPARENCY_TERRAIN);
    }

    public static void transparencyTerrainFinished() {
        GpuStageTimer.end(transparencyTerrain, null);
        transparencyTerrain = GpuStageTimer.NOT_MEASURED;
    }

    private static int begin(String stage) {
        return GpuStageTimer.enabled() ? GpuStageTimer.begin(GpuStageTimer.stage(stage), null) : GpuStageTimer.NOT_MEASURED;
    }
}
