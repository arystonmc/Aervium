package com.aryston.aervium.client.chunk;

import org.jspecify.annotations.Nullable;

public interface SectionDrawRecordHolder {
    @Nullable SectionDrawRecord aervium$drawRecord();

    void aervium$setDrawRecord(SectionDrawRecord record);

    int aervium$meshVersion();
}
