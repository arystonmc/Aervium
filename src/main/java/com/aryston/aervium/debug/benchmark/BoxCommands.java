package com.aryston.aervium.debug.benchmark;

import java.util.ArrayList;
import java.util.List;

public final class BoxCommands {
    public static final int FILL_LIMIT = 32_768;
    public static final String STRESS_TAG = "aervium_stress";

    private BoxCommands() {
    }

    public static List<String> fill(BlockPoint from, BlockPoint to, String block) {
        int minX = Math.min(from.x(), to.x());
        int minY = Math.min(from.y(), to.y());
        int minZ = Math.min(from.z(), to.z());
        int maxX = Math.max(from.x(), to.x());
        int maxY = Math.max(from.y(), to.y());
        int maxZ = Math.max(from.z(), to.z());
        int layerArea = (maxX - minX + 1) * (maxZ - minZ + 1);
        if (layerArea > FILL_LIMIT) {
            throw new IllegalArgumentException("one layer of the box is larger than a fill command allows");
        }
        int layersPerCommand = FILL_LIMIT / layerArea;
        List<String> commands = new ArrayList<>();
        for (int y = minY; y <= maxY; y += layersPerCommand) {
            int top = Math.min(maxY, y + layersPerCommand - 1);
            commands.add("fill " + minX + " " + y + " " + minZ + " " + maxX + " " + top + " " + maxZ + " " + block);
        }
        return commands;
    }

    public static String forceLoad(BlockPoint from, BlockPoint to, boolean load) {
        return "forceload " + (load ? "add " : "remove ") + from.x() + " " + from.z() + " " + to.x() + " " + to.z();
    }

    public static List<String> killLooseDrops() {
        return List.of("kill @e[type=minecraft:item]", "kill @e[type=minecraft:experience_orb]");
    }

    public static String killTagged() {
        return "kill @e[tag=" + STRESS_TAG + "]";
    }

    public static String killWithin(BlockPoint from, BlockPoint to, String type) {
        return "kill @e[type=" + type + ",x=" + from.x() + ",y=" + from.y() + ",z=" + from.z()
            + ",dx=" + (to.x() - from.x()) + ",dy=" + (to.y() - from.y()) + ",dz=" + (to.z() - from.z()) + "]";
    }

    public static String tagged(String extraData) {
        return "{Tags:[\"" + STRESS_TAG + "\"]" + (extraData.isEmpty() ? "" : "," + extraData) + "}";
    }
}
