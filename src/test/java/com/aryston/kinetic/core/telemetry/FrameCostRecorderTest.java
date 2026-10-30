package com.aryston.kinetic.core.telemetry;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FrameCostRecorderTest {
    private static final double EPSILON = 1e-9;

    @Test
    void cpuTimeLeavesOutTheWaitsOfTheFrame() {
        FrameCostRecorder recorder = new FrameCostRecorder(8);
        recorder.frameStarted(0L);
        recorder.waited(3_000_000L);
        recorder.waited(1_000_000L);
        recorder.frameStarted(10_000_000L);
        recorder.frameStarted(15_000_000L);
        assertArrayEquals(new long[] {6_000_000L, 5_000_000L}, recorder.cpuTimes().latest(8));
    }

    @Test
    void summaryNamesTheSlowerSide() {
        FrameCostSummary gpuBound = FrameCostSummary.of(new long[] {2_000_000L, 4_000_000L}, new long[] {5_000_000L});
        assertEquals(3.0, gpuBound.averageCpuMs(), EPSILON);
        assertEquals(5.0, gpuBound.averageGpuMs(), EPSILON);
        assertEquals(FrameCostSummary.Limit.GPU, gpuBound.limit());
        assertEquals(FrameCostSummary.Limit.CPU, FrameCostSummary.of(new long[] {5_000_000L}, new long[] {2_000_000L}).limit());
        assertEquals(FrameCostSummary.Limit.UNKNOWN, FrameCostSummary.of(new long[] {5_000_000L}, new long[0]).limit());
    }
}
