package com.aryston.aervium.client.config;

import com.aryston.aervium.config.AerviumConfig;
import com.aryston.aervium.core.lod.DistantTerrainLimits;
import com.aryston.aervium.core.lod.GenerationPower;
import com.aryston.aervium.core.lod.LodQuality;
import com.aryston.aervium.core.module.OptimizationModule;
import com.aryston.aervium.world.spawn.SpawnPregenerationSettings;
import com.aryston.arkea.api.config.Binding;
import com.aryston.arkea.api.config.ChoiceStyle;
import com.aryston.arkea.api.config.ConfigOption;
import com.aryston.arkea.ui.render.Icon;
import com.aryston.arkea.ui.render.Icons;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.ModConfigSpec;

final class AerviumOptions {
    static final String KEY = "aervium.config.";

    private static final String TOOLTIP_KEY = "aervium.configuration.";
    private static final String MODULE_KEY = KEY + "module.";
    private static final int TARGET_FPS_MIN = 20;
    private static final int TARGET_FPS_MAX = 1000;
    private static final int TARGET_FPS_STEP = 10;
    private static final int THROTTLED_FPS_MIN = 1;
    private static final int THROTTLED_FPS_MAX = 240;
    private static final int THROTTLED_FPS_STEP = 1;
    private static final int DISTANCE_STEP = 16;
    private static final int RADIUS_STEP = 1;

    final ConfigOption<Boolean> overlay = toggle("overlayVisible", AerviumConfig.OVERLAY_VISIBLE, Icons.GAUGE);
    final ConfigOption<Integer> targetFps = slider("targetFps", AerviumConfig.TARGET_FPS, TARGET_FPS_MIN, TARGET_FPS_MAX, TARGET_FPS_STEP, Icons.SPEED);
    final ConfigOption<Boolean> dynamicFps = toggle("dynamicFps", AerviumConfig.DYNAMIC_FPS, Icons.VSYNC);
    final ConfigOption<Integer> unfocusedFps = slider("unfocusedFps", AerviumConfig.UNFOCUSED_FPS, THROTTLED_FPS_MIN, THROTTLED_FPS_MAX, THROTTLED_FPS_STEP, Icons.EYE)
        .requires(dynamicFps);
    final ConfigOption<Integer> pausedFps = slider("pausedFps", AerviumConfig.PAUSED_FPS, THROTTLED_FPS_MIN, THROTTLED_FPS_MAX, THROTTLED_FPS_STEP, Icons.CLOCK)
        .requires(dynamicFps);
    final ConfigOption<Boolean> distantTerrain = toggle("distantTerrain", AerviumConfig.DISTANT_TERRAIN, Icons.GLOBE);
    final ConfigOption<Integer> distantTerrainDistance = slider("distantTerrainDistance", AerviumConfig.DISTANT_TERRAIN_DISTANCE,
        DistantTerrainLimits.MIN_CHUNKS, DistantTerrainLimits.MAX_CHUNKS, DISTANCE_STEP, Icons.CHUNKS)
        .requires(distantTerrain);
    final ConfigOption<LodQuality> distantTerrainQuality = choice("distantTerrainQuality", AerviumConfig.DISTANT_TERRAIN_QUALITY, LodQuality.values(), Icons.SPARKLE)
        .requires(distantTerrain);
    final ConfigOption<GenerationPower> distantTerrainPower = choice("distantTerrainPower", AerviumConfig.DISTANT_TERRAIN_POWER, GenerationPower.values(), Icons.BOLT)
        .requires(distantTerrain);
    final ConfigOption<Boolean> occlusionCulling = toggle("occlusionCulling", AerviumConfig.OCCLUSION_CULLING, Icons.CUBE);
    final ConfigOption<Boolean> leafCulling = toggle("leafCulling", AerviumConfig.LEAF_CULLING, Icons.LEAF);
    final ConfigOption<Boolean> pregenerate = toggle("pregenerate", AerviumConfig.PREGENERATE, Icons.WORLD);
    final ConfigOption<Integer> pregenerationRadius = slider("pregenerationRadius", AerviumConfig.PREGENERATION_RADIUS,
        SpawnPregenerationSettings.MIN_RADIUS, SpawnPregenerationSettings.MAX_RADIUS, RADIUS_STEP, Icons.COMPASS)
        .requires(pregenerate);
    final List<ConfigOption<Boolean>> modules = modules();

    private static List<ConfigOption<Boolean>> modules() {
        ModuleFile file = new ModuleFile();
        List<ConfigOption<Boolean>> options = new ArrayList<>();
        for (OptimizationModule module : OptimizationModule.values()) {
            options.add(ConfigOption.toggle(MODULE_KEY + module.key(), file.binding(module))
                .icon(module.renderer() ? Icons.MONITOR : Icons.SERVER)
                .description(Component.translatable(MODULE_KEY + module.key() + ".description"))
                .requiresRestart());
        }
        return List.copyOf(options);
    }

    private static ConfigOption<Boolean> toggle(String id, ModConfigSpec.BooleanValue value, Icon icon) {
        return describe(ConfigOption.toggle(KEY + id, Binding.of(value)), id, icon);
    }

    private static <E extends Enum<E>> ConfigOption<E> choice(String id, ModConfigSpec.EnumValue<E> value, E[] values, Icon icon) {
        return describe(ConfigOption.choice(KEY + id, Binding.of(value), List.of(values),
            entry -> Component.translatable(KEY + id + "." + entry.name().toLowerCase(Locale.ROOT))).style(ChoiceStyle.SEGMENTED), id, icon);
    }

    private static ConfigOption<Integer> slider(String id, ModConfigSpec.IntValue value, int min, int max, int step, Icon icon) {
        return describe(ConfigOption.slider(KEY + id, Binding.of(value), min, max, step), id, icon);
    }

    private static <T> ConfigOption<T> describe(ConfigOption<T> option, String id, Icon icon) {
        return option.icon(icon)
            .description(Component.translatable(KEY + id + ".description"))
            .tooltip(Component.translatable(TOOLTIP_KEY + id + ".tooltip"));
    }
}
