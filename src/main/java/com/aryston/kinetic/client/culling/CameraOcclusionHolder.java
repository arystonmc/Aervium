package com.aryston.kinetic.client.culling;

import com.aryston.kinetic.core.culling.OccluderBoxes;
import org.jspecify.annotations.Nullable;

public interface CameraOcclusionHolder {
    int kinetic$hiddenGeneration();

    void kinetic$setHiddenGeneration(int generation);

    @Nullable OccluderBoxes kinetic$occludersFor(int meshVersion);

    void kinetic$cacheOccluders(OccluderBoxes occluders, int meshVersion);
}
