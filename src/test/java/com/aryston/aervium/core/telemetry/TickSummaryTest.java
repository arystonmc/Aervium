package com.aryston.aervium.core.telemetry;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TickSummaryTest {
    private static final double EPSILON = 1e-6;
    private static final long MILLISECOND = 1_000_000L;

    @Test
    void sortsTicksIntoMillisecondBuckets() {
        long[] ticks = {3 * MILLISECOND, 5 * MILLISECOND, 8 * MILLISECOND, 25 * MILLISECOND, 50 * MILLISECOND, 51 * MILLISECOND, 120 * MILLISECOND};
        TickSummary summary = TickSummary.of(ticks, ticks.length);
        assertArrayEquals(new int[] {2, 1, 0, 1, 0, 1, 2}, summary.histogram());
        assertEquals(2, summary.ticksOverBudget());
        assertEquals(2.0 / 7.0, summary.overBudgetShare(), EPSILON);
        assertEquals(120.0, summary.maxMs(), EPSILON);
        assertEquals(25.0, summary.medianMs(), EPSILON);
    }

    @Test
    void anEmptyTickListHasNoShare() {
        assertEquals(0, TickSummary.of(new long[0], 0).ticks());
        assertEquals(0.0, TickSummary.of(new long[0], 0).overBudgetShare());
    }
}
