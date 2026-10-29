package com.aryston.kinetic.client.culling;

import com.aryston.kinetic.core.culling.OccluderBoxes;

public interface OccluderHolder {
    OccluderBoxes kinetic$occluders();

    void kinetic$setOccluders(OccluderBoxes occluders);
}
