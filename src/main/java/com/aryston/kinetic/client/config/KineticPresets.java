package com.aryston.kinetic.client.config;

import com.aryston.arkea.api.config.ConfigPreset;
import com.aryston.arkea.ui.render.Icon;
import com.aryston.arkea.ui.render.Icons;
import java.util.List;
import net.minecraft.network.chat.Component;

final class KineticPresets {
    private static final String KEY = KineticOptions.KEY + "preset.";
    private static final Level PERFORMANCE = new Level("performance", Icons.BOLT, 128, 12, true);
    private static final Level BALANCED = new Level("balanced", Icons.SLIDERS, 256, 16, true);
    private static final Level QUALITY = new Level("quality", Icons.SPARKLE, 512, 32, false);
    private static final List<Integer> DISTANCES = List.of(64, 128, 256, 512);

    private KineticPresets() {
    }

    static List<ConfigPreset> create(KineticOptions options) {
        return List.of(preset(PERFORMANCE, options), preset(BALANCED, options), preset(QUALITY, options));
    }

    static List<ConfigPreset> distances(KineticOptions options) {
        return DISTANCES.stream()
            .map(chunks -> new ConfigPreset(Component.translatable(KEY + "distance", chunks), Component.translatable(KEY + "distance.description", chunks), Icons.CHUNKS)
                .set(options.distantTerrain, true)
                .set(options.distantTerrainDistance, chunks))
            .toList();
    }

    private static ConfigPreset preset(Level level, KineticOptions options) {
        return new ConfigPreset(Component.translatable(KEY + level.id()), Component.translatable(KEY + level.id() + ".description"), level.icon())
            .set(options.distantTerrain, true)
            .set(options.distantTerrainDistance, level.distanceChunks())
            .set(options.occlusionCulling, true)
            .set(options.leafCulling, level.leafCulling())
            .set(options.pregenerate, true)
            .set(options.pregenerationRadius, level.pregenerationRadius());
    }

    private record Level(String id, Icon icon, int distanceChunks, int pregenerationRadius, boolean leafCulling) {
    }
}
