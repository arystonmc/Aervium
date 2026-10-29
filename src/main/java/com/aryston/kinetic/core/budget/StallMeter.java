package com.aryston.kinetic.core.budget;

public final class StallMeter {
    private long count;
    private long totalNanos;
    private long maxNanos;

    public void record(long nanos) {
        count++;
        totalNanos += nanos;
        maxNanos = Math.max(maxNanos, nanos);
    }

    public Totals take() {
        Totals totals = new Totals(count, totalNanos, maxNanos);
        count = 0;
        totalNanos = 0;
        maxNanos = 0;
        return totals;
    }

    public record Totals(long count, long totalNanos, long maxNanos) {
        private static final double NANOS_PER_MILLI = 1.0E6;

        public double averageMs() {
            return count == 0 ? 0.0 : totalNanos / NANOS_PER_MILLI / count;
        }

        public double totalMs() {
            return totalNanos / NANOS_PER_MILLI;
        }

        public double maxMs() {
            return maxNanos / NANOS_PER_MILLI;
        }
    }
}
