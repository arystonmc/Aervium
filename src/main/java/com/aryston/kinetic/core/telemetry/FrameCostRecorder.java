package com.aryston.kinetic.core.telemetry;

public final class FrameCostRecorder {
    private static final int SHARED_CAPACITY = 1 << 17;
    private static final long NO_FRAME = -1L;
    public static final FrameCostRecorder SHARED = new FrameCostRecorder(SHARED_CAPACITY);

    private final SampleRing cpuTimes;
    private final SampleRing gpuTimes;
    private long lastFrameStart = NO_FRAME;
    private long waited;

    public FrameCostRecorder(int capacity) {
        this.cpuTimes = new SampleRing(capacity);
        this.gpuTimes = new SampleRing(capacity);
    }

    public void waited(long nanos) {
        waited += nanos;
    }

    public void frameStarted(long nanoTime) {
        if (lastFrameStart != NO_FRAME) {
            cpuTimes.record(Math.max(0L, nanoTime - lastFrameStart - waited));
        }
        lastFrameStart = nanoTime;
        waited = 0L;
    }

    public void gpuFrameMeasured(long nanos) {
        gpuTimes.record(nanos);
    }

    public SampleRing cpuTimes() {
        return cpuTimes;
    }

    public SampleRing gpuTimes() {
        return gpuTimes;
    }
}
