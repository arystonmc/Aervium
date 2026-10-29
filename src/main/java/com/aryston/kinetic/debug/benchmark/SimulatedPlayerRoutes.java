package com.aryston.kinetic.debug.benchmark;

public final class SimulatedPlayerRoutes {
    public static final int PLAYERS = 12;
    public static final int VIEW_RADIUS_CHUNKS = 10;
    public static final int STEP_TICKS = 20;

    private static final int START_DISTANCE_CHUNKS = 40;
    private static final double FULL_TURN = 2.0 * Math.PI;

    private SimulatedPlayerRoutes() {
    }

    public static boolean moves(int tick) {
        return tick % STEP_TICKS == 0;
    }

    public static ChunkPoint position(int player, int tick, int spawnChunkX, int spawnChunkZ) {
        double angle = FULL_TURN * player / PLAYERS;
        int distance = START_DISTANCE_CHUNKS + tick / STEP_TICKS;
        return new ChunkPoint(
            spawnChunkX + (int) Math.round(Math.cos(angle) * distance),
            spawnChunkZ + (int) Math.round(Math.sin(angle) * distance)
        );
    }

    public record ChunkPoint(int x, int z) {
    }
}
