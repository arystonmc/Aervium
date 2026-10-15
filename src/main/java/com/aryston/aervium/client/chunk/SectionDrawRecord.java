package com.aryston.aervium.client.chunk;

import com.aryston.aervium.core.mesh.FaceRanges;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.pipeline.IndexType;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.BlockPos;

public final class SectionDrawRecord {
    public static final int SLOTS = 3;
    static final int LAYER = 0;
    static final int GROUP_HASH = 1;
    static final int INDEX_COUNT = 2;
    static final int FIRST_INDEX = 3;
    static final int BASE_VERTEX = 4;
    static final int FLAGS = 5;
    static final int FACES = 6;
    static final int STRIDE = FACES + FaceRanges.PACKED_INTS;
    static final int CUSTOM_INDEX_BUFFER = 1;
    static final int HAS_FACES = 2;
    private static final int HASH_SEED = 173;
    private static final int HASH_FACTOR = 31;
    private static final AtomicInteger MESH_VERSIONS = new AtomicInteger();

    final int meshVersion;
    final int originX;
    final int originY;
    final int originZ;
    final int count;
    final int[] packed;
    final LayerDraw[] draws;
    private boolean valid = true;

    private SectionDrawRecord(int meshVersion, BlockPos origin, LayerDraw[] draws, int count) {
        this.meshVersion = meshVersion;
        this.originX = origin.getX();
        this.originY = origin.getY();
        this.originZ = origin.getZ();
        this.count = count;
        this.draws = draws;
        this.packed = new int[count * STRIDE];
        for (int slot = 0; slot < count; slot++) {
            pack(draws[slot], slot * STRIDE);
        }
    }

    private void pack(LayerDraw draw, int offset) {
        packed[offset + LAYER] = draw.layer().ordinal();
        packed[offset + GROUP_HASH] = draw.groupHash();
        packed[offset + INDEX_COUNT] = draw.indexCount();
        packed[offset + FIRST_INDEX] = draw.firstIndex();
        packed[offset + BASE_VERTEX] = draw.baseVertex();
        int flags = draw.customIndexBuffer() ? CUSTOM_INDEX_BUFFER : 0;
        if (draw.faces() != null) {
            flags |= HAS_FACES;
            draw.faces().pack(packed, offset + FACES);
        }
        packed[offset + FLAGS] = flags;
    }

    static SectionDrawRecord build(SectionMesh mesh, int meshVersion, BlockPos origin, SectionRenderDispatcher dispatcher) {
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
        return new SectionDrawRecord(meshVersion, origin, draws, count);
    }

    public static int nextMeshVersion() {
        return MESH_VERSIONS.incrementAndGet();
    }

    public boolean isFor(int currentMeshVersion) {
        return valid && meshVersion == currentMeshVersion;
    }

    public void invalidate() {
        valid = false;
    }
}
