package com.aryston.aervium.core.budget;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class StallMeterTest {
    private static final double EPSILON = 1.0E-9;

    @Test
    void sumsAveragesAndKeepsTheLongestStall() {
        StallMeter meter = new StallMeter();
        meter.record(1_000_000);
        meter.record(3_000_000);
        StallMeter.Totals totals = meter.take();
        assertEquals(2, totals.count());
        assertEquals(4.0, totals.totalMs(), EPSILON);
        assertEquals(2.0, totals.averageMs(), EPSILON);
        assertEquals(3.0, totals.maxMs(), EPSILON);
    }

    @Test
    void startsOverAfterTaking() {
        StallMeter meter = new StallMeter();
        meter.record(5_000_000);
        meter.take();
        StallMeter.Totals empty = meter.take();
        assertEquals(0, empty.count());
        assertEquals(0.0, empty.averageMs(), EPSILON);
        assertEquals(0.0, empty.maxMs(), EPSILON);
    }
}
