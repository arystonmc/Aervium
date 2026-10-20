package com.aryston.aervium.client.config;

import com.aryston.aervium.client.lod.DistantTerrain;
import com.aryston.aervium.core.telemetry.ByteSizes;
import com.aryston.arkea.api.config.ConfigMeter;
import net.minecraft.network.chat.Component;

final class DistantTerrainMeter {
    private static final String KEY = AerviumOptions.KEY + "meter";
    private static final double FULL_SCALE_MEGABYTES = 1024.0;

    private DistantTerrainMeter() {
    }

    static ConfigMeter create() {
        return new ConfigMeter(
            Component.translatable(KEY),
            values -> Math.clamp(ByteSizes.megabytes(DistantTerrain.statistics().bytes()) / FULL_SCALE_MEGABYTES, 0.0, 1.0),
            values -> Component.translatable(KEY + ".value", Math.round(ByteSizes.megabytes(DistantTerrain.statistics().bytes()))),
            Component.translatable(KEY + ".note")
        );
    }
}
