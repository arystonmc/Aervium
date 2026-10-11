package com.aryston.aervium.debug.benchmark;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntBinaryOperator;

public final class BenchmarkCommands {
    public static final int REDSTONE_PLATFORM_HEIGHT = 24;

    private static final List<String> WORLD_RULES = List.of(
        "gamerule advance_time false",
        "gamerule advance_weather false",
        "gamerule spawn_mobs false",
        "weather clear",
        "time set 6000"
    );
    private static final String CROWD_TAG = "aervium_crowd";
    private static final int CROWD_COLUMNS = 8;
    private static final int CROWD_ROWS = 5;
    private static final int CROWD_SPACING = 2;
    private static final int CLOCK_CLUSTERS = 6;
    private static final int CLOCK_ROWS = 16;
    private static final int CLUSTER_WIDTH = 6;
    private static final int PLATFORM_MARGIN = 2;
    private static final int CLEARANCE = 4;

    private BenchmarkCommands() {
    }

    public static List<String> worldRules() {
        return WORLD_RULES;
    }

    public static List<String> villageCrowd(BlockPoint center, IntBinaryOperator surfaceY) {
        List<String> commands = new ArrayList<>();
        int firstX = center.x() - (CROWD_COLUMNS - 1) * CROWD_SPACING / 2;
        int firstZ = center.z() - (CROWD_ROWS - 1) * CROWD_SPACING / 2;
        for (int column = 0; column < CROWD_COLUMNS; column++) {
            for (int row = 0; row < CROWD_ROWS; row++) {
                int x = firstX + column * CROWD_SPACING;
                int z = firstZ + row * CROWD_SPACING;
                commands.add("summon minecraft:villager " + x + " " + surfaceY.applyAsInt(x, z) + " " + z + " {Tags:[\"" + CROWD_TAG + "\"]}");
            }
        }
        return commands;
    }

    public static int crowdSize() {
        return CROWD_COLUMNS * CROWD_ROWS;
    }

    public static List<String> redstoneBuild(BlockPoint origin) {
        List<String> commands = new ArrayList<>();
        BlockPoint low = origin.offset(-PLATFORM_MARGIN, 0, -PLATFORM_MARGIN);
        BlockPoint high = origin.offset(CLOCK_CLUSTERS * CLUSTER_WIDTH + PLATFORM_MARGIN, CLEARANCE, CLOCK_ROWS + PLATFORM_MARGIN);
        commands.add(fill(low, high, "minecraft:air"));
        commands.add(fill(low.offset(0, -1, 0), new BlockPoint(high.x(), origin.y() - 1, high.z()), "minecraft:stone"));
        for (int cluster = 0; cluster < CLOCK_CLUSTERS; cluster++) {
            commands.addAll(clockCluster(origin.offset(cluster * CLUSTER_WIDTH + 1, 0, 0)));
        }
        return commands;
    }

    public static BlockPoint redstoneCenter(BlockPoint origin) {
        return origin.offset(CLOCK_CLUSTERS * CLUSTER_WIDTH / 2, 0, CLOCK_ROWS / 2);
    }

    public static int clockCount() {
        return CLOCK_CLUSTERS * CLOCK_ROWS;
    }

    private static List<String> clockCluster(BlockPoint first) {
        return List.of(
            row(first.offset(-1, 0, 0), "minecraft:redstone_lamp"),
            row(first, "minecraft:observer[facing=east]"),
            row(first.offset(2, 0, 0), "minecraft:sticky_piston[facing=east]"),
            row(first.offset(3, 0, 0), "minecraft:stone"),
            row(first.offset(1, 0, 0), "minecraft:observer[facing=west]")
        );
    }

    private static String row(BlockPoint start, String block) {
        return fill(start, start.offset(0, 0, CLOCK_ROWS - 1), block);
    }

    private static String fill(BlockPoint from, BlockPoint to, String block) {
        return "fill " + from.coordinates() + " " + to.coordinates() + " " + block;
    }
}
