package com.aryston.aervium.core.hardware;

public record HardwareProfile(
    int cpuThreads,
    long maxHeapBytes,
    long physicalMemoryBytes,
    String gpuName,
    GpuKind gpuKind,
    long gpuMemoryBytes
) {
    public static final long UNKNOWN_BYTES = 0L;

    public String describe() {
        return cpuThreads + " CPU threads, " + megabytes(maxHeapBytes) + " MB heap, "
            + megabytes(physicalMemoryBytes) + " MB RAM, " + gpuName + " (" + gpuKind.name().toLowerCase()
            + ", " + megabytes(gpuMemoryBytes) + " MB video memory)";
    }

    private static long megabytes(long bytes) {
        return bytes / (1024 * 1024);
    }
}
