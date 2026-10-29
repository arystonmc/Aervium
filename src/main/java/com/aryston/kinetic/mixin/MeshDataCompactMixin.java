package com.aryston.kinetic.mixin;

import com.aryston.kinetic.client.chunk.CompactMeshHolder;
import com.mojang.blaze3d.vertex.MeshData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(MeshData.class)
abstract class MeshDataCompactMixin implements CompactMeshHolder {
    @Unique
    private boolean kinetic$compacted;

    @Override
    public boolean kinetic$compacted() {
        return kinetic$compacted;
    }

    @Override
    public void kinetic$markCompacted() {
        kinetic$compacted = true;
    }
}
