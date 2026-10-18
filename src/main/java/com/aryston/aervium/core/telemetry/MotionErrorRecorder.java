package com.aryston.aervium.core.telemetry;

public final class MotionErrorRecorder {
    private static final long NO_FRAME = -1L;

    private final SampleRing errors;
    private long lastPresent = NO_FRAME;

    public MotionErrorRecorder(int capacity) {
        this.errors = new SampleRing(capacity);
    }

    public void framePresented(long nanoTime, double simulatedMillis) {
        if (lastPresent != NO_FRAME) {
            long realNanos = nanoTime - lastPresent;
            errors.record(Math.abs(Math.round(simulatedMillis * Durations.NANOS_PER_MILLISECOND) - realNanos));
        }
        lastPresent = nanoTime;
    }

    public void interrupted() {
        lastPresent = NO_FRAME;
    }

    public SampleRing errors() {
        return errors;
    }
}
