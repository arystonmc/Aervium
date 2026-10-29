package com.aryston.kinetic.core.telemetry;

public record VramUsage(long usedBytes, long budgetBytes) {
    public static final VramUsage UNAVAILABLE = new VramUsage(-1L, -1L);

    public boolean available() {
        return usedBytes >= 0;
    }
}
