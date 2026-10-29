package com.aryston.kinetic.debug.benchmark;

import java.util.ArrayList;
import java.util.List;

public final class StorageSetup implements SceneSetup {
    public static final int SIZE = 64;
    public static final int LAYERS = 2;

    private static final int HEIGHT = 4;
    private static final String STACK = "{Slot:0b,id:\"minecraft:cobblestone\",count:64}";
    private static final List<String> ROWS = List.of(
        "minecraft:chest[facing=south]{Items:[" + STACK + "]}",
        "minecraft:oak_sign[rotation=0]{front_text:{messages:[\"Kinetic\",\"stress\",\"test\",\"sign\"]},back_text:{messages:[\"back\",\"\",\"\",\"\"]}}",
        "minecraft:white_banner{patterns:[{pattern:\"minecraft:stripe_top\",color:\"red\"},{pattern:\"minecraft:cross\",color:\"blue\"}]}",
        "minecraft:furnace[facing=south,lit=true]{Items:[{Slot:0b,id:\"minecraft:raw_iron\",count:64},{Slot:1b,id:\"minecraft:coal_block\",count:64}]}",
        "minecraft:hopper[facing=east]{Items:[" + STACK + "]}",
        "minecraft:ender_chest[facing=south]",
        "minecraft:barrel[facing=up]{Items:[" + STACK + "]}",
        "minecraft:shulker_box"
    );

    @Override
    public int size() {
        return SIZE;
    }

    @Override
    public int height() {
        return HEIGHT;
    }

    public static int blockEntities() {
        return SIZE * SIZE * LAYERS;
    }

    @Override
    public List<String> build(BlockPoint site) {
        List<String> commands = new ArrayList<>(BoxCommands.fill(site, far(site), "minecraft:air"));
        commands.addAll(BoxCommands.fill(site.offset(0, -1, 0), site.offset(SIZE - 1, -1, SIZE - 1), "minecraft:smooth_stone"));
        for (int layer = 0; layer < LAYERS; layer++) {
            for (int row = 0; row < SIZE; row++) {
                String block = ROWS.get((row + layer) % ROWS.size());
                BlockPoint start = site.offset(0, layer, row);
                commands.addAll(BoxCommands.fill(start, start.offset(SIZE - 1, 0, 0), block));
            }
        }
        return commands;
    }

    @Override
    public List<String> everyTick(BlockPoint site, int tick) {
        return List.of();
    }

    @Override
    public List<String> clear(BlockPoint site) {
        List<String> commands = new ArrayList<>(BoxCommands.fill(site.offset(0, -1, 0), far(site), "minecraft:air"));
        commands.add(BoxCommands.killWithin(site.offset(0, -1, 0), far(site), "minecraft:item"));
        return commands;
    }
}
