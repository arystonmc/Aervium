package com.aryston.aervium.client.chunk;

import com.aryston.aervium.core.mesh.FaceRanges;
import org.jspecify.annotations.Nullable;

public interface FaceRangesHolder {
    @Nullable FaceRanges aervium$faceRanges(int layer);
}
