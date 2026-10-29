package com.aryston.kinetic.client.chunk;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.renderer.DynamicGpuData;
import org.lwjgl.system.MemoryUtil;

public final class PackedDraws extends AbstractList<DynamicGpuData.IndexedDraw> {
    static final int INTS_PER_DRAW = 5;
    static final int BYTES_PER_DRAW = INTS_PER_DRAW * Integer.BYTES;
    private static final int INITIAL_DRAWS = 64;
    private static final int SINGLE_INSTANCE = 1;
    private static final int INSTANCE_COUNT = 1;
    private static final int FIRST_INDEX = 2;
    private static final int BASE_VERTEX = 3;
    private static final int BASE_INSTANCE = 4;

    private final Backwards backwards = new Backwards();
    private int[] ints = new int[INITIAL_DRAWS * INTS_PER_DRAW];
    private int size;

    void reset() {
        size = 0;
    }

    void add(int indexCount, int firstIndex, int baseVertex, int baseInstance) {
        int offset = size * INTS_PER_DRAW;
        if (offset + INTS_PER_DRAW > ints.length) {
            ints = Arrays.copyOf(ints, ints.length * 2);
        }
        ints[offset] = indexCount;
        ints[offset + INSTANCE_COUNT] = SINGLE_INSTANCE;
        ints[offset + FIRST_INDEX] = firstIndex;
        ints[offset + BASE_VERTEX] = baseVertex;
        ints[offset + BASE_INSTANCE] = baseInstance;
        size++;
    }

    @Override
    public DynamicGpuData.IndexedDraw get(int index) {
        Objects.checkIndex(index, size);
        int offset = index * INTS_PER_DRAW;
        return new DynamicGpuData.IndexedDraw(
            ints[offset], ints[offset + INSTANCE_COUNT], ints[offset + FIRST_INDEX], ints[offset + BASE_VERTEX], ints[offset + BASE_INSTANCE]
        );
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public List<DynamicGpuData.IndexedDraw> reversed() {
        return backwards;
    }

    static boolean packed(List<DynamicGpuData.IndexedDraw> draws) {
        return draws instanceof PackedDraws || draws instanceof Backwards;
    }

    static void write(List<DynamicGpuData.IndexedDraw> draws, long address) {
        if (draws instanceof Backwards view) {
            view.source().writeBackwards(address);
            return;
        }
        ((PackedDraws) draws).writeForwards(address);
    }

    private void writeForwards(long address) {
        if (size > 0) {
            MemoryUtil.memIntBuffer(address, size * INTS_PER_DRAW).put(ints, 0, size * INTS_PER_DRAW);
        }
    }

    private void writeBackwards(long address) {
        long target = address;
        for (int draw = size - 1; draw >= 0; draw--) {
            int offset = draw * INTS_PER_DRAW;
            for (int value = 0; value < INTS_PER_DRAW; value++) {
                MemoryUtil.memPutInt(target, ints[offset + value]);
                target += Integer.BYTES;
            }
        }
    }

    private final class Backwards extends AbstractList<DynamicGpuData.IndexedDraw> {
        @Override
        public DynamicGpuData.IndexedDraw get(int index) {
            return PackedDraws.this.get(size - 1 - index);
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        public List<DynamicGpuData.IndexedDraw> reversed() {
            return PackedDraws.this;
        }

        PackedDraws source() {
            return PackedDraws.this;
        }
    }
}
