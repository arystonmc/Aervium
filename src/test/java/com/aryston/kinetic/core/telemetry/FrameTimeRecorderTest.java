package com.aryston.kinetic.core.telemetry;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

class FrameTimeRecorderTest {
    @Test
    void recordsTheTimeBetweenFrameStarts() {
        FrameTimeRecorder recorder = new FrameTimeRecorder(8);
        recorder.frameStarted(100);
        recorder.frameStarted(116);
        recorder.frameStarted(150);
        assertArrayEquals(new long[] {16, 34}, recorder.frameTimes().latest(8));
    }

    @Test
    void resetDropsTheGapToTheNextFrame() {
        FrameTimeRecorder recorder = new FrameTimeRecorder(8);
        recorder.frameStarted(100);
        recorder.frameStarted(110);
        recorder.reset();
        recorder.frameStarted(5_000);
        recorder.frameStarted(5_020);
        assertArrayEquals(new long[] {20}, recorder.frameTimes().latest(8));
    }
}
