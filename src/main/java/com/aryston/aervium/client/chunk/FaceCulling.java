package com.aryston.aervium.client.chunk;

import com.aryston.aervium.core.mesh.FaceRanges;
import com.aryston.aervium.core.mesh.FacingSort;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import java.util.Map;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;

public final class FaceCulling {
    private static @Nullable CameraRenderState cameraPass;

    private FaceCulling() {
    }

    public static FaceRanges @Nullable [] sortLayers(Map<ChunkSectionLayer, MeshData> layers) {
        FaceRanges[] ranges = null;
        for (Map.Entry<ChunkSectionLayer, MeshData> entry : layers.entrySet()) {
            ChunkSectionLayer layer = entry.getKey();
            MeshData mesh = entry.getValue();
            MeshData.DrawState state = mesh.drawState();
            if (layer.translucent() || mesh.indexBuffer() != null || state.format() != DefaultVertexFormat.BLOCK) {
                continue;
            }
            if (ranges == null) {
                ranges = new FaceRanges[SectionLayers.ALL.length];
            }
            ranges[layer.ordinal()] = FacingSort.sortQuads(mesh.vertexBuffer(), state.vertexCount(), state.format().getVertexSize());
        }
        return ranges;
    }

    public static void beginTerrainPass(Matrix4fc modelView, CameraRenderState camera) {
        boolean usable = camera.initialized && !camera.isPanoramicMode && !camera.isFrustumCaptured;
        cameraPass = usable && modelView.equals(camera.viewRotationMatrix) ? camera : null;
    }

    public static void endTerrainPass() {
        cameraPass = null;
    }

    static @Nullable CameraRenderState cameraPass() {
        return cameraPass;
    }
}
