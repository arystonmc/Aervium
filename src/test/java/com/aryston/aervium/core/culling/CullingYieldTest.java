package com.aryston.aervium.core.culling;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CullingYieldTest {
    private static final float USEFUL_SHARE = 0.15F;
    private static final int LOW_RESULTS = 3;
    private static final long PAUSE = 1_000L;

    @Test
    void cullingThatHidesEnoughNeverPauses() {
        CullingYield yield = new CullingYield(USEFUL_SHARE, LOW_RESULTS, PAUSE);

        for (int result = 0; result < 10; result++) {
            assertFalse(yield.result(100, 90, result));
        }
        assertFalse(yield.paused(10));
    }

    @Test
    void cullingThatHidesLittlePausesAfterSeveralResultsAndProbesAgainAfterThePause() {
        CullingYield yield = new CullingYield(USEFUL_SHARE, LOW_RESULTS, PAUSE);

        assertFalse(yield.result(100, 5, 0));
        assertFalse(yield.result(100, 5, 1));
        assertTrue(yield.result(100, 5, 2));
        assertTrue(yield.paused(500));
        assertFalse(yield.paused(1_002));
        assertTrue(yield.result(100, 5, 1_003));
        assertTrue(yield.paused(1_500));
    }

    @Test
    void oneUsefulProbeKeepsCullingRunning() {
        CullingYield yield = new CullingYield(USEFUL_SHARE, LOW_RESULTS, PAUSE);
        yield.result(100, 5, 0);
        yield.result(100, 5, 1);
        yield.result(100, 5, 2);

        assertFalse(yield.result(100, 80, 1_003));
        assertFalse(yield.result(100, 5, 1_004));
        assertFalse(yield.paused(1_005));
    }

    @Test
    void resetEndsAPause() {
        CullingYield yield = new CullingYield(USEFUL_SHARE, LOW_RESULTS, PAUSE);
        yield.result(0, 0, 0);
        yield.result(0, 0, 1);
        yield.result(0, 0, 2);
        yield.reset();

        assertFalse(yield.paused(3));
    }
}
