package com.aryston.kinetic.mixin;

import com.aryston.kinetic.client.gpu.GpuStageTimer;
import com.aryston.kinetic.client.gpu.RenderPassTimingHolder;
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
    private int kinetic$openPassTiming = GpuStageTimer.NOT_MEASURED;

    @Inject(
        method = "createRenderPass(Lcom/mojang/renderpearl/api/commands/RenderPassDescriptor;)Lcom/mojang/renderpearl/api/commands/RenderPass;",
        at = @At("HEAD")
    )
    private void kinetic$timePassStart(RenderPassDescriptor descriptor, CallbackInfoReturnable<RenderPass> callback) {
        kinetic$openPassTiming = GpuStageTimer.enabled()
            ? GpuStageTimer.begin(GpuStageTimer.stage(GpuStageTimer.PASS_PREFIX + descriptor.label().get()), null)
            : GpuStageTimer.NOT_MEASURED;
    }

    @Inject(method = "submit", at = @At("HEAD"))
    private void kinetic$timeFrameEnd(CallbackInfo callback) {
        GpuStageTimer.frameSubmitting();
    }

    @Inject(
        method = "createRenderPass(Lcom/mojang/renderpearl/api/commands/RenderPassDescriptor;)Lcom/mojang/renderpearl/api/commands/RenderPass;",
        at = @At("RETURN")
    )
    private void kinetic$attachPassTiming(RenderPassDescriptor descriptor, CallbackInfoReturnable<RenderPass> callback) {
        if (callback.getReturnValue() instanceof RenderPassTimingHolder holder) {
            holder.kinetic$setTiming(kinetic$openPassTiming);
        }
        kinetic$openPassTiming = GpuStageTimer.NOT_MEASURED;
    }
}
