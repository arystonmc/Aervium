package com.aryston.kinetic.debug.benchmark;

import java.util.ArrayList;
import java.util.List;

public final class ParticleStormSetup implements SceneSetup {
    public static final int SIZE = 32;

    private static final int HEIGHT = 12;
    private static final int STORM_HEIGHT = 4;
    private static final double SPREAD = 12.0;
    private static final double SPREAD_UP = 4.0;
    private static final List<Burst> STORM = List.of(
        new Burst("minecraft:flame", 0.02, 400),
        new Burst("minecraft:end_rod", 0.05, 160),
        new Burst("minecraft:campfire_cosy_smoke", 0.01, 24),
        new Burst("minecraft:explosion", 0.0, 6)
    );

    @Override
    public int size() {
        return SIZE;
    }

    @Override
    public int height() {
        return HEIGHT;
    }

    public static int particlesPerTick() {
        return STORM.stream().mapToInt(Burst::count).sum();
    }

    @Override
    public List<String> build(BlockPoint site) {
        List<String> commands = new ArrayList<>(BoxCommands.fill(site, far(site), "minecraft:air"));
        commands.addAll(BoxCommands.fill(site.offset(0, -1, 0), site.offset(SIZE - 1, -1, SIZE - 1), "minecraft:smooth_stone"));
        commands.addAll(BoxCommands.fill(site, site.offset(SIZE - 1, 0, SIZE - 1), "minecraft:campfire[lit=true]"));
        return commands;
    }

    @Override
    public List<String> everyTick(BlockPoint site, int tick) {
        BlockPoint center = center(site);
        String area = center.x() + " " + (center.y() + STORM_HEIGHT) + " " + center.z() + " " + SPREAD + " " + SPREAD_UP + " " + SPREAD;
        List<String> commands = new ArrayList<>(STORM.size());
        for (Burst burst : STORM) {
            commands.add("particle " + burst.particle() + " " + area + " " + burst.speed() + " " + burst.count() + " force");
        }
        return commands;
    }

    @Override
    public List<String> clear(BlockPoint site) {
        return BoxCommands.fill(site.offset(0, -1, 0), far(site), "minecraft:air");
    }

    private record Burst(String particle, double speed, int count) {
    }
}
