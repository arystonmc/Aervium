package com.aryston.kinetic.core.budget;

import com.aryston.kinetic.core.telemetry.Durations;

public final class LapTimer {
    private long lastNanos;
    private boolean started;

    public double lapMillis(long nowNanos) {
        double elapsed = elapsedMillis(nowNanos);
        lastNanos = nowNanos;
        started = true;
        return elapsed;
    }

    public double elapsedMillis(long nowNanos) {
        return started ? Durations.millis(nowNanos - lastNanos) : 0.0;
    }
}
