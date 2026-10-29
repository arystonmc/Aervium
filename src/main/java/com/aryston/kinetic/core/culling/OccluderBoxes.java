package com.aryston.kinetic.core.culling;

public final class OccluderBoxes {
    public static final OccluderBoxes NONE = new OccluderBoxes(new int[0]);
    private static final int BITS = 5;
    private static final int MASK = (1 << BITS) - 1;

    private final int[] boxes;

    OccluderBoxes(int[] boxes) {
        this.boxes = boxes;
    }

    static int pack(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        return minX | minY << BITS | minZ << 2 * BITS | maxX << 3 * BITS | maxY << 4 * BITS | maxZ << 5 * BITS;
    }

    public int count() {
        return boxes.length;
    }

    public boolean isEmpty() {
        return boxes.length == 0;
    }

    public int minX(int box) {
        return boxes[box] & MASK;
    }

    public int minY(int box) {
        return boxes[box] >>> BITS & MASK;
    }

    public int minZ(int box) {
        return boxes[box] >>> 2 * BITS & MASK;
    }

    public int maxX(int box) {
        return boxes[box] >>> 3 * BITS & MASK;
    }

    public int maxY(int box) {
        return boxes[box] >>> 4 * BITS & MASK;
    }

    public int maxZ(int box) {
        return boxes[box] >>> 5 * BITS & MASK;
    }

    public int volume(int box) {
        return (maxX(box) - minX(box)) * (maxY(box) - minY(box)) * (maxZ(box) - minZ(box));
    }
}
