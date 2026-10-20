package com.aryston.aervium.client.lod;

import com.aryston.aervium.core.lod.LodAllocator;
import com.aryston.aervium.core.lod.LodMesher;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.commands.GpuFence;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

final class TerrainDraws {
    static final int INSTANCE_BYTES = 9 * Float.BYTES;
    private static final int INITIAL_DRAWS = 4096;
    private static final int INDICES_PER_QUAD = 6;
    private static final int MAX_STAGING_BYTES = 32 * 1024 * 1024;
    private final List<Slot> available = new ArrayList<>();
    private final List<Slot> used = new ArrayList<>();
    private @Nullable Slot current;
    private int allocatedBytes;
    private int next;

    void draw(RenderPass pass, TerrainGeometry geometry, List<Draw> draws) {
        if (draws.isEmpty()) {
            return;
        }
        Slot slot = reserve(draws.size());
        if (slot == null) {
            return;
        }
        int commandBytes = draws.size() * RenderPass.INDIRECT_INDEXED_DRAW_SIZE;
        int instanceBytes = draws.size() * INSTANCE_BYTES;
        GpuBufferSlice commandSlice = slot.commands.slice((long) next * RenderPass.INDIRECT_INDEXED_DRAW_SIZE, commandBytes);
        GpuBufferSlice instanceSlice = slot.instances.slice((long) next * INSTANCE_BYTES, instanceBytes);
        try (var commandView = commandSlice.map(false, true); var instanceView = instanceSlice.map(false, true)) {
            ByteBuffer commandData = commandView.data().order(ByteOrder.nativeOrder());
            ByteBuffer instanceData = instanceView.data().order(ByteOrder.nativeOrder());
            for (int index = 0; index < draws.size(); index++) {
                Draw draw = draws.get(index);
                commandData.putInt(draw.quads() * INDICES_PER_QUAD).putInt(1).putInt(draw.firstQuad() * INDICES_PER_QUAD)
                    .putInt(draw.allocation().offset() / LodMesher.VERTEX_BYTES).putInt(index);
                instanceData.putFloat(draw.x()).putFloat(draw.y()).putFloat(draw.z()).putFloat(draw.low()).putFloat(draw.high())
                    .putFloat(draw.minX()).putFloat(draw.minZ()).putFloat(draw.maxX()).putFloat(draw.maxZ());
            }
        }
        pass.setVertexBuffer(1, instanceSlice);
        int start = 0;
        while (start < draws.size()) {
            int page = draws.get(start).allocation().page();
            int end = start + 1;
            while (end < draws.size() && draws.get(end).allocation().page() == page) {
                end++;
            }
            pass.setVertexBuffer(0, geometry.page(page).slice());
            pass.drawIndexedIndirect(slot.commands.slice((long) (next + start) * RenderPass.INDIRECT_INDEXED_DRAW_SIZE,
                (long) (end - start) * RenderPass.INDIRECT_INDEXED_DRAW_SIZE), end - start);
            start = end;
        }
        next += draws.size();
    }

    void endFrame() {
        next = 0;
        current = null;
        for (Slot slot : used) {
            slot.fence = RenderSystem.getDevice().createCommandEncoder().createFence();
            available.add(slot);
        }
        used.clear();
    }

    void close() {
        available.forEach(Slot::close);
        used.forEach(Slot::close);
        available.clear();
        used.clear();
        current = null;
        allocatedBytes = 0;
        next = 0;
    }

    private @Nullable Slot reserve(int count) {
        if (current != null && next + count <= current.capacity) {
            return current;
        }
        for (var iterator = available.iterator(); iterator.hasNext();) {
            Slot slot = iterator.next();
            if (slot.fence != null && !slot.fence.awaitCompletion(0L)) {
                continue;
            }
            iterator.remove();
            if (slot.capacity >= count) {
                if (slot.fence != null) {
                    slot.fence.close();
                }
                slot.fence = null;
                used.add(slot);
                current = slot;
                next = 0;
                return slot;
            }
            allocatedBytes -= slot.bytes();
            slot.close();
        }
        int capacity = Math.max(INITIAL_DRAWS, Integer.highestOneBit(count - 1) * 2);
        int bytes = capacity * (RenderPass.INDIRECT_INDEXED_DRAW_SIZE + INSTANCE_BYTES);
        if (allocatedBytes + bytes > MAX_STAGING_BYTES) {
            return null;
        }
        Slot slot = new Slot(capacity);
        allocatedBytes += bytes;
        used.add(slot);
        current = slot;
        next = 0;
        return slot;
    }

    private static final class Slot {
        private final int capacity;
        private final GpuBuffer commands;
        private final GpuBuffer instances;
        private @Nullable GpuFence fence;

        private Slot(int capacity) {
            this.capacity = capacity;
            commands = RenderSystem.getDevice().createBuffer(() -> "Aervium distant draw commands", GpuBuffer.USAGE_INDIRECT_PARAMETERS | GpuBuffer.USAGE_MAP_WRITE,
                capacity * RenderPass.INDIRECT_INDEXED_DRAW_SIZE);
            instances = RenderSystem.getDevice().createBuffer(() -> "Aervium distant draw instances", GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_MAP_WRITE,
                capacity * INSTANCE_BYTES);
        }

        private int bytes() {
            return capacity * (RenderPass.INDIRECT_INDEXED_DRAW_SIZE + INSTANCE_BYTES);
        }

        private void close() {
            commands.close();
            instances.close();
            if (fence != null) {
                fence.close();
                fence = null;
            }
        }
    }

    record Draw(LodAllocator.Allocation allocation, int firstQuad, int quads, float x, float y, float z,
        float low, float high, float minX, float minZ, float maxX, float maxZ, double distance) {
    }
}
