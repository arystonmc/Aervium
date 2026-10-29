package com.aryston.kinetic.client.chunk;

import org.jspecify.annotations.Nullable;

public interface SectionDrawRecordHolder {
    @Nullable SectionDrawRecord kinetic$drawRecord();

    int @Nullable [] kinetic$packedDraws();

    void kinetic$setDrawRecord(SectionDrawRecord record);

    int kinetic$gpuVersion();

    int kinetic$gpuEpoch();

    int kinetic$gpuFlags();

    void kinetic$setGpuState(int meshVersion, int epoch, int flags);

    void kinetic$forgetGpuState();
}
