package com.aryston.aervium.mixin;

import com.aryston.aervium.client.gpu.GpuStageTimer;
import com.aryston.aervium.client.gpu.RenderPassTimingHolder;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.commands.RenderPassDescriptor;
import com.mojang.renderpearl.frontend.FrontendCommandEncoder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FrontendCommandEncoder.class)
abstract class FrontendCommandEncoderTimingMixin {
    @Unique
    private int aervium$openPassTiming = GpuStageTimer.NOT_MEASURED;

    @Inject(
        method = "createRenderPass(Lcom/mojang/renderpearl/api/commands/RenderPassDescriptor;)Lcom/mojang/renderpearl/api/commands/RenderPass;",
        at = @At("HEAD")
    )
    private void aervium$timePassStart(RenderPassDescriptor descriptor, CallbackInfoReturnable<RenderPass> callback) {
        aervium$openPassTiming = GpuStageTimer.enabled()
            ? GpuStageTimer.begin(GpuStageTimer.stage(GpuStageTimer.PASS_PREFIX + descriptor.label().get()), null)
            : GpuStageTimer.NOT_MEASURED;
    }

    @Inject(method = "submit", at = @At("HEAD"))
    private void aervium$timeFrameEnd(CallbackInfo callback) {
        GpuStageTimer.frameSubmitting();
    }

    @Inject(
        method = "createRenderPass(Lcom/mojang/renderpearl/api/commands/RenderPassDescriptor;)Lcom/mojang/renderpearl/api/commands/RenderPass;",
        at = @At("RETURN")
    )
    private void aervium$attachPassTiming(RenderPassDescriptor descriptor, CallbackInfoReturnable<RenderPass> callback) {
        if (callback.getReturnValue() instanceof RenderPassTimingHolder holder) {
            holder.aervium$setTiming(aervium$openPassTiming);
        }
        aervium$openPassTiming = GpuStageTimer.NOT_MEASURED;
    }
}
