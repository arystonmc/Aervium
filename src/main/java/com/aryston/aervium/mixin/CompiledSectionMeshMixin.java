package com.aryston.aervium.mixin;

import com.aryston.aervium.client.chunk.SectionLayers;
import com.aryston.aervium.client.chunk.SectionSliceHolder;
import net.minecraft.client.renderer.chunk.CompiledSectionMesh;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(CompiledSectionMesh.class)
abstract class CompiledSectionMeshMixin implements SectionSliceHolder {
    @Unique
    private Object @Nullable [] aervium$slices;

    @Override
    public @Nullable Object aervium$slice(int layer) {
        Object[] slices = aervium$slices;
        return slices == null ? null : slices[layer];
    }

    @Override
    public void aervium$setSlice(int layer, Object slice) {
        Object[] slices = aervium$slices;
        if (slices == null) {
            slices = new Object[SectionLayers.ALL.length];
            aervium$slices = slices;
        }
        slices[layer] = slice;
    }

    @Override
    public void aervium$clearSlices() {
        aervium$slices = null;
    }
}
