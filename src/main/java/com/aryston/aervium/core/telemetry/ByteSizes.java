package com.aryston.aervium.core.telemetry;

public final class ByteSizes {
    private static final double BYTES_PER_MEGABYTE = 1024.0 * 1024.0;

    private ByteSizes() {
    }

    public static double megabytes(long bytes) {
        return bytes / BYTES_PER_MEGABYTE;
    }
}
