package com.aryston.aervium.client.chunk;

import org.jspecify.annotations.Nullable;

public interface SectionDrawRecordHolder {
    @Nullable SectionDrawRecord aervium$drawRecord();

    int @Nullable [] aervium$packedDraws();

    void aervium$setDrawRecord(SectionDrawRecord record);

    int aervium$gpuVersion();

    int aervium$gpuEpoch();

    int aervium$gpuFlags();

    void aervium$setGpuState(int meshVersion, int epoch, int flags);

    void aervium$forgetGpuState();
}
