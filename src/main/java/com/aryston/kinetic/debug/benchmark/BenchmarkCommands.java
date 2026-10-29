package com.aryston.kinetic.debug.benchmark;

import java.util.List;

public final class BenchmarkCommands {
    private static final List<String> WORLD_RULES = List.of(
        "gamerule advance_time false",
        "gamerule advance_weather false",
        "gamerule spawn_mobs false",
        "weather clear",
        "time set 6000"
    );

    private BenchmarkCommands() {
    }

    public static List<String> worldRules() {
        return WORLD_RULES;
    }
}
