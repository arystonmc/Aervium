package com.aryston.aervium.core.lod;

public enum GenerationPower {
    LOW(1, 0, 3),
    BALANCED(4, 2, 1),
    HIGH(2, 2, 0),
    EXTREME(1, 1, 0);

    private static final int MIN_THREADS = 1;

    private final int coresPerThread;
    private final int reservedCores;
    private final int idleMultiplier;

    GenerationPower(int coresPerThread, int reservedCores, int idleMultiplier) {
        this.coresPerThread = coresPerThread;
        this.reservedCores = reservedCores;
        this.idleMultiplier = idleMultiplier;
    }

    public int threads(int cores) {
        if (this == LOW) {
            return MIN_THREADS;
        }
        return Math.max(MIN_THREADS, (cores - reservedCores) / coresPerThread);
    }

    public long idleNanos(long workNanos) {
        return Math.max(0L, workNanos) * idleMultiplier;
    }
}
