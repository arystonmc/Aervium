package com.aryston.aervium.mixin;

import com.aryston.aervium.client.culling.CameraOcclusionHolder;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(SectionRenderDispatcher.RenderSection.class)
abstract class RenderSectionOcclusionMixin implements CameraOcclusionHolder {
    @Unique
    private int aervium$hiddenGeneration;

    @Override
    public int aervium$hiddenGeneration() {
        return aervium$hiddenGeneration;
    }

    @Override
    public void aervium$setHiddenGeneration(int generation) {
        aervium$hiddenGeneration = generation;
    }
}
