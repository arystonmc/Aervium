package com.aryston.kinetic.debug.benchmark;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aryston.kinetic.debug.benchmark.SimulatedPlayerRoutes.ChunkPoint;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SimulatedPlayerRoutesTest {
    @Test
    void playersSpreadInDifferentDirections() {
        Set<ChunkPoint> starts = new HashSet<>();
        for (int player = 0; player < SimulatedPlayerRoutes.PLAYERS; player++) {
            starts.add(SimulatedPlayerRoutes.position(player, 0, 0, 0));
        }
        assertEquals(SimulatedPlayerRoutes.PLAYERS, starts.size());
    }

    @Test
    void playersWalkAwayFromTheSpawnOneChunkPerStep() {
        ChunkPoint first = SimulatedPlayerRoutes.position(0, 0, 10, 20);
        ChunkPoint later = SimulatedPlayerRoutes.position(0, SimulatedPlayerRoutes.STEP_TICKS, 10, 20);
        assertEquals(first.x() + 1, later.x());
        assertEquals(first.z(), later.z());
    }

    @Test
    void playersMoveOnlyEveryStep() {
        assertTrue(SimulatedPlayerRoutes.moves(0));
        assertFalse(SimulatedPlayerRoutes.moves(1));
        assertTrue(SimulatedPlayerRoutes.moves(SimulatedPlayerRoutes.STEP_TICKS));
    }
}
