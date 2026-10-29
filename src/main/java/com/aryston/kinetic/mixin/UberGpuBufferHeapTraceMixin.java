package com.aryston.kinetic.mixin;

import com.aryston.kinetic.client.FrameTrace;
import com.mojang.blaze3d.vertex.UberGpuBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(UberGpuBuffer.class)
abstract class UberGpuBufferHeapTraceMixin {
    @Inject(method = "uploadStagedAllocations", at = @At(value = "NEW", target = "com/mojang/blaze3d/vertex/UberGpuBuffer$UberGpuBufferHeap"))
    private void kinetic$traceHeapCreated(CallbackInfoReturnable<Boolean> callback) {
        FrameTrace.bufferHeapCreated();
    }

    @Inject(method = "uploadStagedAllocations", at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/api/buffers/GpuBuffer;close()V"))
    private void kinetic$traceHeapDestroyed(CallbackInfoReturnable<Boolean> callback) {
        FrameTrace.bufferHeapDestroyed();
    }
}
