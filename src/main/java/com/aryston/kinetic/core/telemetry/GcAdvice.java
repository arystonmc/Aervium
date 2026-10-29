package com.aryston.kinetic.core.telemetry;

import java.lang.management.ManagementFactory;
import java.util.Optional;

public final class GcAdvice {
    public static final String RECOMMENDED_FLAGS = "-XX:+UseZGC";
    private static final long GIGABYTE = 1L << 30;
    private static final long MIN_HEAP_BYTES = 3 * GIGABYTE;
    private static final long MIN_PHYSICAL_FOR_HEAP_ADVICE = 12 * GIGABYTE;

    private GcAdvice() {
    }

    public static Optional<String> forThisJvm() {
        long physical = ManagementFactory.getOperatingSystemMXBean() instanceof com.sun.management.OperatingSystemMXBean system
            ? system.getTotalMemorySize()
            : 0L;
        return advise(GarbageCollector.current(), Runtime.getRuntime().maxMemory(), physical);
    }

    public static Optional<String> advise(GarbageCollector collector, long maxHeapBytes, long physicalBytes) {
        boolean wrongCollector = !collector.lowLatency();
        boolean smallHeap = maxHeapBytes < MIN_HEAP_BYTES && physicalBytes >= MIN_PHYSICAL_FOR_HEAP_ADVICE;
        if (!wrongCollector && !smallHeap) {
            return Optional.empty();
        }
        StringBuilder advice = new StringBuilder();
        if (wrongCollector) {
            advice.append("the ").append(collector.displayName())
                .append(" collector stops the game for every collection; add ").append(RECOMMENDED_FLAGS)
                .append(" to the launcher's JVM arguments for pauses under a millisecond");
        }
        if (smallHeap) {
            if (!advice.isEmpty()) {
                advice.append("; ");
            }
            advice.append("the heap limit is ").append(maxHeapBytes / (1024 * 1024))
                .append(" MB, raise it to at least 4 GB (-Xmx4G) so the collector has room");
        }
        return Optional.of(advice.toString());
    }
}
