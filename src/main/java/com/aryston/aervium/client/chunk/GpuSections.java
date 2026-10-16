package com.aryston.aervium.client.chunk;

import com.aryston.aervium.client.gpu.GpuCulling;
import com.aryston.aervium.client.gpu.GpuSectionEntry;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;

final class GpuSections {
    static final int CPU_NONE = 0;
    static final int CPU_TRANSLUCENT = 1;
    static final int CPU_ALL = 2;
    private static final int GPU_SLOTS = 1;
    private static final int TRANSLUCENT_SLOTS = 2;
    private static final int UNSUPPORTED = -1;
    private static final int NO_GROUP = -1;
    private static final GpuSectionEntry ENTRY = new GpuSectionEntry();

    private GpuSections() {
    }

    static int cpuSlots(SectionRenderDispatcher.RenderSection section, SectionRenderDispatcher dispatcher, long now, long fadeDuration) {
        SectionDrawRecordHolder holder = (SectionDrawRecordHolder) section;
        int meshVersion = ((MeshVersionHolder) section).aervium$meshVersion();
        int flags = holder.aervium$gpuEpoch() == GpuCulling.epoch() && holder.aervium$gpuVersion() == meshVersion
            ? holder.aervium$gpuFlags()
            : upload(section, holder, meshVersion, dispatcher);
        if (flags == UNSUPPORTED) {
            return CPU_ALL;
        }
        if ((flags & GPU_SLOTS) != 0) {
            GpuCulling.addVisible(section.index, section.getVisibility(now, fadeDuration));
        }
        return (flags & TRANSLUCENT_SLOTS) != 0 ? CPU_TRANSLUCENT : CPU_NONE;
    }

    private static int upload(
        SectionRenderDispatcher.RenderSection section, SectionDrawRecordHolder holder, int meshVersion, SectionRenderDispatcher dispatcher
    ) {
        if (!GpuCulling.fits(section.index)) {
            return UNSUPPORTED;
        }
        int[] packed = SectionDrawLists.currentDraws(section, dispatcher);
        LayerDraw[] draws = holder.aervium$drawRecord().draws;
        ENTRY.reset(packed[SectionDrawRecord.ORIGIN_X], packed[SectionDrawRecord.ORIGIN_Y], packed[SectionDrawRecord.ORIGIN_Z]);
        int flags = 0;
        int sharedIndexCount = 0;
        for (int slot = 0; slot < packed[SectionDrawRecord.COUNT]; slot++) {
            int offset = SectionDrawRecord.slotOffset(slot);
            ChunkSectionLayer layer = SectionLayers.ALL[packed[offset + SectionDrawRecord.LAYER]];
            if (layer.translucent()) {
                flags |= TRANSLUCENT_SLOTS;
                continue;
            }
            LayerDraw draw = draws[slot];
            int group = GpuCulling.groupId(layer, draw.vertexBuffer(), draw.indexBuffer(), draw.indexType());
            int slotFlags = packed[offset + SectionDrawRecord.FLAGS];
            int indexCount = packed[offset + SectionDrawRecord.INDEX_COUNT];
            boolean added = group != NO_GROUP && ENTRY.addSlot(
                group,
                indexCount,
                packed[offset + SectionDrawRecord.FIRST_INDEX],
                packed[offset + SectionDrawRecord.BASE_VERTEX],
                (slotFlags & SectionDrawRecord.HAS_FACES) != 0,
                packed,
                offset + SectionDrawRecord.FACES
            );
            if (!added) {
                return UNSUPPORTED;
            }
            if ((slotFlags & SectionDrawRecord.CUSTOM_INDEX_BUFFER) == 0) {
                sharedIndexCount = Math.max(sharedIndexCount, indexCount);
            }
            flags |= GPU_SLOTS;
        }
        if ((flags & GPU_SLOTS) != 0) {
            GpuCulling.upload(section.index, ENTRY, sharedIndexCount);
        }
        holder.aervium$setGpuState(meshVersion, GpuCulling.epoch(), flags);
        return flags;
    }
}
