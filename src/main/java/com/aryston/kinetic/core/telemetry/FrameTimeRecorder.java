package com.aryston.kinetic.core.telemetry;

public final class FrameTimeRecorder {
    private static final long NO_FRAME = -1L;

    private final SampleRing frameTimes;
    private long lastFrameStart = NO_FRAME;

    public FrameTimeRecorder(int capacity) {
        this.frameTimes = new SampleRing(capacity);
    }

    public void frameStarted(long nanoTime) {
        if (lastFrameStart != NO_FRAME) {
            frameTimes.record(nanoTime - lastFrameStart);
        }
        lastFrameStart = nanoTime;
    }

    public void reset() {
        lastFrameStart = NO_FRAME;
        frameTimes.clear();
    }

    public SampleRing frameTimes() {
        return frameTimes;
    }
}
