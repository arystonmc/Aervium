package com.aryston.aervium.core.telemetry;

public record FrameTimeSummary(
    int frames,
    double averageFps,
    double onePercentLowFps,
    double pointOnePercentLowFps,
    double averageMs,
    double standardDeviationMs,
    double medianMs,
    double p99Ms,
    double p999Ms,
    double maxMs
) {
    public static FrameTimeSummary of(long[] frameNanos, int count) {
        return from(SampleStatistics.ofSorting(frameNanos, count));
    }

    public static FrameTimeSummary from(SampleStatistics statistics) {
        return new FrameTimeSummary(
            statistics.count(),
            Durations.perSecond(statistics.mean()),
            Durations.perSecond(statistics.p99()),
            Durations.perSecond(statistics.p999()),
            Durations.millis(statistics.mean()),
            Durations.millis(statistics.standardDeviation()),
            Durations.millis(statistics.median()),
            Durations.millis(statistics.p99()),
            Durations.millis(statistics.p999()),
            Durations.millis(statistics.max())
        );
    }
}
