package com.aryston.aervium.core.hardware;

/**
 * Sorts a machine into one of three classes. A machine is held back by its weakest part, so each rule can only lower the class.
 */
public final class HardwareClassifier {
    private static final long GIGABYTE = 1L << 30;

    private static final int LOW_CPU_THREADS = 4;
    private static final int MEDIUM_CPU_THREADS = 6;
    private static final long LOW_HEAP = 2 * GIGABYTE;
    private static final long MEDIUM_HEAP = 3 * GIGABYTE;
    private static final long LOW_PHYSICAL = 8 * GIGABYTE;
    private static final long MEDIUM_PHYSICAL = 12 * GIGABYTE;
    private static final long LOW_DISCRETE_MEMORY = 2 * GIGABYTE;
    private static final long MEDIUM_DISCRETE_MEMORY = 4 * GIGABYTE;

    private HardwareClassifier() {
    }

    public static HardwareTier classify(HardwareProfile profile) {
        HardwareTier tier = HardwareTier.HIGH;
        tier = lowest(tier, byCpu(profile.cpuThreads()));
        tier = lowest(tier, byAmount(profile.maxHeapBytes(), LOW_HEAP, MEDIUM_HEAP));
        tier = lowest(tier, byAmount(profile.physicalMemoryBytes(), LOW_PHYSICAL, MEDIUM_PHYSICAL));
        tier = lowest(tier, byGpu(profile));
        return tier;
    }

    private static HardwareTier byCpu(int threads) {
        if (threads <= LOW_CPU_THREADS) {
            return HardwareTier.LOW;
        }
        return threads < MEDIUM_CPU_THREADS ? HardwareTier.MEDIUM : HardwareTier.HIGH;
    }

    private static HardwareTier byAmount(long bytes, long low, long medium) {
        if (bytes == HardwareProfile.UNKNOWN_BYTES) {
            return HardwareTier.HIGH;
        }
        if (bytes < low) {
            return HardwareTier.LOW;
        }
        return bytes < medium ? HardwareTier.MEDIUM : HardwareTier.HIGH;
    }

    private static HardwareTier byGpu(HardwareProfile profile) {
        return switch (profile.gpuKind()) {
            case SOFTWARE -> HardwareTier.LOW;
            case INTEGRATED -> HardwareTier.MEDIUM;
            case DISCRETE -> byAmount(profile.gpuMemoryBytes(), LOW_DISCRETE_MEMORY, MEDIUM_DISCRETE_MEMORY);
            case UNKNOWN -> HardwareTier.HIGH;
        };
    }

    private static HardwareTier lowest(HardwareTier a, HardwareTier b) {
        return a.ordinal() <= b.ordinal() ? a : b;
    }
}
