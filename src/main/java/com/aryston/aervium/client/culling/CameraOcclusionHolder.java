package com.aryston.aervium.client.culling;

import com.aryston.aervium.core.culling.OccluderBoxes;
import org.jspecify.annotations.Nullable;

public interface CameraOcclusionHolder {
    int aervium$hiddenGeneration();

    void aervium$setHiddenGeneration(int generation);

    @Nullable OccluderBoxes aervium$occludersFor(int meshVersion);

    void aervium$cacheOccluders(OccluderBoxes occluders, int meshVersion);
}
