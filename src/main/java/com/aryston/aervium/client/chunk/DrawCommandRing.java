package com.aryston.aervium.client.chunk;

import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.DynamicGpuData;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

final class DrawCommandRing {
    private static final String LABEL = "Aervium chunk draw commands";
    private static final int INITIAL_DRAWS = 16384;
    private static final int USAGE = GpuBuffer.USAGE_INDIRECT_PARAMETERS | GpuBuffer.USAGE_MAP_WRITE;

    private final List<MappableRingBuffer> retired = new ArrayList<>();
    private @Nullable MappableRingBuffer ring;
    private int capacity;
    private int next;

    GpuBufferSlice[] write(List<List<DynamicGpuData.IndexedDraw>> lists) {
        int total = 0;
        for (List<DynamicGpuData.IndexedDraw> draws : lists) {
            total += draws.size() * PackedDraws.BYTES_PER_DRAW;
        }
        MappableRingBuffer current = reserve(total);
        GpuBuffer buffer = current.currentBuffer();
        GpuBufferSlice[] slices = new GpuBufferSlice[lists.size()];
        if (total == 0) {
            for (int index = 0; index < slices.length; index++) {
                slices[index] = buffer.slice(next, 0);
            }
            return slices;
        }
        int offset = next;
        try (GpuBufferSlice.MappedView view = buffer.slice(next, total).map(false, true)) {
            long address = MemoryUtil.memAddress(view.data());
            for (int index = 0; index < slices.length; index++) {
                List<DynamicGpuData.IndexedDraw> draws = lists.get(index);
                int bytes = draws.size() * PackedDraws.BYTES_PER_DRAW;
                PackedDraws.write(draws, address + offset - next);
                slices[index] = buffer.slice(offset, bytes);
                offset += bytes;
            }
        }
        next = offset;
        return slices;
    }

    void endFrame() {
        next = 0;
        if (ring != null) {
            ring.rotate();
        }
        for (MappableRingBuffer buffer : retired) {
            buffer.close();
        }
        retired.clear();
    }

    private MappableRingBuffer reserve(int bytes) {
        if (ring != null && next + bytes <= capacity) {
            return ring;
        }
        if (ring != null) {
            retired.add(ring);
        }
        capacity = Mth.smallestEncompassingPowerOfTwo(Math.max(Math.max(capacity * 2, bytes), INITIAL_DRAWS * PackedDraws.BYTES_PER_DRAW));
        next = 0;
        int size = capacity;
        ring = new MappableRingBuffer(() -> LABEL + " " + size, USAGE, capacity);
        return ring;
    }
}
