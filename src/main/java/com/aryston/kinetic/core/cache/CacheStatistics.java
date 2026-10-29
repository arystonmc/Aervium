package com.aryston.kinetic.core.cache;

import java.util.concurrent.atomic.AtomicLong;

public final class CacheStatistics {
    private final AtomicLong hits = new AtomicLong();
    private final AtomicLong misses = new AtomicLong();
    private final AtomicLong workNanos = new AtomicLong();

    public void hit(long nanos) {
        hits.incrementAndGet();
        workNanos.addAndGet(nanos);
    }

    public void miss(long nanos) {
        misses.incrementAndGet();
        workNanos.addAndGet(nanos);
    }

    public long hits() {
        return hits.get();
    }

    public long misses() {
        return misses.get();
    }

    public long workNanos() {
        return workNanos.get();
    }
}
