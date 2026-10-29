package com.aryston.kinetic.mixin;

import com.aryston.kinetic.client.gpu.GpuStageTimer;
import com.aryston.kinetic.client.gpu.RenderPassTimingHolder;
import com.mojang.renderpearl.frontend.FrontendRenderPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FrontendRenderPass.class)
abstract class FrontendRenderPassTimingMixin implements RenderPassTimingHolder {
    @Unique
    private int kinetic$timing = GpuStageTimer.NOT_MEASURED;

    @Override
    public void kinetic$setTiming(int pair) {
        kinetic$timing = pair;
    }

    @Inject(method = "close", at = @At("RETURN"))
    private void kinetic$timePassEnd(CallbackInfo callback) {
        int pair = kinetic$timing;
        kinetic$timing = GpuStageTimer.NOT_MEASURED;
        GpuStageTimer.end(pair, null);
    }
}
