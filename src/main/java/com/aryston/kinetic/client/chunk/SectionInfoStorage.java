package com.aryston.kinetic.client.chunk;

import com.aryston.kinetic.client.gpu.GpuBufferUsage;
import com.aryston.kinetic.client.gpu.GpuCulling;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

final class SectionInfoStorage {
    private static final String LABEL = "Kinetic chunk sections";
    private static final int INITIAL_SECTIONS = 16384;
    private static final int ALIGNMENT = 256;
    private static final int SECTIONS_PER_ALIGNMENT = ALIGNMENT / PackedSectionInfos.BYTES_PER_SECTION;
    private static final int USAGE = GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST | GpuBufferUsage.STORAGE;

    private final List<GpuBuffer> retired = new ArrayList<>();
    private @Nullable GpuBuffer buffer;
    private @Nullable ByteBuffer staging;
    private int capacity;
    private int next;

    GpuBufferSlice write(PackedSectionInfos sections, int gpuSections) {
        int cpuSections = gpuSections == 0 ? sections.count() : Mth.roundToward(sections.count(), SECTIONS_PER_ALIGNMENT);
        int bytes = (cpuSections + gpuSections) * PackedSectionInfos.BYTES_PER_SECTION;
        if (bytes == 0) {
            return new GpuBufferSlice(null, 0L, 0L);
        }
        reserve(bytes);
        int cpuBytes = sections.count() * PackedSectionInfos.BYTES_PER_SECTION;
        if (cpuBytes > 0) {
            sections.write(MemoryUtil.memAddress0(staging) + next);
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(buffer.slice(next, cpuBytes), staging.slice(next, cpuBytes));
        }
        if (gpuSections > 0) {
            GpuCulling.dispatch(buffer, next + (long) cpuSections * PackedSectionInfos.BYTES_PER_SECTION, cpuSections);
        }
        GpuBufferSlice slice = buffer.slice(next, bytes);
        next = Mth.roundToward(next + bytes, ALIGNMENT);
        return slice;
    }

    void endFrame() {
        next = 0;
        for (GpuBuffer old : retired) {
            old.close();
        }
        retired.clear();
    }

    private void reserve(int bytes) {
        if (buffer != null && next + bytes <= capacity) {
            return;
        }
        if (buffer != null) {
            retired.add(buffer);
        }
        capacity = Mth.smallestEncompassingPowerOfTwo(Math.max(Math.max(capacity * 2, bytes), INITIAL_SECTIONS * PackedSectionInfos.BYTES_PER_SECTION));
        next = 0;
        int size = capacity;
        buffer = RenderSystem.getDevice().createBuffer(() -> LABEL + " " + size, USAGE, capacity);
        staging = staging == null ? MemoryUtil.memAlloc(capacity) : MemoryUtil.memRealloc(staging, capacity);
    }
}
