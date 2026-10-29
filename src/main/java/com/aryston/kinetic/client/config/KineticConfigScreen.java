package com.aryston.kinetic.client.config;

import com.aryston.kinetic.Kinetic;
import com.aryston.arkea.api.config.ArkeaConfigScreen;
import com.aryston.arkea.api.config.ConfigPreset;
import com.aryston.arkea.ui.render.Icons;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class KineticConfigScreen {
    private static final Identifier LOGO = Identifier.fromNamespaceAndPath(Kinetic.MOD_ID, "textures/gui/logo.png");

    private KineticConfigScreen() {
    }

    public static Screen create(Screen parent) {
        KineticOptions options = new KineticOptions();
        return ArkeaConfigScreen.builder(text("title"))
            .brand(LOGO, text("brand"), text("brand.subtitle"))
            .meter(DistantTerrainMeter.create())
            .group(text("group.configuration"))
            .page("overview", text("page.overview"), Icons.SLIDERS, page -> {
                for (ConfigPreset preset : KineticPresets.create(options)) {
                    page.preset(preset);
                }
                page.fullSection(text("section.overlay"), section -> section.add(options.overlay));
                page.fullSection(text("section.frameRate"), section -> section.add(options.targetFps).add(options.dynamicFps)
                    .add(options.unfocusedFps).add(options.pausedFps));
            })
            .page("distantTerrain", text("page.distantTerrain"), Icons.GLOBE, page -> {
                for (ConfigPreset preset : KineticPresets.distances(options)) {
                    page.preset(preset);
                }
                page.notice(text("distantTerrain.notice"));
                page.fullSection(text("section.distantTerrain"), section -> section.add(options.distantTerrain).add(options.distantTerrainDistance)
                    .add(options.distantTerrainQuality).add(options.distantTerrainPower));
            })
            .page("rendering", text("page.rendering"), Icons.MONITOR, page -> page
                .fullSection(text("section.culling"), section -> section.add(options.occlusionCulling).add(options.leafCulling)))
            .page("world", text("page.world"), Icons.WORLD, page -> page
                .fullSection(text("section.pregeneration"), section -> section.add(options.pregenerate).add(options.pregenerationRadius)))
            .group(text("group.system"))
            .page("modules", text("page.modules"), Icons.LAYERS, page -> page
                .notice(text("modules.notice"))
                .fullSection(text("section.modules"), section -> options.modules.forEach(section::add)))
            .build(parent);
    }

    private static Component text(String key) {
        return Component.translatable(KineticOptions.KEY + key);
    }
}
