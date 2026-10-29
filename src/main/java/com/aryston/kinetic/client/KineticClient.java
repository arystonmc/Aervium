package com.aryston.kinetic.client;

import com.aryston.kinetic.Kinetic;
import com.aryston.kinetic.client.config.ArkeaSettings;
import com.aryston.kinetic.config.KineticConfig;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = Kinetic.MOD_ID, dist = Dist.CLIENT)
public final class KineticClient {
    private static final String ARKEA_MOD_ID = "arkea";

    public KineticClient(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, KineticConfig.SPEC);
        if (ModList.get().isLoaded(ARKEA_MOD_ID)) {
            ArkeaSettings.register(container);
        } else {
            container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        }
        ClientEvents.register(modBus);
    }
}
