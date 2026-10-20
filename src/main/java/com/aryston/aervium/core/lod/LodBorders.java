package com.aryston.aervium.core.lod;

import java.util.Arrays;

public final class LodBorders {
    public static final int NORTH = 0;
    public static final int SOUTH = 1;
    public static final int WEST = 2;
    public static final int EAST = 3;
    public static final int SIDES = 4;
    public static final int UNKNOWN = Integer.MAX_VALUE;

    public static final LodBorders NONE = new LodBorders(empty());

    private final int[] tops;

    LodBorders(int[] tops) {
        this.tops = tops;
    }

    public int top(int side, int sample) {
        return tops[side * LodTile.SIZE + sample];
    }

    public boolean matches(LodBorders other) {
        return Arrays.equals(tops, other.tops);
    }

    private static int[] empty() {
        int[] tops = new int[SIDES * LodTile.SIZE];
        Arrays.fill(tops, UNKNOWN);
        return tops;
    }
}
