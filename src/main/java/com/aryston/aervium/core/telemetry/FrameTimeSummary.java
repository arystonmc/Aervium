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
    double maxMs,
    int framesOver33Ms,
    int framesOver50Ms
) {
    public static final long STUTTER_NANOS = 33_000_000L;
    public static final long LONG_STUTTER_NANOS = 50_000_000L;

    public static FrameTimeSummary of(long[] frameNanos, int count) {
        int over33 = 0;
        int over50 = 0;
        for (int index = 0; index < count; index++) {
            if (frameNanos[index] > STUTTER_NANOS) {
                over33++;
            }
            if (frameNanos[index] > LONG_STUTTER_NANOS) {
                over50++;
            }
        }
        SampleStatistics statistics = SampleStatistics.ofSorting(frameNanos, count);
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
            Durations.millis(statistics.max()),
            over33,
            over50
        );
    }
}
