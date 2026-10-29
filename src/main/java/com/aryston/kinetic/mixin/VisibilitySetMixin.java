package com.aryston.kinetic.mixin;

import com.aryston.kinetic.client.culling.OccluderHolder;
import com.aryston.kinetic.core.culling.OccluderBoxes;
import net.minecraft.client.renderer.chunk.VisibilitySet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(VisibilitySet.class)
abstract class VisibilitySetMixin implements OccluderHolder {
    @Unique
    private OccluderBoxes kinetic$occluders = OccluderBoxes.NONE;

    @Override
    public OccluderBoxes kinetic$occluders() {
        return kinetic$occluders;
    }

    @Override
    public void kinetic$setOccluders(OccluderBoxes occluders) {
        kinetic$occluders = occluders;
    }
}
