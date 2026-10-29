package com.aryston.kinetic.world;

import com.aryston.kinetic.core.budget.SystemTimings;
import com.aryston.kinetic.core.budget.TimedSystem;
import com.aryston.kinetic.core.telemetry.SampleRing;

public final class TickTimeRecorder {
    public static final TickTimeRecorder SERVER = new TickTimeRecorder();

    private static final int CAPACITY = 4096;
    private static final long NOT_TICKING = -1L;

    private final SampleRing tickTimes = new SampleRing(CAPACITY);
    private volatile boolean running;
    private long tickStart = NOT_TICKING;

    private TickTimeRecorder() {
    }

    public void tickStarted() {
        tickStart = SystemTimings.begin();
    }

    public void tickFinished() {
        if (tickStart == NOT_TICKING) {
            return;
        }
        long start = tickStart;
        long nanos = System.nanoTime() - start;
        tickStart = NOT_TICKING;
        synchronized (tickTimes) {
            tickTimes.record(nanos);
        }
        running = true;
        SystemTimings.SHARED.end(TimedSystem.SERVER_TICK, start);
    }

    public void serverStopped() {
        running = false;
        synchronized (tickTimes) {
            tickTimes.clear();
        }
    }

    public boolean isRunning() {
        return running;
    }

    public long mark() {
        synchronized (tickTimes) {
            return tickTimes.recorded();
        }
    }

    public long[] latest(int maxCount) {
        synchronized (tickTimes) {
            return tickTimes.latest(maxCount);
        }
    }

    public long[] since(long mark) {
        synchronized (tickTimes) {
            return tickTimes.since(mark);
        }
    }
}
