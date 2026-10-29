package com.aryston.kinetic.core.telemetry;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SampleStatisticsTest {
    private static final double EPSILON = 1e-9;

    @Test
    void usesTheNearestRankForPercentiles() {
        long[] sorted = new long[100];
        for (int i = 0; i < sorted.length; i++) {
            sorted[i] = i + 1;
        }
        assertEquals(50, SampleStatistics.percentile(sorted, 100, 0.5));
        assertEquals(99, SampleStatistics.percentile(sorted, 100, 0.99));
        assertEquals(100, SampleStatistics.percentile(sorted, 100, 0.999));
        assertEquals(1, SampleStatistics.percentile(sorted, 100, 0.0));
    }

    @Test
    void computesMeanAndPopulationStandardDeviation() {
        long[] values = {2, 4, 4, 4, 5, 5, 7, 9};
        SampleStatistics statistics = SampleStatistics.ofSorting(values, values.length);
        assertEquals(5.0, statistics.mean(), EPSILON);
        assertEquals(2.0, statistics.standardDeviation(), EPSILON);
        assertEquals(2, statistics.min());
        assertEquals(9, statistics.max());
        assertEquals(4, statistics.median());
    }

    @Test
    void sortsOnlyTheCountedPrefix() {
        long[] values = {30, 10, 20, 0};
        SampleStatistics statistics = SampleStatistics.ofSorting(values, 3);
        assertEquals(3, statistics.count());
        assertEquals(10, statistics.min());
        assertEquals(30, statistics.max());
        assertEquals(0, values[3]);
    }

    @Test
    void anEmptySampleIsEmpty() {
        assertEquals(SampleStatistics.EMPTY, SampleStatistics.ofSorting(new long[0], 0));
    }
}
