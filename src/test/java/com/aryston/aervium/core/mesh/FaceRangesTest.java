package com.aryston.aervium.core.mesh;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FaceRangesTest {
    private static final int OFFSET = 5;
    private static final int[] FIRST_QUAD = {0, 3, 7, 7, 12, 20, 21, 30};
    private static final float[] PLANES = {0.0F, 16.0F, 0.0F, 9.5F, 2.0F, 14.0F, 1.0F};
    private static final double[] EYES = {-20.0, -0.4, 0.6, 1.5, 8.0, 9.2, 14.6, 15.8, 16.4, 40.0};

    @Test
    void packedRangesMatchTheObject() {
        FaceRanges faces = new FaceRanges(FIRST_QUAD, PLANES);
        int[] packed = new int[OFFSET + FaceRanges.PACKED_INTS];
        faces.pack(packed, OFFSET);
        for (int bucket = 0; bucket < FaceRanges.BUCKETS; bucket++) {
            assertEquals(faces.firstQuad(bucket), FaceRanges.firstQuad(packed, OFFSET, bucket));
            assertEquals(faces.quadCount(bucket), FaceRanges.quadCount(packed, OFFSET, bucket));
        }
        assertEquals(faces.firstQuad(FaceRanges.BUCKETS), FaceRanges.firstQuad(packed, OFFSET, FaceRanges.BUCKETS));
        for (double x : EYES) {
            for (double y : EYES) {
                for (double z : EYES) {
                    assertEquals(faces.visibleBuckets(x, y, z), FaceRanges.visibleBuckets(packed, OFFSET, x, y, z));
                }
            }
        }
    }

    @Test
    void eachEyeSideShowsOnlyTheFacesTurnedToIt() {
        FaceRanges faces = new FaceRanges(FIRST_QUAD, PLANES);
        int east = 1 << FaceRanges.EAST;
        int west = 1 << FaceRanges.WEST;
        assertEquals(east, faces.visibleBuckets(20.0, 5.0, 8.0) & (east | west));
        assertEquals(west, faces.visibleBuckets(-4.0, 5.0, 8.0) & (east | west));
        assertEquals(1 << FaceRanges.UNASSIGNED, faces.visibleBuckets(8.0, 5.0, 8.0) & 1 << FaceRanges.UNASSIGNED);
    }
}
