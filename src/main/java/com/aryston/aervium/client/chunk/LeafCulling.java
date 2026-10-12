package com.aryston.aervium.client.chunk;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class LeafCulling {
    private static volatile boolean enabled;

    private LeafCulling() {
    }

    public static boolean enabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        if (enabled == value) {
            return;
        }
        enabled = value;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null) {
            minecraft.execute(LeafCulling::rebuildChunks);
        }
    }

    public static boolean hidden(BlockGetter level, BlockState state, Direction direction, BlockPos neighborPos) {
        if (!(state.getBlock() instanceof LeavesBlock)) {
            return false;
        }
        if (!(level.getBlockState(neighborPos).getBlock() instanceof LeavesBlock)) {
            return false;
        }
        return level.getBlockState(neighborPos.relative(direction)).getBlock() instanceof LeavesBlock;
    }

    private static void rebuildChunks() {
        LevelExtractor extractor = Minecraft.getInstance().levelExtractor;
        if (extractor != null) {
            extractor.allChanged();
        }
    }
}
