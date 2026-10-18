package com.aryston.aervium.core.telemetry;

public record MotionErrorSummary(int frames, double averageMs, double p99Ms, double maxMs) {
    public static MotionErrorSummary of(long[] errorNanos, int count) {
        SampleStatistics statistics = SampleStatistics.ofSorting(errorNanos, count);
        return new MotionErrorSummary(
            statistics.count(),
            Durations.millis(statistics.mean()),
            Durations.millis(statistics.p99()),
            Durations.millis(statistics.max())
        );
    }
}
