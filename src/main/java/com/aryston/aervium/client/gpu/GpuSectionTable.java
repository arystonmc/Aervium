package com.aryston.aervium.client.gpu;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkBufferCopy;
import org.lwjgl.vulkan.VkCommandBuffer;

final class GpuSectionTable {
    static final int STRIDE = 36;
    static final int HEADER = 4;
    static final int SLOT_STRIDE = 16;
    static final int SLOTS = 2;
    static final int BYTES = STRIDE * Integer.BYTES;
    private static final String LABEL = "Aervium section table";
    private static final String STAGING_LABEL = "Aervium section table uploads";
    private static final int USAGE = GpuBuffer.USAGE_COPY_DST | GpuBufferUsage.STORAGE;
    private static final int STAGING_USAGE = GpuBuffer.USAGE_COPY_SRC | GpuBuffer.USAGE_MAP_WRITE;
    private static final int INITIAL_SECTIONS = 32768;
    private static final int INITIAL_UPLOADS = 1024;

    private final List<GpuBuffer> retiredTables = new ArrayList<>();
    private final List<MappableRingBuffer> retiredStaging = new ArrayList<>();
    private @Nullable GpuBuffer table;
    private int capacity;
    private int requested = INITIAL_SECTIONS;
    private int epoch;
    private @Nullable MappableRingBuffer staging;
    private int stagingCapacity;
    private GpuBufferSlice.@Nullable MappedView view;
    private long address;
    private int[] targets = new int[INITIAL_UPLOADS];
    private int uploads;

    void begin(int maxUploads) {
        if (table == null || requested > capacity) {
            if (table != null) {
                retiredTables.add(table);
            }
            capacity = Mth.smallestEncompassingPowerOfTwo(requested);
            int size = capacity;
            table = RenderSystem.getDevice().createBuffer(() -> LABEL + " " + size, USAGE, (long) capacity * BYTES);
            epoch++;
        }
        if (staging == null || maxUploads > stagingCapacity) {
            if (staging != null) {
                retiredStaging.add(staging);
            }
            stagingCapacity = Mth.smallestEncompassingPowerOfTwo(Math.max(maxUploads, INITIAL_UPLOADS));
            int entries = stagingCapacity;
            staging = new MappableRingBuffer(() -> STAGING_LABEL + " " + entries, STAGING_USAGE, stagingCapacity * BYTES);
        }
        if (targets.length < stagingCapacity) {
            targets = Arrays.copyOf(targets, stagingCapacity);
        }
        uploads = 0;
    }

    int epoch() {
        return epoch;
    }

    boolean fits(int section) {
        if (section < capacity) {
            return true;
        }
        requested = Math.max(requested, section + 1);
        return false;
    }

    void put(int section, int[] entry) {
        if (view == null) {
            view = staging.currentBuffer().slice(0, (long) stagingCapacity * BYTES).map(false, true);
            address = MemoryUtil.memAddress(view.data());
        }
        MemoryUtil.memIntBuffer(address + (long) uploads * BYTES, STRIDE).put(entry, 0, STRIDE);
        targets[uploads++] = section;
    }

    GpuBuffer buffer() {
        return table;
    }

    long size() {
        return (long) capacity * BYTES;
    }

    void recordUploads(VkCommandBuffer commandBuffer) {
        closeView();
        if (uploads == 0) {
            return;
        }
        VkBufferCopy.Buffer regions = VkBufferCopy.calloc(uploads);
        try {
            for (int upload = 0; upload < uploads; upload++) {
                regions.get(upload).srcOffset((long) upload * BYTES).dstOffset((long) targets[upload] * BYTES).size(BYTES);
            }
            VK10.vkCmdCopyBuffer(commandBuffer, VulkanAccess.handle(staging.currentBuffer()), VulkanAccess.handle(table), regions);
        } finally {
            regions.free();
        }
        uploads = 0;
    }

    void endFrame() {
        closeView();
        uploads = 0;
        if (staging != null) {
            staging.rotate();
        }
        retiredTables.forEach(GpuBuffer::close);
        retiredTables.clear();
        retiredStaging.forEach(MappableRingBuffer::close);
        retiredStaging.clear();
    }

    private void closeView() {
        if (view != null) {
            view.close();
            view = null;
        }
    }
}
