package com.aryston.aervium.client;

import com.aryston.aervium.Aervium;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;

final class AerviumKeys {
    private static final KeyMapping.Category CATEGORY = new KeyMapping.Category(Identifier.fromNamespaceAndPath(Aervium.MOD_ID, Aervium.MOD_ID));

    static final KeyMapping TOGGLE_OVERLAY = new KeyMapping(
        "key.aervium.overlay",
        KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYBOARD,
        InputConstants.KEY_F7,
        CATEGORY
    );

    private AerviumKeys() {
    }

    static void register(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(TOGGLE_OVERLAY);
    }
}
