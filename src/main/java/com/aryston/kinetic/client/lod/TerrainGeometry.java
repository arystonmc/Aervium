package com.aryston.kinetic.client.lod;

import com.aryston.kinetic.core.lod.LodAllocator;
import com.aryston.kinetic.core.lod.LodMesher;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import org.jspecify.annotations.Nullable;

final class TerrainGeometry {
    private static final int PAGE_BYTES = 16 * 1024 * 1024;
    private static final int MAX_PAGES = 8;
    private static final int ALIGNMENT = LodMesher.VERTEX_BYTES * LodMesher.VERTICES_PER_QUAD;
    private final LodAllocator allocator = new LodAllocator(PAGE_BYTES, MAX_PAGES, ALIGNMENT);
    private final ArrayList<GpuBuffer> pages = new ArrayList<>();

    LodAllocator.@Nullable Allocation upload(ByteBuffer vertices) {
        LodAllocator.Allocation allocation = allocator.allocate(vertices.remaining());
        if (allocation == null) {
            return null;
        }
        while (allocation.page() >= pages.size()) {
            pages.add(RenderSystem.getDevice().createBuffer(() -> "Kinetic distant geometry page", GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST, PAGE_BYTES));
        }
        RenderSystem.getDevice().createCommandEncoder().writeToBuffer(pages.get(allocation.page()).slice(allocation.offset(), vertices.remaining()), vertices);
        return allocation;
    }

    GpuBuffer page(int page) {
        return pages.get(page);
    }

    void free(LodAllocator.@Nullable Allocation allocation) {
        if (allocation != null) {
            allocator.free(allocation);
        }
    }

    void close() {
        pages.forEach(GpuBuffer::close);
        pages.clear();
    }
}
