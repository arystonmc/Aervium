package com.aryston.aervium.core.telemetry;

import java.util.Arrays;

public record SampleStatistics(int count, double mean, double standardDeviation, long min, long max, long median, long p95, long p99, long p999) {
    public static final SampleStatistics EMPTY = new SampleStatistics(0, 0.0, 0.0, 0, 0, 0, 0, 0, 0);

    private static final double MEDIAN = 0.5;
    private static final double P95 = 0.95;
    private static final double P99 = 0.99;
    private static final double P999 = 0.999;

    public static SampleStatistics ofSorting(long[] values, int count) {
        if (count <= 0) {
            return EMPTY;
        }
        Arrays.sort(values, 0, count);
        double mean = mean(values, count);
        return new SampleStatistics(
            count,
            mean,
            standardDeviation(values, count, mean),
            values[0],
            values[count - 1],
            percentile(values, count, MEDIAN),
            percentile(values, count, P95),
            percentile(values, count, P99),
            percentile(values, count, P999)
        );
    }

    public static long percentile(long[] sorted, int count, double fraction) {
        if (count <= 0) {
            return 0;
        }
        int rank = (int) Math.ceil(fraction * count);
        return sorted[Math.clamp(rank - 1, 0, count - 1)];
    }

    private static double mean(long[] values, int count) {
        double sum = 0.0;
        for (int i = 0; i < count; i++) {
            sum += values[i];
        }
        return sum / count;
    }

    private static double standardDeviation(long[] values, int count, double mean) {
        double squares = 0.0;
        for (int i = 0; i < count; i++) {
            double difference = values[i] - mean;
            squares += difference * difference;
        }
        return Math.sqrt(squares / count);
    }
}
