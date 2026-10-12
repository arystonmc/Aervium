package com.aryston.aervium.core.culling;

import java.util.Arrays;

public final class OpaqueMask {
    public static final int SIZE = 16;
    public static final int CELL = 2;
    public static final int MAX_BOXES = 24;
    private static final int CELLS = SIZE / CELL;

    private final long[] bits = new long[SIZE * SIZE * SIZE / Long.SIZE];
    private int opaque;

    public static OpaqueMask fromBits(long[] words) {
        OpaqueMask mask = new OpaqueMask();
        int length = Math.min(words.length, mask.bits.length);
        for (int index = 0; index < length; index++) {
            mask.bits[index] = words[index];
            mask.opaque += Long.bitCount(words[index]);
        }
        return mask;
    }

    public void set(int x, int y, int z) {
        int index = (y & 15) << 8 | (z & 15) << 4 | (x & 15);
        long bit = 1L << (index & 63);
        if ((bits[index >>> 6] & bit) == 0) {
            bits[index >>> 6] |= bit;
            opaque++;
        }
    }

    public boolean get(int x, int y, int z) {
        int index = y << 8 | z << 4 | x;
        return (bits[index >>> 6] & 1L << (index & 63)) != 0;
    }

    public int opaqueBlocks() {
        return opaque;
    }

    public OccluderBoxes boxes() {
        if (opaque < CELL * CELL * CELL) {
            return OccluderBoxes.NONE;
        }
        if (opaque == SIZE * SIZE * SIZE) {
            return new OccluderBoxes(new int[] {OccluderBoxes.pack(0, 0, 0, SIZE, SIZE, SIZE)});
        }
        boolean[] free = new boolean[CELLS * CELLS * CELLS];
        for (int y = 0; y < CELLS; y++) {
            for (int z = 0; z < CELLS; z++) {
                for (int x = 0; x < CELLS; x++) {
                    free[cell(x, y, z)] = solidCell(x, y, z);
                }
            }
        }
        int[] found = new int[CELLS * CELLS * CELLS];
        int[] volumes = new int[found.length];
        int count = 0;
        for (int y = 0; y < CELLS; y++) {
            for (int z = 0; z < CELLS; z++) {
                for (int x = 0; x < CELLS; x++) {
                    if (!free[cell(x, y, z)]) {
                        continue;
                    }
                    int endX = x + 1;
                    while (endX < CELLS && free[cell(endX, y, z)]) {
                        endX++;
                    }
                    int endZ = z + 1;
                    while (endZ < CELLS && rowFree(free, x, endX, y, endZ)) {
                        endZ++;
                    }
                    int endY = y + 1;
                    while (endY < CELLS && layerFree(free, x, endX, endY, z, endZ)) {
                        endY++;
                    }
                    for (int cy = y; cy < endY; cy++) {
                        for (int cz = z; cz < endZ; cz++) {
                            for (int cx = x; cx < endX; cx++) {
                                free[cell(cx, cy, cz)] = false;
                            }
                        }
                    }
                    found[count] = OccluderBoxes.pack(x * CELL, y * CELL, z * CELL, endX * CELL, endY * CELL, endZ * CELL);
                    volumes[count] = (endX - x) * (endY - y) * (endZ - z);
                    count++;
                }
            }
        }
        if (count == 0) {
            return OccluderBoxes.NONE;
        }
        return new OccluderBoxes(largest(found, volumes, count));
    }

    private static int[] largest(int[] boxes, int[] volumes, int count) {
        if (count <= MAX_BOXES) {
            return Arrays.copyOf(boxes, count);
        }
        long[] ranked = new long[count];
        for (int index = 0; index < count; index++) {
            ranked[index] = (long) volumes[index] << 32 | index;
        }
        Arrays.sort(ranked);
        int[] kept = new int[MAX_BOXES];
        for (int index = 0; index < MAX_BOXES; index++) {
            kept[index] = boxes[(int) ranked[count - 1 - index]];
        }
        return kept;
    }

    private boolean solidCell(int cellX, int cellY, int cellZ) {
        for (int y = cellY * CELL; y < cellY * CELL + CELL; y++) {
            for (int z = cellZ * CELL; z < cellZ * CELL + CELL; z++) {
                for (int x = cellX * CELL; x < cellX * CELL + CELL; x++) {
                    if (!get(x, y, z)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static boolean rowFree(boolean[] free, int x, int endX, int y, int z) {
        for (int cx = x; cx < endX; cx++) {
            if (!free[cell(cx, y, z)]) {
                return false;
            }
        }
        return true;
    }

    private static boolean layerFree(boolean[] free, int x, int endX, int y, int z, int endZ) {
        for (int cz = z; cz < endZ; cz++) {
            if (!rowFree(free, x, endX, y, cz)) {
                return false;
            }
        }
        return true;
    }

    private static int cell(int x, int y, int z) {
        return (y * CELLS + z) * CELLS + x;
    }
}
