package com.aryston.aervium.mixin;

import com.aryston.aervium.client.chunk.CompactMeshHolder;
import com.mojang.blaze3d.vertex.MeshData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(MeshData.class)
abstract class MeshDataCompactMixin implements CompactMeshHolder {
    @Unique
    private boolean aervium$compacted;

    @Override
    public boolean aervium$compacted() {
        return aervium$compacted;
    }

    @Override
    public void aervium$markCompacted() {
        aervium$compacted = true;
    }
}
