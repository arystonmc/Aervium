package com.aryston.aervium.debug.benchmark;

import java.util.ArrayList;
import java.util.List;

public final class ExplosionSetup implements SceneSetup {
    public static final int SIZE = 16;
    public static final int TNT_LAYERS = 8;

    private static final int HEIGHT = 48;
    private static final int DEPTH = 24;

    @Override
    public int size() {
        return SIZE;
    }

    @Override
    public int height() {
        return HEIGHT;
    }

    public static int tntCount() {
        return SIZE * SIZE * TNT_LAYERS;
    }

    @Override
    public List<String> build(BlockPoint site) {
        List<String> commands = new ArrayList<>(BoxCommands.fill(site, far(site), "minecraft:air"));
        commands.addAll(BoxCommands.fill(site, site.offset(SIZE - 1, TNT_LAYERS - 1, SIZE - 1), "minecraft:tnt"));
        return commands;
    }

    @Override
    public List<String> everyTick(BlockPoint site, int tick) {
        if (tick != BenchmarkRoute.SETTLE_TICKS) {
            return List.of();
        }
        BlockPoint fuse = center(site).offset(0, TNT_LAYERS, 0);
        return List.of("setblock " + fuse.coordinates() + " minecraft:redstone_block");
    }

    @Override
    public List<String> clear(BlockPoint site) {
        BlockPoint low = site.offset(-SIZE, -DEPTH, -SIZE);
        BlockPoint high = far(site).offset(SIZE, 0, SIZE);
        return List.of(
            BoxCommands.killWithin(low, high, "minecraft:item"),
            BoxCommands.killWithin(low, high, "minecraft:tnt")
        );
    }
}
