package com.aryston.aervium.client.chunk;

import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.pipeline.IndexType;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import org.jspecify.annotations.Nullable;

record LayerDraw(
    ChunkSectionLayer layer,
    GpuBuffer vertexBuffer,
    @Nullable GpuBuffer indexBuffer,
    @Nullable IndexType indexType,
    int groupHash,
    int indexCount,
    int firstIndex,
    int baseVertex,
    boolean customIndexBuffer
) {
}
