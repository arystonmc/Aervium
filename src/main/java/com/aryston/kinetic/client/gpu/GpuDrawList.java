package com.aryston.kinetic.client.gpu;

import java.util.AbstractList;
import net.minecraft.client.renderer.DynamicGpuData;

public final class GpuDrawList extends AbstractList<DynamicGpuData.IndexedDraw> {
    private final int rank;
    private final int capacity;

    GpuDrawList(int rank, int capacity) {
        this.rank = rank;
        this.capacity = capacity;
    }

    int rank() {
        return rank;
    }

    @Override
    public DynamicGpuData.IndexedDraw get(int index) {
        throw new UnsupportedOperationException("GPU-built draws are not readable on the CPU");
    }

    @Override
    public int size() {
        return capacity;
    }
}
