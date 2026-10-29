package com.aryston.kinetic.core.budget;

import com.aryston.kinetic.core.telemetry.Durations;
import com.aryston.kinetic.core.telemetry.TelemetrySnapshot.SystemTiming;
import java.util.Arrays;
import java.util.List;

public final class SystemTimings {
    public static final SystemTimings SHARED = new SystemTimings();

    private static final double SMOOTHING = 0.1;

    private final double[] smoothedNanos = new double[TimedSystem.values().length];
    private final long[] peakNanos = new long[TimedSystem.values().length];

    SystemTimings() {
    }

    public static long begin() {
        return System.nanoTime();
    }

    public void end(TimedSystem system, long startNanos) {
        record(system, System.nanoTime() - startNanos);
    }

    void record(TimedSystem system, long nanos) {
        int index = system.ordinal();
        double previous = smoothedNanos[index];
        smoothedNanos[index] = previous == 0.0 ? nanos : previous + (nanos - previous) * SMOOTHING;
        peakNanos[index] = Math.max(peakNanos[index], nanos);
    }

    public List<SystemTiming> sample(FrameBudget budget) {
        return Arrays.stream(TimedSystem.values()).map(system -> takeTiming(system, budget)).toList();
    }

    private SystemTiming takeTiming(TimedSystem system, FrameBudget budget) {
        int index = system.ordinal();
        long peak = peakNanos[index];
        peakNanos[index] = 0;
        double smoothed = smoothedNanos[index];
        return new SystemTiming(system, Durations.millis(smoothed), Durations.millis(peak), budget.isOverBudget(system, Math.round(smoothed)));
    }
}
