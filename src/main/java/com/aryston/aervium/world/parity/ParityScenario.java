package com.aryston.aervium.world.parity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

final class ParityScenario {
    static final int GROUND_Y = -61;
    static final int MIN_X = -24;
    static final int MAX_X = 23;
    static final int MIN_Z = -24;
    static final int MAX_Z = 23;
    static final int MIN_Y = -64;
    static final int MAX_Y = -44;

    private static final int FARM_SIZE = 9;
    private static final int CHEST_SLOTS = 27;
    private static final int FUEL_COUNT = 16;
    private static final int ORE_COUNT = 32;

    private ParityScenario() {
    }

    static void build(ServerLevel level) {
        redstoneClock(level, -20, -20);
        hopperChain(level, -8, -20);
        furnace(level, 4, -20);
        farm(level, -20, -6);
        fluids(level, 4, -6);
        plants(level, -20, 8);
        torches(level, 8, 8);
    }

    private static void redstoneClock(ServerLevel level, int x, int z) {
        int y = GROUND_Y + 1;
        set(level, x + 1, y, z, facing(Blocks.OBSERVER, Direction.EAST));
        set(level, x + 2, y, z, facing(Blocks.OBSERVER, Direction.WEST));
        for (int i = 0; i < 4; i++) {
            set(level, x - i, y, z, Blocks.REDSTONE_WIRE.defaultBlockState());
        }
        set(level, x - 4, y, z, Blocks.REDSTONE_LAMP.defaultBlockState());
        for (int i = 0; i < 4; i++) {
            set(level, x + 3 + i, y, z, Blocks.REDSTONE_WIRE.defaultBlockState());
        }
        set(level, x + 7, y, z, facing(Blocks.STICKY_PISTON, Direction.EAST));
        set(level, x + 8, y, z, Blocks.SLIME_BLOCK.defaultBlockState());
        set(level, x + 2, y, z + 1, Blocks.REDSTONE_LAMP.defaultBlockState());
    }

    private static void hopperChain(ServerLevel level, int x, int z) {
        int y = GROUND_Y + 1;
        set(level, x, y + 2, z, Blocks.CHEST.defaultBlockState());
        set(level, x, y + 1, z, Blocks.HOPPER.defaultBlockState().setValue(BlockStateProperties.FACING_HOPPER, Direction.EAST));
        set(level, x + 1, y + 1, z, Blocks.HOPPER.defaultBlockState().setValue(BlockStateProperties.FACING_HOPPER, Direction.DOWN));
        set(level, x + 1, y, z, Blocks.CHEST.defaultBlockState());
        if (level.getBlockEntity(new BlockPos(x, y + 2, z)) instanceof ChestBlockEntity chest) {
            for (int slot = 0; slot < CHEST_SLOTS; slot += 3) {
                chest.setItem(slot, new ItemStack(Items.COBBLESTONE, Items.COBBLESTONE.getDefaultMaxStackSize()));
            }
        }
    }

    private static void furnace(ServerLevel level, int x, int z) {
        BlockPos pos = new BlockPos(x, GROUND_Y + 1, z);
        level.setBlock(pos, Blocks.FURNACE.defaultBlockState(), Block.UPDATE_ALL);
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity instanceof FurnaceBlockEntity furnace) {
            furnace.setItem(0, new ItemStack(Items.RAW_IRON, ORE_COUNT));
            furnace.setItem(1, new ItemStack(Items.COAL, FUEL_COUNT));
        }
    }

    private static void farm(ServerLevel level, int x, int z) {
        int center = FARM_SIZE / 2;
        for (int dx = 0; dx < FARM_SIZE; dx++) {
            for (int dz = 0; dz < FARM_SIZE; dz++) {
                if (dx == center && dz == center) {
                    set(level, x + dx, GROUND_Y, z + dz, Blocks.WATER.defaultBlockState());
                    continue;
                }
                set(level, x + dx, GROUND_Y, z + dz, Blocks.FARMLAND.defaultBlockState());
                set(level, x + dx, GROUND_Y + 1, z + dz, Blocks.WHEAT.defaultBlockState());
            }
        }
    }

    private static void fluids(ServerLevel level, int x, int z) {
        for (int dx = -1; dx <= 9; dx++) {
            for (int dz = -1; dz <= 9; dz++) {
                boolean wall = dx == -1 || dz == -1 || dx == 9 || dz == 9;
                if (wall) {
                    set(level, x + dx, GROUND_Y + 1, z + dz, Blocks.STONE.defaultBlockState());
                }
            }
        }
        set(level, x, GROUND_Y + 1, z, Blocks.WATER.defaultBlockState());
        set(level, x + 8, GROUND_Y + 1, z + 8, Blocks.LAVA.defaultBlockState());
    }

    private static void plants(ServerLevel level, int x, int z) {
        for (int i = 0; i < 6; i++) {
            set(level, x + i * 3, GROUND_Y + 1, z, Blocks.OAK_SAPLING.defaultBlockState());
            set(level, x + i * 3, GROUND_Y, z + 3, Blocks.SAND.defaultBlockState());
            set(level, x + i * 3 + 1, GROUND_Y, z + 3, Blocks.WATER.defaultBlockState());
            set(level, x + i * 3, GROUND_Y + 1, z + 3, Blocks.SUGAR_CANE.defaultBlockState());
        }
    }

    private static void torches(ServerLevel level, int x, int z) {
        for (int i = 0; i < 5; i++) {
            set(level, x + i * 2, GROUND_Y + 1, z + i, Blocks.TORCH.defaultBlockState());
        }
        set(level, x + 2, GROUND_Y + 4, z, Blocks.STONE.defaultBlockState());
    }

    private static BlockState facing(Block block, Direction direction) {
        return block.defaultBlockState().setValue(DirectionalBlock.FACING, direction);
    }

    private static void set(ServerLevel level, int x, int y, int z, BlockState state) {
        level.setBlock(new BlockPos(x, y, z), state, Block.UPDATE_ALL);
    }
}
