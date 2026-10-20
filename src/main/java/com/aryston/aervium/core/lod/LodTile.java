package com.aryston.aervium.core.lod;

import org.jspecify.annotations.Nullable;

public final class LodTile {
    public static final int SIZE = 64;
    public static final int SAMPLES = SIZE * SIZE;

    private final int level;
    private final int tileX;
    private final int tileZ;
    private final long[] samples = new long[SAMPLES];
    private int present;
    private int complete;
    private int approximate;
    private @Nullable LodLayers layers;
    private int version;

    LodTile(int level, int tileX, int tileZ) {
        this.level = level;
        this.tileX = tileX;
        this.tileZ = tileZ;
    }

    public int level() {
        return level;
    }

    public int tileX() {
        return tileX;
    }

    public int tileZ() {
        return tileZ;
    }

    public int present() {
        return present;
    }

    public int version() {
        return version;
    }

    public boolean complete() {
        return complete == SAMPLES;
    }

    public boolean exact() {
        return complete() && approximate == 0;
    }

    public @Nullable LodLayers layers() {
        return layers;
    }

    void setLayers(@Nullable LodLayers value) {
        if (value != null && value.spans() == 0) {
            value = null;
        }
        if (layers == value || layers != null && value != null && layers.matches(value)) {
            return;
        }
        layers = value;
        version++;
    }

    public long bytes() {
        return (long) SAMPLES * Long.BYTES + (layers == null ? 0L : layers.bytes());
    }

    public long sample(int x, int z) {
        return samples[z * SIZE + x];
    }

    long[] samples() {
        return samples;
    }

    void set(int x, int z, long sample) {
        int index = z * SIZE + x;
        long previous = samples[index];
        if (previous == sample) {
            return;
        }
        if (LodSample.present(previous) != LodSample.present(sample)) {
            present += LodSample.present(sample) ? 1 : -1;
        }
        if (LodSample.complete(previous) != LodSample.complete(sample)) {
            complete += LodSample.complete(sample) ? 1 : -1;
        }
        if (LodSample.approximate(previous) != LodSample.approximate(sample)) {
            approximate += LodSample.approximate(sample) ? 1 : -1;
        }
        samples[index] = sample;
        version++;
    }
}
