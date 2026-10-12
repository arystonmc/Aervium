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
    private static final int AERVIUM$FIELD_SLOTS = 3;

    @Unique
    private @Nullable Object aervium$slice0;

    @Unique
    private @Nullable Object aervium$slice1;

    @Unique
    private @Nullable Object aervium$slice2;

    @Unique
    private Object @Nullable [] aervium$moreSlices;

    @Override
    public @Nullable Object aervium$slice(int layer) {
        return switch (layer) {
            case 0 -> aervium$slice0;
            case 1 -> aervium$slice1;
            case 2 -> aervium$slice2;
            default -> aervium$moreSlices == null ? null : aervium$moreSlices[layer - AERVIUM$FIELD_SLOTS];
        };
    }

    @Override
    public void aervium$setSlice(int layer, Object slice) {
        switch (layer) {
            case 0 -> aervium$slice0 = slice;
            case 1 -> aervium$slice1 = slice;
            case 2 -> aervium$slice2 = slice;
            default -> {
                if (aervium$moreSlices == null) {
                    aervium$moreSlices = new Object[SectionLayers.ALL.length - AERVIUM$FIELD_SLOTS];
                }
                aervium$moreSlices[layer - AERVIUM$FIELD_SLOTS] = slice;
            }
        }
    }

    @Override
    public void aervium$clearSlices() {
        aervium$slice0 = null;
        aervium$slice1 = null;
        aervium$slice2 = null;
        aervium$moreSlices = null;
    }
}
