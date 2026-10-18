package com.aryston.aervium.debug.benchmark;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class BenchmarkCommandsTest {
    private static final BlockPoint SITE = new BlockPoint(-40, 120, 33);

    @Test
    void freezesTimeAndWeather() {
        assertTrue(BenchmarkCommands.worldRules().contains("gamerule advance_time false"));
        assertTrue(BenchmarkCommands.worldRules().contains("gamerule advance_weather false"));
    }

    @Test
    void splitsLargeBoxesIntoFillsWithinTheVanillaLimit() {
        List<String> fills = BoxCommands.fill(new BlockPoint(0, 0, 0), new BlockPoint(63, 47, 63), "minecraft:air");
        assertEquals(6, fills.size());
        fills.forEach(command -> assertTrue(volume(command) <= BoxCommands.FILL_LIMIT, command));
        assertEquals(64L * 48 * 64, fills.stream().mapToLong(BenchmarkCommandsTest::volume).sum());
    }

    @Test
    void refusesALayerLargerThanOneFill() {
        assertThrows(IllegalArgumentException.class, () -> BoxCommands.fill(new BlockPoint(0, 0, 0), new BlockPoint(200, 0, 200), "minecraft:air"));
    }

    @Test
    void everySceneKeepsItsFillsWithinTheVanillaLimit() {
        for (BenchmarkScene scene : BenchmarkScene.values()) {
            SceneSetup setup = scene.setup();
            for (List<String> commands : List.of(setup.build(SITE), setup.clear(SITE))) {
                commands.stream()
                    .filter(command -> command.startsWith("fill "))
                    .forEach(command -> assertTrue(volume(command) <= BoxCommands.FILL_LIMIT, scene + ": " + command));
            }
        }
    }

    @Test
    void theCrowdSummonsEveryEntityWithTheStressTag() {
        List<String> summons = new CrowdSetup().build(SITE).stream().filter(command -> command.startsWith("summon ")).toList();
        assertEquals(CrowdSetup.MOBS + CrowdSetup.ITEMS + CrowdSetup.ARMOR_STANDS, summons.size());
        summons.forEach(command -> assertTrue(command.contains(BoxCommands.STRESS_TAG), command));
        assertTrue(new CrowdSetup().clear(SITE).contains(BoxCommands.killTagged()));
    }

    @Test
    void theStorageFillsEveryRowOfEveryLayer() {
        long rows = new StorageSetup().build(SITE).stream().filter(command -> command.contains("{") || command.contains("ender_chest") || command.contains("shulker_box")).count();
        assertEquals(StorageSetup.SIZE * StorageSetup.LAYERS, rows);
    }

    @Test
    void theExplosionIsLitOnlyWhenTheMeasurementStarts() {
        ExplosionSetup explosion = new ExplosionSetup();
        assertTrue(explosion.everyTick(SITE, BenchmarkRoute.SETTLE_TICKS - 1).isEmpty());
        assertEquals(1, explosion.everyTick(SITE, BenchmarkRoute.SETTLE_TICKS).size());
        assertTrue(explosion.everyTick(SITE, BenchmarkRoute.SETTLE_TICKS).getFirst().endsWith("minecraft:redstone_block"));
    }

    @Test
    void theParticleStormSpawnsItsParticlesEveryTick() {
        List<String> storm = new ParticleStormSetup().everyTick(SITE, 0);
        int count = storm.stream().mapToInt(command -> Integer.parseInt(command.split(" ")[9])).sum();
        assertEquals(ParticleStormSetup.particlesPerTick(), count);
    }

    @Test
    void scenesWithoutASiteBuildNothing() {
        assertTrue(BenchmarkScene.WALK.setup().build(SITE).isEmpty());
        assertTrue(BenchmarkScene.SERVER.setup().clear(SITE).isEmpty());
    }

    private static long volume(String fillCommand) {
        String[] parts = fillCommand.split(" ");
        long dx = Math.abs(Long.parseLong(parts[4]) - Long.parseLong(parts[1])) + 1;
        long dy = Math.abs(Long.parseLong(parts[5]) - Long.parseLong(parts[2])) + 1;
        long dz = Math.abs(Long.parseLong(parts[6]) - Long.parseLong(parts[3])) + 1;
        return dx * dy * dz;
    }
}
