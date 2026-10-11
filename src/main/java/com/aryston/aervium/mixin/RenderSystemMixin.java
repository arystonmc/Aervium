package com.aryston.aervium.mixin;

import com.aryston.aervium.client.pipeline.PipelineWarmup;
import com.mojang.blaze3d.pipeline.PipelineCache;
import com.mojang.blaze3d.systems.RenderSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderSystem.class)
abstract class RenderSystemMixin {
    @Inject(method = "setCurrentPipelineCache", at = @At("RETURN"))
    private static void aervium$warmUpNewCache(PipelineCache pipelineCache, CallbackInfoReturnable<PipelineCache> callback) {
        PipelineWarmup.cacheInstalled(pipelineCache);
    }
}
