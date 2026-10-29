package com.aryston.kinetic.core.pregeneration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PregenerationHoldTest {
    private static final long LIMIT = 90_000L;
    private static final long STALL = 15_000L;
    private static final long SETTLE = 750L;
    private static final TerrainProgress HALF = new TerrainProgress(50, 100, 0, 0, 0);
    private static final TerrainProgress DONE = new TerrainProgress(100, 100, 40, 40, 0);

    @Test
    void waitsWhileTerrainIsMissing() {
        PregenerationHold hold = new PregenerationHold(0L, LIMIT, STALL, SETTLE);
        assertEquals(PregenerationHold.State.WAITING, hold.update(HALF, 1_000L));
    }

    @Test
    void releasesOnceCompleteTerrainHadTimeToFadeIn() {
        PregenerationHold hold = new PregenerationHold(0L, LIMIT, STALL, SETTLE);
        assertEquals(PregenerationHold.State.WAITING, hold.update(DONE, 1_000L));
        assertEquals(PregenerationHold.State.WAITING, hold.update(DONE, 1_500L));
        assertEquals(PregenerationHold.State.READY, hold.update(DONE, 1_750L));
    }

    @Test
    void restartsTheFadeWhenNewSectionsAppear() {
        PregenerationHold hold = new PregenerationHold(0L, LIMIT, STALL, SETTLE);
        hold.update(DONE, 1_000L);
        hold.update(new TerrainProgress(100, 100, 40, 41, 0), 1_500L);
        assertEquals(PregenerationHold.State.WAITING, hold.update(new TerrainProgress(100, 100, 41, 41, 0), 1_800L));
        assertEquals(PregenerationHold.State.READY, hold.update(new TerrainProgress(100, 100, 41, 41, 0), 2_550L));
    }

    @Test
    void givesUpWhenNothingAdvances() {
        PregenerationHold hold = new PregenerationHold(0L, LIMIT, STALL, SETTLE);
        hold.update(HALF, 1_000L);
        assertEquals(PregenerationHold.State.WAITING, hold.update(HALF, 15_999L));
        assertEquals(PregenerationHold.State.STALLED, hold.update(HALF, 16_000L));
    }

    @Test
    void givesUpAfterTheLimit() {
        PregenerationHold hold = new PregenerationHold(0L, LIMIT, STALL, SETTLE);
        for (int step = 1; step < 90; step++) {
            hold.update(new TerrainProgress(step, 100, 0, 0, 0), step * 1_000L);
        }
        assertEquals(PregenerationHold.State.TIMED_OUT, hold.update(new TerrainProgress(90, 100, 0, 0, 0), 90_000L));
    }
}
