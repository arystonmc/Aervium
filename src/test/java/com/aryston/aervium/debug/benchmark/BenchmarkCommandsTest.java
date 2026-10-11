package com.aryston.aervium.debug.benchmark;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class BenchmarkCommandsTest {
    private static final int MAX_FILL_VOLUME = 32_768;

    @Test
    void summonsTheWholeCrowdOnTheSurface() {
        List<String> commands = BenchmarkCommands.villageCrowd(new BlockPoint(100, 70, 200), (x, z) -> 64 + (x + z) % 3);
        assertEquals(BenchmarkCommands.crowdSize(), commands.size());
        assertTrue(commands.stream().allMatch(command -> command.startsWith("summon minecraft:villager ")));
        assertEquals(commands.size(), commands.stream().distinct().count());
    }

    @Test
    void buildsTheRedstoneClocksAfterTheirSurroundings() {
        List<String> commands = BenchmarkCommands.redstoneBuild(new BlockPoint(0, 90, 0));
        assertTrue(commands.get(0).endsWith("minecraft:air"));
        assertTrue(commands.get(1).endsWith("minecraft:stone"));
        assertTrue(commands.getLast().endsWith("minecraft:observer[facing=west]"));
        long eastObservers = commands.stream().filter(command -> command.endsWith("observer[facing=east]")).count();
        assertEquals(BenchmarkCommands.clockCount() / 16, eastObservers);
    }

    @Test
    void everyFillStaysWithinTheVanillaLimit() {
        BenchmarkCommands.redstoneBuild(new BlockPoint(-40, 120, 33)).stream()
            .filter(command -> command.startsWith("fill "))
            .forEach(command -> assertTrue(volume(command) <= MAX_FILL_VOLUME, command));
    }

    @Test
    void freezesTimeAndWeather() {
        assertTrue(BenchmarkCommands.worldRules().contains("gamerule advance_time false"));
        assertTrue(BenchmarkCommands.worldRules().contains("gamerule advance_weather false"));
    }

    private static long volume(String fillCommand) {
        String[] parts = fillCommand.split(" ");
        long dx = Math.abs(Long.parseLong(parts[4]) - Long.parseLong(parts[1])) + 1;
        long dy = Math.abs(Long.parseLong(parts[5]) - Long.parseLong(parts[2])) + 1;
        long dz = Math.abs(Long.parseLong(parts[6]) - Long.parseLong(parts[3])) + 1;
        return dx * dy * dz;
    }
}
