package com.aryston.aervium.core.telemetry;

public final class SampleRing {
    private final long[] samples;
    private long recorded;

    public SampleRing(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.samples = new long[capacity];
    }

    public void record(long sample) {
        samples[(int) (recorded % samples.length)] = sample;
        recorded++;
    }

    public void clear() {
        recorded = 0;
    }

    public int size() {
        return (int) Math.min(recorded, samples.length);
    }

    public long recorded() {
        return recorded;
    }

    public int copyLatest(long[] destination, int maxCount) {
        int count = Math.min(Math.min(maxCount, destination.length), size());
        long first = recorded - count;
        for (int i = 0; i < count; i++) {
            destination[i] = samples[(int) ((first + i) % samples.length)];
        }
        return count;
    }

    public long[] since(long mark) {
        return latest((int) Math.min(Math.max(0, recorded - mark), size()));
    }

    public long[] latest(int maxCount) {
        long[] copy = new long[Math.min(maxCount, size())];
        copyLatest(copy, copy.length);
        return copy;
    }
}
