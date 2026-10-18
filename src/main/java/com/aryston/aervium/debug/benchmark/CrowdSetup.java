package com.aryston.aervium.debug.benchmark;

import java.util.ArrayList;
import java.util.List;

public final class CrowdSetup implements SceneSetup {
    public static final int MOBS = 1600;
    public static final int ITEMS = 800;
    public static final int ARMOR_STANDS = 200;

    private static final int MARGIN = 4;
    private static final int PEN = 40;
    private static final int SIZE = PEN + 2 * MARGIN;
    private static final int HEIGHT = 16;
    private static final int WALL_HEIGHT = 3;
    private static final int ITEM_LAYER = 6;
    private static final int STAND_LAYER = 10;
    private static final int STAND_COLUMNS = 20;
    private static final int STAND_SPACING = 2;
    private static final double CELL_CENTER = 0.5;
    private static final List<String> MOB_TYPES = List.of(
        "minecraft:cow", "minecraft:sheep", "minecraft:pig", "minecraft:chicken",
        "minecraft:villager", "minecraft:rabbit", "minecraft:fox", "minecraft:goat"
    );
    private static final List<String> ITEM_TYPES = List.of(
        "minecraft:diamond", "minecraft:emerald", "minecraft:iron_ingot", "minecraft:gold_ingot",
        "minecraft:apple", "minecraft:bread", "minecraft:oak_log", "minecraft:redstone"
    );

    @Override
    public int size() {
        return SIZE;
    }

    @Override
    public int height() {
        return HEIGHT;
    }

    @Override
    public List<String> build(BlockPoint site) {
        List<String> commands = new ArrayList<>(BoxCommands.fill(site, far(site), "minecraft:air"));
        commands.addAll(BoxCommands.fill(site.offset(0, -1, 0), site.offset(SIZE - 1, -1, SIZE - 1), "minecraft:smooth_stone"));
        commands.addAll(walls(site.offset(MARGIN - 1, 0, MARGIN - 1)));
        BlockPoint pen = site.offset(MARGIN, 0, MARGIN);
        for (int index = 0; index < MOBS; index++) {
            commands.add(summon(MOB_TYPES.get(index % MOB_TYPES.size()), pen, index % PEN, 0, index / PEN, "PersistenceRequired:1b"));
        }
        for (int index = 0; index < ITEMS; index++) {
            String item = ITEM_TYPES.get(index % ITEM_TYPES.size());
            commands.add(summon("minecraft:item", pen, index % PEN, ITEM_LAYER, index / PEN,
                "Item:{id:\"" + item + "\",count:1},Age:-32768,PickupDelay:32767,NoGravity:1b"));
        }
        for (int index = 0; index < ARMOR_STANDS; index++) {
            commands.add(summon("minecraft:armor_stand", pen, index % STAND_COLUMNS * STAND_SPACING, STAND_LAYER,
                index / STAND_COLUMNS * STAND_SPACING, "NoGravity:1b,ShowArms:1b"));
        }
        return commands;
    }

    @Override
    public List<String> everyTick(BlockPoint site, int tick) {
        return List.of();
    }

    @Override
    public List<String> clear(BlockPoint site) {
        List<String> commands = new ArrayList<>(List.of(BoxCommands.killTagged()));
        commands.addAll(BoxCommands.fill(site.offset(0, -1, 0), far(site), "minecraft:air"));
        return commands;
    }

    private static List<String> walls(BlockPoint corner) {
        int span = PEN + 1;
        BlockPoint top = corner.offset(0, WALL_HEIGHT - 1, 0);
        List<String> commands = new ArrayList<>();
        commands.addAll(BoxCommands.fill(corner, top.offset(span, 0, 0), "minecraft:glass"));
        commands.addAll(BoxCommands.fill(corner.offset(0, 0, span), top.offset(span, 0, span), "minecraft:glass"));
        commands.addAll(BoxCommands.fill(corner, top.offset(0, 0, span), "minecraft:glass"));
        commands.addAll(BoxCommands.fill(corner.offset(span, 0, 0), top.offset(span, 0, span), "minecraft:glass"));
        return commands;
    }

    private static String summon(String type, BlockPoint origin, int x, int y, int z, String data) {
        return "summon " + type + " " + (origin.x() + x + CELL_CENTER) + " " + (origin.y() + y) + " " + (origin.z() + z + CELL_CENTER)
            + " " + BoxCommands.tagged(data);
    }
}
