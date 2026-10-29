package com.aryston.kinetic.core.lod;

import org.jspecify.annotations.Nullable;

public record LodData(long[] samples, @Nullable LodLayers layers) {
    public LodData {
        if (samples.length != LodTile.SAMPLES || layers != null && layers.columns() != samples.length) {
            throw new IllegalArgumentException("Invalid distant terrain tile payload");
        }
    }

    public boolean exact() {
        for (long sample : samples) {
            if (!LodSample.complete(sample) || LodSample.approximate(sample)) {
                return false;
            }
        }
        return true;
    }
}
