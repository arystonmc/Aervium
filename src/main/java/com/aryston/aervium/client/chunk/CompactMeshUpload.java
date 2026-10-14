package com.aryston.aervium.client.chunk;

import com.aryston.aervium.Aervium;
import com.aryston.aervium.core.mesh.CompactVertexEncoder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;

public final class CompactMeshUpload {
    private static final AtomicBoolean CLAMP_REPORTED = new AtomicBoolean();

    private CompactMeshUpload() {
    }

    public static VertexFormat buildFormat(VertexFormat layerFormat) {
        return layerFormat == CompactTerrainFormat.FORMAT ? DefaultVertexFormat.BLOCK : layerFormat;
    }

    public static ByteBuffer uploadView(ChunkSectionLayer layer, MeshData mesh, ByteBuffer vertices) {
        if (layer.vertexFormat() != CompactTerrainFormat.FORMAT) {
            return vertices;
        }
        int vertexCount = mesh.drawState().vertexCount();
        CompactMeshHolder holder = (CompactMeshHolder) mesh;
        if (!holder.aervium$compacted()) {
            int clamped = CompactVertexEncoder.encodeInPlace(vertices, vertexCount);
            holder.aervium$markCompacted();
            if (clamped > 0 && CLAMP_REPORTED.compareAndSet(false, true)) {
                Aervium.LOGGER.warn("Aervium compactVertices: a {} mesh had {} vertices outside the compact range; they were moved to its edge", layer.label(), clamped);
            }
        }
        return vertices.slice(vertices.position(), CompactVertexEncoder.compactBytes(vertexCount));
    }
}
