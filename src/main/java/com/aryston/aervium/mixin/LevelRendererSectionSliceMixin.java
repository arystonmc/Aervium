package com.aryston.aervium.mixin;

import com.aryston.aervium.client.chunk.SectionLayers;
import com.aryston.aervium.client.chunk.SectionSliceHolder;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LevelRenderer.class)
abstract class LevelRendererSectionSliceMixin {
    @Redirect(
        method = "extractSectionDrawGroups",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/ChunkSectionLayer;values()[Lnet/minecraft/client/renderer/chunk/ChunkSectionLayer;")
    )
    private ChunkSectionLayer[] aervium$sharedLayers() {
        return SectionLayers.ALL;
    }

    @Redirect(
        method = "extractSectionDrawGroups",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher;getRenderSectionSlice(Lnet/minecraft/client/renderer/chunk/SectionMesh;Lnet/minecraft/client/renderer/chunk/ChunkSectionLayer;)Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher$RenderSectionBufferSlice;"
        )
    )
    private SectionRenderDispatcher.@Nullable RenderSectionBufferSlice aervium$cachedSlice(
        SectionRenderDispatcher dispatcher,
        SectionMesh mesh,
        ChunkSectionLayer layer
    ) {
        if (!(mesh instanceof SectionSliceHolder holder)) {
            return dispatcher.getRenderSectionSlice(mesh, layer);
        }
        Object cached = holder.aervium$slice(layer.ordinal());
        if (cached != null) {
            return cached == SectionSliceHolder.ABSENT ? null : (SectionRenderDispatcher.RenderSectionBufferSlice) cached;
        }
        SectionRenderDispatcher.RenderSectionBufferSlice slice = dispatcher.getRenderSectionSlice(mesh, layer);
        holder.aervium$setSlice(layer.ordinal(), slice == null ? SectionSliceHolder.ABSENT : slice);
        return slice;
    }
}
