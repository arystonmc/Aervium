package com.aryston.aervium.mixin;

import com.aryston.aervium.client.culling.CameraOcclusionHolder;
import com.aryston.aervium.core.culling.OccluderBoxes;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(SectionRenderDispatcher.RenderSection.class)
abstract class RenderSectionOcclusionMixin implements CameraOcclusionHolder {
    @Unique
    private int aervium$hiddenGeneration;

    @Unique
    private @Nullable OccluderBoxes aervium$occluders;

    @Unique
    private int aervium$occluderVersion;

    @Override
    public int aervium$hiddenGeneration() {
        return aervium$hiddenGeneration;
    }

    @Override
    public void aervium$setHiddenGeneration(int generation) {
        aervium$hiddenGeneration = generation;
    }

    @Override
    public @Nullable OccluderBoxes aervium$occludersFor(int meshVersion) {
        return aervium$occluderVersion == meshVersion ? aervium$occluders : null;
    }

    @Override
    public void aervium$cacheOccluders(OccluderBoxes occluders, int meshVersion) {
        aervium$occluders = occluders;
        aervium$occluderVersion = meshVersion;
    }
}
