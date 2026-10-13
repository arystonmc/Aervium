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
    private static final double EYE_MARGIN = 0.5;

    private final int[] firstQuad;
    private final float[] planes;

    FaceRanges(int[] firstQuad, float[] planes) {
        this.firstQuad = firstQuad;
        this.planes = planes;
    }

    public int firstQuad(int bucket) {
        return firstQuad[bucket];
    }

    public int quadCount(int bucket) {
        return firstQuad[bucket + 1] - firstQuad[bucket];
    }

    public int visibleBuckets(double x, double y, double z) {
        int mask = 1 << UNASSIGNED;
        mask |= x > planes[EAST] - EYE_MARGIN ? 1 << EAST : 0;
        mask |= x < planes[WEST] + EYE_MARGIN ? 1 << WEST : 0;
        mask |= y > planes[UP] - EYE_MARGIN ? 1 << UP : 0;
        mask |= y < planes[DOWN] + EYE_MARGIN ? 1 << DOWN : 0;
        mask |= z > planes[SOUTH] - EYE_MARGIN ? 1 << SOUTH : 0;
        mask |= z < planes[NORTH] + EYE_MARGIN ? 1 << NORTH : 0;
        return mask;
    }
}
