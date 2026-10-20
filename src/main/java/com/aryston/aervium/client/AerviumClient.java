package com.aryston.aervium.client;

import com.aryston.aervium.Aervium;
import com.aryston.aervium.client.config.ArkeaSettings;
import com.aryston.aervium.config.AerviumConfig;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = Aervium.MOD_ID, dist = Dist.CLIENT)
public final class AerviumClient {
    private static final String ARKEA_MOD_ID = "arkea";

    public AerviumClient(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, AerviumConfig.SPEC);
        if (ModList.get().isLoaded(ARKEA_MOD_ID)) {
            ArkeaSettings.register(container);
        } else {
            container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        }
        ClientEvents.register(modBus);
    }
}
