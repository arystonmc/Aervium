package com.aryston.kinetic.client.chunk;

import org.jspecify.annotations.Nullable;

public interface SectionSliceHolder {
    Object ABSENT = new Object();

    @Nullable Object kinetic$slice(int layer);

    void kinetic$setSlice(int layer, Object slice);

    void kinetic$clearSlices();

    void kinetic$trackRecord(SectionDrawRecord record);
}
