package com.aryston.aervium.client.culling;

import com.aryston.aervium.core.culling.OccluderBoxes;

public interface OccluderHolder {
    OccluderBoxes aervium$occluders();

    void aervium$setOccluders(OccluderBoxes occluders);
}
