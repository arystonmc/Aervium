package com.aryston.kinetic.core.mesh;

import java.util.Arrays;

public final class SharedCorners {
    private static final int AXES = 3;

    private final float[] corners;
    private final int[] cornerOfVertex;

    private SharedCorners(float[] corners, int[] cornerOfVertex) {
        this.corners = corners;
        this.cornerOfVertex = cornerOfVertex;
    }

    public static SharedCorners of(float[] vertexPositions) {
        int vertices = vertexPositions.length / AXES;
        float[] unique = new float[vertexPositions.length];
        int[] cornerOfVertex = new int[vertices];
        int count = 0;
        for (int vertex = 0; vertex < vertices; vertex++) {
            int found = find(unique, count, vertexPositions, vertex * AXES);
            if (found < 0) {
                System.arraycopy(vertexPositions, vertex * AXES, unique, count * AXES, AXES);
                found = count++;
            }
            cornerOfVertex[vertex] = found;
        }
        return new SharedCorners(Arrays.copyOf(unique, count * AXES), cornerOfVertex);
    }

    public int cornerCount() {
        return corners.length / AXES;
    }

    public float cornerX(int corner) {
        return corners[corner * AXES];
    }

    public float cornerY(int corner) {
        return corners[corner * AXES + 1];
    }

    public float cornerZ(int corner) {
        return corners[corner * AXES + 2];
    }

    public int cornerOf(int vertex) {
        return cornerOfVertex[vertex];
    }

    private static int find(float[] unique, int count, float[] positions, int offset) {
        for (int corner = 0; corner < count; corner++) {
            int base = corner * AXES;
            if (Float.floatToRawIntBits(unique[base]) == Float.floatToRawIntBits(positions[offset])
                && Float.floatToRawIntBits(unique[base + 1]) == Float.floatToRawIntBits(positions[offset + 1])
                && Float.floatToRawIntBits(unique[base + 2]) == Float.floatToRawIntBits(positions[offset + 2])) {
                return corner;
            }
        }
        return -1;
    }
}
