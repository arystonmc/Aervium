package com.aryston.kinetic.core.mesh;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.junit.jupiter.api.Test;

class FacingSortTest {
    private static final int STRIDE = 28;
    private static final int QUAD_BYTES = STRIDE * 4;

    @Test
    void groupsQuadsByFacingAndKeepsTheirBytes() {
        ByteBuffer vertices = ByteBuffer.allocateDirect(QUAD_BYTES * 4).order(ByteOrder.nativeOrder());
        putQuad(vertices, 0, new float[][] {{3, 0, 0}, {3, 1, 0}, {3, 1, 1}, {3, 0, 1}}, 11);
        putQuad(vertices, 1, new float[][] {{0, 5, 0}, {0, 5, 1}, {1, 5, 1}, {1, 5, 0}}, 22);
        putQuad(vertices, 2, new float[][] {{0, 0, 0}, {1, 1, 0}, {1, 1, 1}, {0, 0, 1}}, 33);
        putQuad(vertices, 3, new float[][] {{7, 0, 0}, {7, 0, 1}, {7, 1, 1}, {7, 1, 0}}, 44);
        FaceRanges ranges = FacingSort.sortQuads(vertices, 16, STRIDE);
        assertEquals(1, ranges.quadCount(FaceRanges.UNASSIGNED));
        assertEquals(1, ranges.quadCount(FaceRanges.EAST));
        assertEquals(1, ranges.quadCount(FaceRanges.WEST));
        assertEquals(1, ranges.quadCount(FaceRanges.UP));
        assertEquals(33, tag(vertices, 0));
        int total = 0;
        for (int bucket = 0; bucket < FaceRanges.BUCKETS; bucket++) {
            total += ranges.quadCount(bucket);
        }
        assertEquals(4, total);
        int sum = 0;
        for (int quad = 0; quad < 4; quad++) {
            sum += tag(vertices, quad);
        }
        assertEquals(110, sum);
    }

    @Test
    void facesAwayFromTheEyeAreSkipped() {
        ByteBuffer vertices = ByteBuffer.allocateDirect(QUAD_BYTES * 2).order(ByteOrder.nativeOrder());
        putQuad(vertices, 0, new float[][] {{3, 0, 0}, {3, 1, 0}, {3, 1, 1}, {3, 0, 1}}, 1);
        putQuad(vertices, 1, new float[][] {{7, 0, 0}, {7, 0, 1}, {7, 1, 1}, {7, 1, 0}}, 2);
        FaceRanges ranges = FacingSort.sortQuads(vertices, 8, STRIDE);
        assertEquals(1, ranges.quadCount(FaceRanges.EAST));
        assertEquals(1, ranges.quadCount(FaceRanges.WEST));
        int far = ranges.visibleBuckets(100.0, 0.5, 0.5);
        int near = ranges.visibleBuckets(-100.0, 0.5, 0.5);
        int between = ranges.visibleBuckets(5.0, 0.5, 0.5);
        assertTrue((far >> FaceRanges.EAST & 1) == 1 && (far >> FaceRanges.WEST & 1) == 0);
        assertTrue((near >> FaceRanges.WEST & 1) == 1 && (near >> FaceRanges.EAST & 1) == 0);
        assertEquals(1 << FaceRanges.UNASSIGNED | 1 << FaceRanges.EAST | 1 << FaceRanges.WEST, between);
        assertEquals(0, far >> FaceRanges.UP & 1);
        assertEquals(0, far >> FaceRanges.DOWN & 1);
    }

    @Test
    void windingDecidesTheFacing() {
        ByteBuffer vertices = ByteBuffer.allocateDirect(QUAD_BYTES).order(ByteOrder.nativeOrder());
        putQuad(vertices, 0, new float[][] {{0, 1, 0}, {0, 1, 1}, {1, 1, 1}, {1, 1, 0}}, 1);
        FaceRanges ranges = FacingSort.sortQuads(vertices, 4, STRIDE);
        assertEquals(1, ranges.quadCount(FaceRanges.UP));
        assertEquals(1 << FaceRanges.UNASSIGNED | 1 << FaceRanges.UP, ranges.visibleBuckets(0.5, 2.0, 0.5));
        assertEquals(1 << FaceRanges.UNASSIGNED | 1 << FaceRanges.UP, ranges.visibleBuckets(0.5, 0.75, 0.5));
        assertEquals(1 << FaceRanges.UNASSIGNED, ranges.visibleBuckets(0.5, 0.25, 0.5));
    }

    private static void putQuad(ByteBuffer buffer, int quad, float[][] corners, int tag) {
        for (int vertex = 0; vertex < 4; vertex++) {
            int at = quad * QUAD_BYTES + vertex * STRIDE;
            buffer.putFloat(at, corners[vertex][0]);
            buffer.putFloat(at + 4, corners[vertex][1]);
            buffer.putFloat(at + 8, corners[vertex][2]);
            buffer.putInt(at + 12, tag);
        }
    }

    private static int tag(ByteBuffer buffer, int quad) {
        return buffer.getInt(quad * QUAD_BYTES + 12);
    }
}
