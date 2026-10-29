package com.aryston.kinetic.mixin;

import com.aryston.kinetic.client.chunk.CompactWireframe;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ChunkSectionsToRender.class)
abstract class ChunkSectionsToRenderCompactMixin {
    private static final String RENDER_LAYERS = "Lnet/minecraft/client/renderer/chunk/ChunkSectionsToRender;renderLayers([Lnet/minecraft/client/renderer/chunk/ChunkSectionLayer;Lcom/mojang/renderpearl/api/textures/GpuSampler;Lcom/mojang/renderpearl/api/commands/RenderPass;Lcom/mojang/renderpearl/api/textures/GpuTextureView;Lcom/mojang/renderpearl/api/textures/GpuTextureView;Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;)V";

    @ModifyArg(method = "renderGroup", at = @At(value = "INVOKE", target = RENDER_LAYERS), index = 5)
    private @Nullable RenderPipeline kinetic$compactWireframe(@Nullable RenderPipeline override, @Local(argsOnly = true) ChunkSectionLayerGroup group) {
        return CompactWireframe.direct(override, group);
    }

    @ModifyArg(method = "renderGroup", at = @At(value = "INVOKE", target = RENDER_LAYERS), index = 6)
    private @Nullable RenderPipeline kinetic$compactWireframeMultiDraw(@Nullable RenderPipeline override, @Local(argsOnly = true) ChunkSectionLayerGroup group) {
        return CompactWireframe.multiDraw(override, group);
    }
}
