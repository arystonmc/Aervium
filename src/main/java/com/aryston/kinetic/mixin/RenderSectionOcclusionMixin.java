package com.aryston.kinetic.mixin;

import com.aryston.kinetic.client.culling.CameraOcclusionHolder;
import com.aryston.kinetic.core.culling.OccluderBoxes;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(SectionRenderDispatcher.RenderSection.class)
abstract class RenderSectionOcclusionMixin implements CameraOcclusionHolder {
    @Unique
    private int kinetic$hiddenGeneration;

    @Unique
    private @Nullable OccluderBoxes kinetic$occluders;

    @Unique
    private int kinetic$occluderVersion;

    @Override
    public int kinetic$hiddenGeneration() {
        return kinetic$hiddenGeneration;
    }

    @Override
    public void kinetic$setHiddenGeneration(int generation) {
        kinetic$hiddenGeneration = generation;
    }

    @Override
    public @Nullable OccluderBoxes kinetic$occludersFor(int meshVersion) {
        return kinetic$occluderVersion == meshVersion ? kinetic$occluders : null;
    }

    @Override
    public void kinetic$cacheOccluders(OccluderBoxes occluders, int meshVersion) {
        kinetic$occluders = occluders;
        kinetic$occluderVersion = meshVersion;
    }
}
