package com.aryston.aervium.mixin;

import com.aryston.aervium.client.gpu.GpuStageTimer;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FrameGraphBuilder.class)
abstract class FrameGraphBuilderTimingMixin {
    @Unique
    private static int aervium$openPass = GpuStageTimer.NOT_MEASURED;

    @WrapOperation(
        method = "execute(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder$Inspector;)V",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder$Inspector;beforeExecutePass(Ljava/lang/String;)V")
    )
    private void aervium$timePassStart(FrameGraphBuilder.Inspector inspector, String name, Operation<Void> original) {
        original.call(inspector, name);
        if (GpuStageTimer.enabled()) {
            aervium$openPass = GpuStageTimer.begin(GpuStageTimer.stage(name), null);
        }
    }

    @WrapOperation(
        method = "execute(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder$Inspector;)V",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder$Inspector;afterExecutePass(Ljava/lang/String;)V")
    )
    private void aervium$timePassEnd(FrameGraphBuilder.Inspector inspector, String name, Operation<Void> original) {
        GpuStageTimer.end(aervium$openPass, null);
        aervium$openPass = GpuStageTimer.NOT_MEASURED;
        original.call(inspector, name);
    }
}
