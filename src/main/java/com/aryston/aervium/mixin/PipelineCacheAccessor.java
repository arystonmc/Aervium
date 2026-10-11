package com.aryston.aervium.mixin;

import com.mojang.blaze3d.pipeline.PipelineCache;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.ShaderSource;
import java.util.Map;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PipelineCache.class)
public interface PipelineCacheAccessor {
    @Accessor("device")
    GpuDevice aervium$device();

    @Accessor("shaderSource")
    ShaderSource aervium$shaderSource();

    @Accessor("cache")
    Map<RenderPipeline, CompiledRenderPipeline> aervium$compiled();
}
