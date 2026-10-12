package com.aryston.aervium.mixin;

import com.aryston.aervium.client.culling.OccluderHolder;
import com.aryston.aervium.core.culling.OccluderBoxes;
import net.minecraft.client.renderer.chunk.VisibilitySet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(VisibilitySet.class)
abstract class VisibilitySetMixin implements OccluderHolder {
    @Unique
    private OccluderBoxes aervium$occluders = OccluderBoxes.NONE;

    @Override
    public OccluderBoxes aervium$occluders() {
        return aervium$occluders;
    }

    @Override
    public void aervium$setOccluders(OccluderBoxes occluders) {
        aervium$occluders = occluders;
    }
}
