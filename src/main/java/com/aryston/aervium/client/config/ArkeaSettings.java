package com.aryston.aervium.client.config;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

public final class ArkeaSettings {
    private ArkeaSettings() {
    }

    public static void register(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, (mod, parent) -> AerviumConfigScreen.create(parent));
    }
}
