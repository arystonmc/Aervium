package com.aryston.aervium.core.telemetry;

public record HeapUsage(long usedBytes, long maxBytes) {
    public static HeapUsage current() {
        Runtime runtime = Runtime.getRuntime();
        return new HeapUsage(runtime.totalMemory() - runtime.freeMemory(), runtime.maxMemory());
    }
}
