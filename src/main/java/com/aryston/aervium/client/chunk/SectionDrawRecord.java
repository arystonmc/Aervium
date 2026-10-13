package com.aryston.aervium.client.chunk;

import com.aryston.aervium.core.mesh.FaceRanges;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.pipeline.IndexType;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.jspecify.annotations.Nullable;

public final class SectionDrawRecord {
    public static final int SLOTS = 3;
    private static final int HASH_SEED = 173;
    private static final int HASH_FACTOR = 31;

    final SectionMesh mesh;
    final int count;
    final @Nullable LayerDraw slot0;
    final @Nullable LayerDraw slot1;
    final @Nullable LayerDraw slot2;
    private boolean valid = true;

    private SectionDrawRecord(SectionMesh mesh, LayerDraw[] draws, int count) {
        this.mesh = mesh;
        this.count = count;
        this.slot0 = draws[0];
        this.slot1 = draws[1];
        this.slot2 = draws[2];
    }

    static SectionDrawRecord build(SectionMesh mesh, SectionRenderDispatcher dispatcher) {
        LayerDraw[] draws = new LayerDraw[SLOTS];
        int count = 0;
        for (ChunkSectionLayer layer : SectionLayers.ALL) {
            SectionMesh.SectionDraw draw = mesh.getSectionDraw(layer);
            SectionRenderDispatcher.RenderSectionBufferSlice slice = dispatcher.getRenderSectionSlice(mesh, layer);
            if (slice == null || draw == null || (draw.hasCustomIndexBuffer() && slice.indexBuffer() == null)) {
                continue;
            }
            VertexFormat vertexFormat = layer.pipeline(false).getVertexFormatBinding(0);
            GpuBuffer vertexBuffer = slice.vertexBuffer();
            int groupHash = HASH_FACTOR * HASH_SEED + vertexBuffer.hashCode();
            GpuBuffer indexBuffer = null;
            IndexType indexType = null;
            int firstIndex = 0;
            if (draw.hasCustomIndexBuffer()) {
                indexBuffer = slice.indexBuffer();
                indexType = draw.indexType();
                groupHash = HASH_FACTOR * groupHash + indexBuffer.hashCode();
                groupHash = HASH_FACTOR * groupHash + indexType.hashCode();
                firstIndex = (int) (slice.indexBufferOffset() / indexType.bytes);
            }
            int baseVertex = (int) (slice.vertexBufferOffset() / vertexFormat.getVertexSize());
            FaceRanges faces = mesh instanceof FaceRangesHolder holder ? holder.aervium$faceRanges(layer.ordinal()) : null;
            draws[count++] = new LayerDraw(
                layer, vertexBuffer, indexBuffer, indexType, groupHash, draw.indexCount(), firstIndex, baseVertex, draw.hasCustomIndexBuffer(), faces
            );
        }
        return new SectionDrawRecord(mesh, draws, count);
    }

    public boolean isFor(SectionMesh current) {
        return valid && mesh == current;
    }

    public void invalidate() {
        valid = false;
    }

    @Nullable LayerDraw slot(int index) {
        return switch (index) {
            case 0 -> slot0;
            case 1 -> slot1;
            default -> slot2;
        };
    }
}
