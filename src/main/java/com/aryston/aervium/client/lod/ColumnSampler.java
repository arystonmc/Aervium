package com.aryston.aervium.client.lod;

import net.minecraft.client.color.block.BlockColors;
import net.minecraft.world.level.chunk.LevelChunk;

final class ColumnSampler {
    private final BlockColors colors;

    ColumnSampler(BlockColors colors) {
        this.colors = colors;
    }

    SavedColumnView view(LevelChunk chunk) {
        return new SavedColumnView(chunk, colors, 0, chunk.getLevel().dimensionType().hasSkyLight());
    }
}
