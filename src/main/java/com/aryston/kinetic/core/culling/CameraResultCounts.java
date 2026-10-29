package com.aryston.kinetic.core.culling;

public record CameraResultCounts(long frames, long widened, long none, long tooOld, long outOfReach) {
    public long missing() {
        return none + tooOld + outOfReach;
    }
}
