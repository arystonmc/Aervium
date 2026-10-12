package com.aryston.aervium.mixin;

import com.aryston.aervium.client.chunk.SectionDrawLists;
import com.aryston.aervium.client.chunk.SectionLayers;
import com.aryston.aervium.client.chunk.SectionSliceHolder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.DynamicGpuData;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.OptionsRenderState;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.jspecify.annotations.Nullable;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelRenderer.class)
abstract class LevelRendererSectionSliceMixin {
    @Shadow
    @Final
    private OptionsRenderState optionsRenderState;

    @Shadow
    @Final
    private ObjectArrayList<SectionRenderDispatcher.RenderSection> visibleSections;

    @Shadow
    private @Nullable SectionRenderDispatcher sectionRenderDispatcher;

    @Inject(method = "extractSectionDrawGroups", at = @At("HEAD"), cancellable = true)
    private void aervium$extractFromRecords(
        boolean respectTranslucentOrder,
        List<DynamicGpuData.ChunkSectionInfo> sectionInfos,
        Map<ChunkSectionLayer, List<LevelRenderer.ChunkDrawGroup>> drawGroups,
        CallbackInfoReturnable<Integer> callback
    ) {
        SectionRenderDispatcher dispatcher = sectionRenderDispatcher;
        if (dispatcher == null || !SectionDrawLists.supported()) {
            return;
        }
        dispatcher.lock();
        try {
            callback.setReturnValue(SectionDrawLists.extract(
                visibleSections,
                dispatcher,
                Util.toMillis(optionsRenderState.chunkSectionFadeInTime),
                Util.getMillis(),
                respectTranslucentOrder,
                sectionInfos,
                drawGroups
            ));
        } finally {
            dispatcher.unlock();
        }
    }

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
