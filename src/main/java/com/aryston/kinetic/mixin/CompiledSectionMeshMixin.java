package com.aryston.kinetic.mixin;

import com.aryston.kinetic.client.chunk.SectionDrawRecord;
import com.aryston.kinetic.client.chunk.SectionLayers;
import com.aryston.kinetic.client.chunk.SectionSliceHolder;
import net.minecraft.client.renderer.chunk.CompiledSectionMesh;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(CompiledSectionMesh.class)
abstract class CompiledSectionMeshMixin implements SectionSliceHolder {
    @Unique
    private static final int KINETIC$FIELD_SLOTS = 3;

    @Unique
    private @Nullable Object kinetic$slice0;

    @Unique
    private @Nullable Object kinetic$slice1;

    @Unique
    private @Nullable Object kinetic$slice2;

    @Unique
    private Object @Nullable [] kinetic$moreSlices;

    @Unique
    private @Nullable SectionDrawRecord kinetic$record;

    @Override
    public @Nullable Object kinetic$slice(int layer) {
        return switch (layer) {
            case 0 -> kinetic$slice0;
            case 1 -> kinetic$slice1;
            case 2 -> kinetic$slice2;
            default -> kinetic$moreSlices == null ? null : kinetic$moreSlices[layer - KINETIC$FIELD_SLOTS];
        };
    }

    @Override
    public void kinetic$setSlice(int layer, Object slice) {
        switch (layer) {
            case 0 -> kinetic$slice0 = slice;
            case 1 -> kinetic$slice1 = slice;
            case 2 -> kinetic$slice2 = slice;
            default -> {
                if (kinetic$moreSlices == null) {
                    kinetic$moreSlices = new Object[SectionLayers.ALL.length - KINETIC$FIELD_SLOTS];
                }
                kinetic$moreSlices[layer - KINETIC$FIELD_SLOTS] = slice;
            }
        }
    }

    @Override
    public void kinetic$clearSlices() {
        kinetic$slice0 = null;
        kinetic$slice1 = null;
        kinetic$slice2 = null;
        kinetic$moreSlices = null;
        SectionDrawRecord record = kinetic$record;
        if (record != null) {
            record.invalidate();
            kinetic$record = null;
        }
    }

    @Override
    public void kinetic$trackRecord(SectionDrawRecord record) {
        kinetic$record = record;
    }
}
