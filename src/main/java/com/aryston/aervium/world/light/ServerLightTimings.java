package com.aryston.aervium.world.light;

import com.aryston.aervium.core.budget.StallMeter;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

/** Time the server light worker spends per batch of light updates, for the parity and benchmark logs. */
public final class ServerLightTimings {
    private static final ThreadMXBean THREADS = ManagementFactory.getThreadMXBean();
    private static final StallMeter BATCHES = new StallMeter();
    private static long cpuNanos;

    private ServerLightTimings() {
    }

    /** CPU time of the calling thread, or 0 when the JVM cannot measure it. */
    public static long threadCpuNanos() {
        return THREADS.isCurrentThreadCpuTimeSupported() ? THREADS.getCurrentThreadCpuTime() : 0L;
    }

    public static void batchFinished(long startNanos, long startCpuNanos) {
        long nanos = System.nanoTime() - startNanos;
        long cpu = threadCpuNanos() - startCpuNanos;
        synchronized (BATCHES) {
            BATCHES.record(nanos);
            cpuNanos += cpu;
        }
    }

    public static Totals take() {
        synchronized (BATCHES) {
            Totals totals = new Totals(BATCHES.take(), cpuNanos / 1.0E6);
            cpuNanos = 0;
            return totals;
        }
    }

    public record Totals(StallMeter.Totals wall, double cpuMs) {
    }
}
