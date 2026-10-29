package com.aryston.kinetic.client.chunk;

import com.aryston.kinetic.core.mesh.FaceRanges;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.pipeline.IndexType;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.BlockPos;
import org.jspecify.annotations.Nullable;

public final class SectionDrawRecord {
    public static final int SLOTS = 3;
    static final int MESH_VERSION = 0;
    static final int VALID = 1;
    static final int ORIGIN_X = 2;
    static final int ORIGIN_Y = 3;
    static final int ORIGIN_Z = 4;
    static final int COUNT = 5;
    static final int HEADER = 6;
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
    private static final int TRUE = 1;
    private static final int FALSE = 0;
    private static final int HASH_SEED = 173;
    private static final int HASH_FACTOR = 31;
    private static final AtomicInteger MESH_VERSIONS = new AtomicInteger();

    final int[] packed;
    final LayerDraw[] draws;
    private @Nullable SectionDrawRecordHolder owner;

    private SectionDrawRecord(int meshVersion, BlockPos origin, LayerDraw[] draws, int count) {
        this.draws = draws;
        this.packed = new int[HEADER + count * STRIDE];
        packed[MESH_VERSION] = meshVersion;
        packed[VALID] = TRUE;
        packed[ORIGIN_X] = origin.getX();
        packed[ORIGIN_Y] = origin.getY();
        packed[ORIGIN_Z] = origin.getZ();
        packed[COUNT] = count;
        for (int slot = 0; slot < count; slot++) {
            pack(draws[slot], slotOffset(slot));
        }
    }

    static int slotOffset(int slot) {
        return HEADER + slot * STRIDE;
    }

    static boolean current(int[] packed, int meshVersion) {
        return packed[VALID] == TRUE && packed[MESH_VERSION] == meshVersion;
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
            FaceRanges faces = mesh instanceof FaceRangesHolder holder ? holder.kinetic$faceRanges(layer.ordinal()) : null;
            draws[count++] = new LayerDraw(
                layer, vertexBuffer, indexBuffer, indexType, groupHash, draw.indexCount(), firstIndex, baseVertex, draw.hasCustomIndexBuffer(), faces
            );
        }
        return new SectionDrawRecord(meshVersion, origin, draws, count);
    }

    public static int nextMeshVersion() {
        return MESH_VERSIONS.incrementAndGet();
    }

    public void attach(SectionDrawRecordHolder holder) {
        owner = holder;
    }

    public int[] packedDraws() {
        return packed;
    }

    public void invalidate() {
        packed[VALID] = FALSE;
        if (owner != null) {
            owner.kinetic$forgetGpuState();
        }
    }
}
