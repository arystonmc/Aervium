package com.aryston.kinetic.client.model;

import com.aryston.kinetic.core.mesh.SharedCorners;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class CubeCorners {
    private static final int AXES = 3;
    private static final int MAX_CORNERS = 64;
    private static final ThreadLocal<float[]> TRANSFORMED = ThreadLocal.withInitial(() -> new float[MAX_CORNERS * AXES]);

    private CubeCorners() {
    }

    public static SharedCorners of(ModelPart.Polygon[] polygons) {
        int vertices = 0;
        for (ModelPart.Polygon polygon : polygons) {
            vertices += polygon.vertices().length;
        }
        float[] positions = new float[vertices * AXES];
        int offset = 0;
        for (ModelPart.Polygon polygon : polygons) {
            for (ModelPart.Vertex vertex : polygon.vertices()) {
                positions[offset++] = vertex.worldX();
                positions[offset++] = vertex.worldY();
                positions[offset++] = vertex.worldZ();
            }
        }
        return SharedCorners.of(positions);
    }

    public static void compile(
        ModelPart.Polygon[] polygons, SharedCorners corners, PoseStack.Pose pose, VertexConsumer builder, int lightCoords, int overlayCoords, int color
    ) {
        Matrix4f matrix = pose.pose();
        Vector3f scratch = new Vector3f();
        int count = corners.cornerCount();
        if (count > MAX_CORNERS) {
            compilePerVertex(polygons, pose, builder, lightCoords, overlayCoords, color);
            return;
        }
        float[] transformed = TRANSFORMED.get();
        for (int corner = 0; corner < count; corner++) {
            Vector3f position = matrix.transformPosition(corners.cornerX(corner), corners.cornerY(corner), corners.cornerZ(corner), scratch);
            transformed[corner * AXES] = position.x();
            transformed[corner * AXES + 1] = position.y();
            transformed[corner * AXES + 2] = position.z();
        }
        int vertexIndex = 0;
        for (ModelPart.Polygon polygon : polygons) {
            Vector3f normal = pose.transformNormal(polygon.normal(), scratch);
            float nx = normal.x();
            float ny = normal.y();
            float nz = normal.z();
            for (ModelPart.Vertex vertex : polygon.vertices()) {
                int base = corners.cornerOf(vertexIndex++) * AXES;
                builder.addVertex(transformed[base], transformed[base + 1], transformed[base + 2], color, vertex.u(), vertex.v(), overlayCoords, lightCoords, nx, ny, nz);
            }
        }
    }

    private static void compilePerVertex(
        ModelPart.Polygon[] polygons, PoseStack.Pose pose, VertexConsumer builder, int lightCoords, int overlayCoords, int color
    ) {
        Matrix4f matrix = pose.pose();
        Vector3f scratch = new Vector3f();
        for (ModelPart.Polygon polygon : polygons) {
            Vector3f normal = pose.transformNormal(polygon.normal(), scratch);
            float nx = normal.x();
            float ny = normal.y();
            float nz = normal.z();
            for (ModelPart.Vertex vertex : polygon.vertices()) {
                Vector3f position = matrix.transformPosition(vertex.worldX(), vertex.worldY(), vertex.worldZ(), scratch);
                builder.addVertex(position.x(), position.y(), position.z(), color, vertex.u(), vertex.v(), overlayCoords, lightCoords, nx, ny, nz);
            }
        }
    }
}
