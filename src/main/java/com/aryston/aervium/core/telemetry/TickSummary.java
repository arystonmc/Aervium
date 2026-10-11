package com.aryston.aervium.core.telemetry;

public record TickSummary(
    int ticks,
    double averageMs,
    double standardDeviationMs,
    double medianMs,
    double p95Ms,
    double p99Ms,
    double maxMs,
    int ticksOverBudget,
    int[] histogram
) {
    public static final long[] HISTOGRAM_UPPER_BOUNDS_MS = {5, 10, 20, 30, 40, 50};

    public static TickSummary of(long[] tickNanos, int count) {
        int[] histogram = histogram(tickNanos, count);
        int overBudget = histogram[histogram.length - 1];
        SampleStatistics statistics = SampleStatistics.ofSorting(tickNanos, count);
        return new TickSummary(
            statistics.count(),
            Durations.millis(statistics.mean()),
            Durations.millis(statistics.standardDeviation()),
            Durations.millis(statistics.median()),
            Durations.millis(statistics.p95()),
            Durations.millis(statistics.p99()),
            Durations.millis(statistics.max()),
            overBudget,
            histogram
        );
    }

    public double overBudgetShare() {
        return ticks == 0 ? 0.0 : (double) ticksOverBudget / ticks;
    }

    private static int[] histogram(long[] tickNanos, int count) {
        int[] buckets = new int[HISTOGRAM_UPPER_BOUNDS_MS.length + 1];
        for (int i = 0; i < count; i++) {
            buckets[bucket(tickNanos[i])]++;
        }
        return buckets;
    }

    private static int bucket(long nanos) {
        double millis = Durations.millis(nanos);
        for (int i = 0; i < HISTOGRAM_UPPER_BOUNDS_MS.length; i++) {
            if (millis <= HISTOGRAM_UPPER_BOUNDS_MS[i]) {
                return i;
            }
        }
        return HISTOGRAM_UPPER_BOUNDS_MS.length;
    }
}
