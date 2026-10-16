package com.aryston.aervium.core.telemetry;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class FrameTimeSummaryTest {
    private static final double EPSILON = 1e-6;
    private static final long TEN_MS = 10_000_000L;
    private static final long FIFTY_MS = 50_000_000L;
    private static final long HUNDRED_MS = 100_000_000L;

    @Test
    void steadyFramesHaveEqualAverageAndLows() {
        long[] frames = new long[1000];
        Arrays.fill(frames, TEN_MS);
        FrameTimeSummary summary = FrameTimeSummary.of(frames, frames.length);
        assertEquals(100.0, summary.averageFps(), EPSILON);
        assertEquals(100.0, summary.onePercentLowFps(), EPSILON);
        assertEquals(100.0, summary.pointOnePercentLowFps(), EPSILON);
        assertEquals(0.0, summary.standardDeviationMs(), EPSILON);
    }

    @Test
    void spikesLowerTheLowsMoreThanTheAverage() {
        long[] frames = new long[1000];
        Arrays.fill(frames, TEN_MS);
        for (int i = 0; i < 10; i++) {
            frames[i * 100] = FIFTY_MS;
        }
        frames[550] = HUNDRED_MS;
        FrameTimeSummary summary = FrameTimeSummary.of(frames, frames.length);
        assertEquals(20.0, summary.onePercentLowFps(), EPSILON);
        assertEquals(20.0, summary.pointOnePercentLowFps(), EPSILON);
        assertEquals(100.0, summary.maxMs(), EPSILON);
        double expectedAverageMs = (989 * 10.0 + 10 * 50.0 + 100.0) / 1000.0;
        assertEquals(expectedAverageMs, summary.averageMs(), EPSILON);
        assertEquals(1000.0 / expectedAverageMs, summary.averageFps(), EPSILON);
    }

    @Test
    void noFramesGiveZeroes() {
        assertEquals(0.0, FrameTimeSummary.of(new long[0], 0).averageFps());
        assertEquals(0, FrameTimeSummary.of(new long[0], 0).frames());
    }

    @Test
    void countsFramesOverTheStutterThresholds() {
        long[] frames = {10_000_000L, 33_000_000L, 33_000_001L, 50_000_000L, 50_000_001L, 120_000_000L};
        FrameTimeSummary summary = FrameTimeSummary.of(frames, frames.length);
        assertEquals(4, summary.framesOver33Ms());
        assertEquals(2, summary.framesOver50Ms());
    }
}
