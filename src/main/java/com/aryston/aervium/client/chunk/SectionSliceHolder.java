package com.aryston.aervium.client.chunk;

import org.jspecify.annotations.Nullable;

public interface SectionSliceHolder {
    Object ABSENT = new Object();

    @Nullable Object aervium$slice(int layer);

    void aervium$setSlice(int layer, Object slice);

    void aervium$clearSlices();
}
