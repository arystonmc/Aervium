package com.aryston.kinetic.client;

import com.aryston.kinetic.Kinetic;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;

final class KineticKeys {
    private static final KeyMapping.Category CATEGORY = new KeyMapping.Category(Identifier.fromNamespaceAndPath(Kinetic.MOD_ID, Kinetic.MOD_ID));

    static final KeyMapping ZOOM = new KeyMapping(
        "key.kinetic.zoom",
        KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYBOARD,
        InputConstants.KEY_C,
        CATEGORY
    );

    private KineticKeys() {
    }

    static void register(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(ZOOM);
    }
}
