package com.aryston.aervium.mixin;

import com.aryston.aervium.client.gpu.GpuStageTimer;
import com.aryston.aervium.client.gpu.RenderPassTimingHolder;
import com.mojang.renderpearl.frontend.FrontendRenderPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FrontendRenderPass.class)
abstract class FrontendRenderPassTimingMixin implements RenderPassTimingHolder {
    @Unique
    private int aervium$timing = GpuStageTimer.NOT_MEASURED;

    @Override
    public void aervium$setTiming(int pair) {
        aervium$timing = pair;
    }

    @Inject(method = "close", at = @At("RETURN"))
    private void aervium$timePassEnd(CallbackInfo callback) {
        int pair = aervium$timing;
        aervium$timing = GpuStageTimer.NOT_MEASURED;
        GpuStageTimer.end(pair, null);
    }
}
