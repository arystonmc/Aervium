package com.aryston.kinetic.client.chunk;

import com.aryston.kinetic.core.mesh.FaceRanges;
import org.jspecify.annotations.Nullable;

public interface FaceRangesHolder {
    @Nullable FaceRanges kinetic$faceRanges(int layer);
}
