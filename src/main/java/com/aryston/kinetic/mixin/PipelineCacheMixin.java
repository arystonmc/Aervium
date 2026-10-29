package com.aryston.kinetic.mixin;

import com.aryston.kinetic.client.pipeline.ShaderCaches;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.blaze3d.pipeline.PipelineCache;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import java.util.Map;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(PipelineCache.class)
abstract class PipelineCacheMixin {
    @Shadow
    @Final
    private Map<RenderPipeline, CompiledRenderPipeline> cache;

    @WrapMethod(method = "get")
    private CompiledRenderPipeline kinetic$timeLateCompile(RenderPipeline pipeline, Operation<CompiledRenderPipeline> original) {
        if (cache.containsKey(pipeline)) {
            return original.call(pipeline);
        }
        long start = System.nanoTime();
        CompiledRenderPipeline compiled = original.call(pipeline);
        ShaderCaches.recordLateCompile(pipeline.getLocation(), System.nanoTime() - start);
        return compiled;
    }
}
