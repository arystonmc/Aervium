package com.aryston.aervium.core.mesh;

public final class FaceRanges {
    public static final int UNASSIGNED = 0;
    public static final int EAST = 1;
    public static final int WEST = 2;
    public static final int UP = 3;
    public static final int DOWN = 4;
    public static final int SOUTH = 5;
    public static final int NORTH = 6;
    public static final int BUCKETS = 7;
    public static final int ALL = (1 << BUCKETS) - 1;
    private static final int QUAD_OFFSETS = BUCKETS + 1;
    public static final int PACKED_INTS = QUAD_OFFSETS + BUCKETS;
    private static final double EYE_MARGIN = 0.5;

    private final int[] packed = new int[PACKED_INTS];

    FaceRanges(int[] firstQuad, float[] planes) {
        System.arraycopy(firstQuad, 0, packed, 0, QUAD_OFFSETS);
        for (int bucket = 0; bucket < BUCKETS; bucket++) {
            packed[QUAD_OFFSETS + bucket] = Float.floatToRawIntBits(planes[bucket]);
        }
    }

    public void pack(int[] target, int offset) {
        System.arraycopy(packed, 0, target, offset, PACKED_INTS);
    }

    public int firstQuad(int bucket) {
        return firstQuad(packed, 0, bucket);
    }

    public int quadCount(int bucket) {
        return quadCount(packed, 0, bucket);
    }

    public int visibleBuckets(double x, double y, double z) {
        return visibleBuckets(packed, 0, x, y, z);
    }

    public static int firstQuad(int[] packed, int offset, int bucket) {
        return packed[offset + bucket];
    }

    public static int quadCount(int[] packed, int offset, int bucket) {
        return packed[offset + bucket + 1] - packed[offset + bucket];
    }

    public static int visibleBuckets(int[] packed, int offset, double x, double y, double z) {
        int planes = offset + QUAD_OFFSETS;
        int mask = 1 << UNASSIGNED;
        mask |= x > plane(packed, planes, EAST) - EYE_MARGIN ? 1 << EAST : 0;
        mask |= x < plane(packed, planes, WEST) + EYE_MARGIN ? 1 << WEST : 0;
        mask |= y > plane(packed, planes, UP) - EYE_MARGIN ? 1 << UP : 0;
        mask |= y < plane(packed, planes, DOWN) + EYE_MARGIN ? 1 << DOWN : 0;
        mask |= z > plane(packed, planes, SOUTH) - EYE_MARGIN ? 1 << SOUTH : 0;
        mask |= z < plane(packed, planes, NORTH) + EYE_MARGIN ? 1 << NORTH : 0;
        return mask;
    }

    private static float plane(int[] packed, int planes, int bucket) {
        return Float.intBitsToFloat(packed[planes + bucket]);
    }
}
