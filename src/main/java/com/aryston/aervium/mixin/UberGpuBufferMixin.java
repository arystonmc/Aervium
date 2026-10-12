package com.aryston.aervium.mixin;

import com.aryston.aervium.client.chunk.SectionSliceHolder;
import com.mojang.blaze3d.vertex.TlsfAllocator;
import com.mojang.blaze3d.vertex.UberGpuBuffer;
import java.util.Map;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(UberGpuBuffer.class)
abstract class UberGpuBufferMixin<T> {
    @Shadow
    @Final
    private Map<T, TlsfAllocator.Allocation> allocationMap;

    @Inject(method = "freeAllocation", at = @At("HEAD"))
    private void aervium$forgetSlices(T allocationKey, CallbackInfo callback) {
        if (allocationKey instanceof SectionSliceHolder holder) {
            holder.aervium$clearSlices();
        }
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void aervium$forgetAllSlices(CallbackInfo callback) {
        for (T key : allocationMap.keySet()) {
            if (key instanceof SectionSliceHolder holder) {
                holder.aervium$clearSlices();
            }
        }
    }
}
