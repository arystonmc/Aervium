package com.aryston.kinetic.debug.benchmark;

import java.util.ArrayList;
import java.util.List;

public final class RedstoneSetup implements SceneSetup {
    public static final int CLOCK_CLUSTERS = 12;
    public static final int CLOCK_ROWS = 16;

    private static final int CLUSTER_WIDTH = 6;
    private static final int MARGIN = 2;
    private static final int SIZE = CLOCK_CLUSTERS * CLUSTER_WIDTH + 2 * MARGIN;
    private static final int HEIGHT = 4;

    @Override
    public int size() {
        return SIZE;
    }

    @Override
    public int height() {
        return HEIGHT;
    }

    public static int clockCount() {
        return CLOCK_CLUSTERS * CLOCK_ROWS;
    }

    @Override
    public List<String> build(BlockPoint site) {
        List<String> commands = new ArrayList<>(BoxCommands.fill(site, far(site), "minecraft:air"));
        commands.addAll(BoxCommands.fill(site.offset(0, -1, 0), site.offset(SIZE - 1, -1, SIZE - 1), "minecraft:stone"));
        BlockPoint first = site.offset(MARGIN, 0, MARGIN);
        for (int cluster = 0; cluster < CLOCK_CLUSTERS; cluster++) {
            commands.addAll(clockCluster(first.offset(cluster * CLUSTER_WIDTH + 1, 0, 0)));
        }
        return commands;
    }

    @Override
    public List<String> everyTick(BlockPoint site, int tick) {
        return List.of();
    }

    @Override
    public List<String> clear(BlockPoint site) {
        return BoxCommands.fill(site.offset(0, -1, 0), far(site), "minecraft:air");
    }

    private static List<String> clockCluster(BlockPoint first) {
        List<String> commands = new ArrayList<>();
        commands.addAll(row(first.offset(-1, 0, 0), "minecraft:redstone_lamp"));
        commands.addAll(row(first, "minecraft:observer[facing=east]"));
        commands.addAll(row(first.offset(2, 0, 0), "minecraft:sticky_piston[facing=east]"));
        commands.addAll(row(first.offset(3, 0, 0), "minecraft:stone"));
        commands.addAll(row(first.offset(1, 0, 0), "minecraft:observer[facing=west]"));
        return commands;
    }

    private static List<String> row(BlockPoint start, String block) {
        return BoxCommands.fill(start, start.offset(0, 0, CLOCK_ROWS - 1), block);
    }
}
