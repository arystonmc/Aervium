package com.aryston.kinetic.client.benchmark;

import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelStorageSource;

final class FreshWorld {
    private FreshWorld() {
    }

    static void create(Minecraft minecraft, Screen parent, String folder, LevelSettings settings, long seed) throws IOException {
        LevelStorageSource levels = minecraft.getLevelSource();
        if (levels.levelExists(folder)) {
            try (LevelStorageSource.LevelStorageAccess access = levels.createAccess(folder)) {
                access.deleteLevel();
            }
        }
        minecraft.createWorldOpenFlows().createFreshLevel(folder, settings, new WorldOptions(seed, true, false), WorldPresets::createNormalWorldDimensions, parent);
    }
}
