package com.aryston.aervium.core.telemetry;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MotionErrorRecorderTest {
    @Test
    void recordsHowFarTheSimulatedStepMissesTheRealOne() {
        MotionErrorRecorder recorder = new MotionErrorRecorder(8);
        recorder.framePresented(0L, 0.0);
        recorder.framePresented(3_300_000L, 3.0);
        recorder.framePresented(6_600_000L, 4.0);
        assertArrayEquals(new long[] {300_000L, 700_000L}, recorder.errors().latest(8));
    }

    @Test
    void anInterruptionSkipsTheNextStep() {
        MotionErrorRecorder recorder = new MotionErrorRecorder(8);
        recorder.framePresented(0L, 0.0);
        recorder.interrupted();
        recorder.framePresented(500_000_000L, 1.0);
        assertEquals(0, recorder.errors().size());
    }

    @Test
    void summarizesErrorsInMilliseconds() {
        MotionErrorSummary summary = MotionErrorSummary.of(new long[] {1_000_000L, 3_000_000L}, 2);
        assertEquals(2.0, summary.averageMs(), 1e-9);
        assertEquals(3.0, summary.maxMs(), 1e-9);
    }
}
