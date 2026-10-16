package com.aryston.aervium.client.gpu;

public final class GpuSectionEntry {
    private static final int ORIGIN_X = 0;
    private static final int ORIGIN_Y = 1;
    private static final int ORIGIN_Z = 2;
    private static final int SLOT_COUNT = 3;
    private static final int GROUP = 0;
    private static final int INDEX_COUNT = 1;
    private static final int FIRST_INDEX = 2;
    private static final int BASE_VERTEX = 3;
    private static final int FLAGS = 4;
    private static final int QUAD_OFFSETS = 5;
    private static final int PLANES = 9;
    private static final int HAS_FACES = 1;
    private static final int FACE_BOUNDARIES = 8;
    private static final int FACE_PLANES = 7;
    private static final int HALF_SHIFT = 16;

    private final int[] ints = new int[GpuSectionTable.STRIDE];
    private int slots;

    public void reset(int originX, int originY, int originZ) {
        ints[ORIGIN_X] = originX;
        ints[ORIGIN_Y] = originY;
        ints[ORIGIN_Z] = originZ;
        ints[SLOT_COUNT] = 0;
        slots = 0;
    }

    public boolean addSlot(int group, int indexCount, int firstIndex, int baseVertex, boolean hasFaces, int[] faces, int facesOffset) {
        if (slots == GpuSectionTable.SLOTS) {
            return false;
        }
        int slot = GpuSectionTable.HEADER + slots * GpuSectionTable.SLOT_STRIDE;
        ints[slot + GROUP] = group;
        ints[slot + INDEX_COUNT] = indexCount;
        ints[slot + FIRST_INDEX] = firstIndex;
        ints[slot + BASE_VERTEX] = baseVertex;
        ints[slot + FLAGS] = hasFaces ? HAS_FACES : 0;
        if (hasFaces) {
            for (int boundary = 0; boundary < FACE_BOUNDARIES; boundary += 2) {
                int low = faces[facesOffset + boundary];
                int high = faces[facesOffset + boundary + 1];
                if (low > GpuCulling.MAX_QUAD_OFFSET || high > GpuCulling.MAX_QUAD_OFFSET) {
                    return false;
                }
                ints[slot + QUAD_OFFSETS + boundary / 2] = low | high << HALF_SHIFT;
            }
            System.arraycopy(faces, facesOffset + FACE_BOUNDARIES, ints, slot + PLANES, FACE_PLANES);
        }
        slots++;
        ints[SLOT_COUNT] = slots;
        return true;
    }

    int[] ints() {
        return ints;
    }
}
