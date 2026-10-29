package com.aryston.kinetic.integration;

import java.util.List;
import net.neoforged.fml.ModList;

public final class CompatibilityGuard {
    static final List<String> REPLACED_RENDERERS = List.of("sodium", "iris");

    private CompatibilityGuard() {
    }

    public static List<String> loadedRendererMods() {
        return REPLACED_RENDERERS.stream()
            .filter(ModList.get()::isLoaded)
            .toList();
    }
}
