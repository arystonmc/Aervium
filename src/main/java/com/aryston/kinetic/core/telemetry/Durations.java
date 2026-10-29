package com.aryston.kinetic.core.telemetry;

public final class Durations {
    public static final double NANOS_PER_MILLISECOND = 1_000_000.0;
    public static final double NANOS_PER_SECOND = 1_000_000_000.0;
    public static final long TICK_BUDGET_NANOS = 50_000_000L;

    private Durations() {
    }

    public static double millis(double nanos) {
        return nanos / NANOS_PER_MILLISECOND;
    }

    public static double perSecond(double nanos) {
        return nanos <= 0.0 ? 0.0 : NANOS_PER_SECOND / nanos;
    }
}
