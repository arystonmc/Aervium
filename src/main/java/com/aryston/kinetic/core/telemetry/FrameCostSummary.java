package com.aryston.kinetic.core.telemetry;

public record FrameCostSummary(double averageCpuMs, double p99CpuMs, double averageGpuMs, double p99GpuMs, Limit limit) {
    public static final FrameCostSummary EMPTY = new FrameCostSummary(0.0, 0.0, 0.0, 0.0, Limit.UNKNOWN);

    public static FrameCostSummary of(long[] cpuNanos, long[] gpuNanos) {
        SampleStatistics cpu = SampleStatistics.ofSorting(cpuNanos, cpuNanos.length);
        SampleStatistics gpu = SampleStatistics.ofSorting(gpuNanos, gpuNanos.length);
        return new FrameCostSummary(
            Durations.millis(cpu.mean()),
            Durations.millis(cpu.p99()),
            Durations.millis(gpu.mean()),
            Durations.millis(gpu.p99()),
            Limit.of(cpu, gpu)
        );
    }

    public enum Limit {
        CPU("CPU bound"),
        GPU("GPU bound"),
        UNKNOWN("no GPU timings");

        private final String label;

        Limit(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }

        static Limit of(SampleStatistics cpu, SampleStatistics gpu) {
            if (cpu.count() == 0 || gpu.count() == 0) {
                return UNKNOWN;
            }
            return gpu.mean() >= cpu.mean() ? GPU : CPU;
        }
    }
}
