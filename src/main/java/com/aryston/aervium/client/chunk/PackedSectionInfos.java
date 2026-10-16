package com.aryston.aervium.client.chunk;

import java.util.Arrays;
import org.lwjgl.system.MemoryUtil;

final class PackedSectionInfos {
    static final int INTS_PER_SECTION = 4;
    static final int BYTES_PER_SECTION = INTS_PER_SECTION * Integer.BYTES;
    private static final int INITIAL_SECTIONS = 4096;
    private static final int Y = 1;
    private static final int Z = 2;
    private static final int VISIBILITY = 3;

    private int[] ints = new int[INITIAL_SECTIONS * INTS_PER_SECTION];
    private int count;

    void clear() {
        count = 0;
    }

    int add(int x, int y, int z, float visibility) {
        int offset = count * INTS_PER_SECTION;
        if (offset + INTS_PER_SECTION > ints.length) {
            ints = Arrays.copyOf(ints, ints.length * 2);
        }
        ints[offset] = x;
        ints[offset + Y] = y;
        ints[offset + Z] = z;
        ints[offset + VISIBILITY] = Float.floatToRawIntBits(visibility);
        return count++;
    }

    int count() {
        return count;
    }

    void write(long address) {
        if (count > 0) {
            MemoryUtil.memIntBuffer(address, count * INTS_PER_SECTION).put(ints, 0, count * INTS_PER_SECTION);
        }
    }
}
