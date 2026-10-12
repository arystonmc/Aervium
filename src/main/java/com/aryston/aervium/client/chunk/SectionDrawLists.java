package com.aryston.aervium.client.chunk;

import com.mojang.renderpearl.api.buffers.GpuBuffer;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.DynamicGpuData;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.BlockPos;

public final class SectionDrawLists {
    private SectionDrawLists() {
    }

    public static boolean supported() {
        return SectionLayers.ALL.length <= SectionDrawRecord.SLOTS;
    }

    public static int extract(
        ObjectArrayList<SectionRenderDispatcher.RenderSection> visibleSections,
        SectionRenderDispatcher dispatcher,
        long fadeDuration,
        long now,
        boolean respectTranslucentOrder,
        List<DynamicGpuData.ChunkSectionInfo> sectionInfos,
        Map<ChunkSectionLayer, List<LevelRenderer.ChunkDrawGroup>> drawGroups
    ) {
        int largestIndexCount = 0;
        int lastTransparentGroupHash = 0;
        Int2ObjectOpenHashMap<LevelRenderer.ChunkDrawGroup> drawGroupCache = new Int2ObjectOpenHashMap<>();
        Object[] sections = visibleSections.elements();
        int size = visibleSections.size();
        for (int index = 0; index < size; index++) {
            SectionRenderDispatcher.RenderSection section = (SectionRenderDispatcher.RenderSection) sections[index];
            SectionMesh mesh = section.getSectionMesh();
            SectionDrawRecordHolder holder = (SectionDrawRecordHolder) section;
            SectionDrawRecord record = holder.aervium$drawRecord();
            if (record == null || !record.isFor(mesh)) {
                record = SectionDrawRecord.build(mesh, dispatcher);
                holder.aervium$setDrawRecord(record);
                if (mesh instanceof SectionSliceHolder slices) {
                    slices.aervium$trackRecord(record);
                }
            }
            if (record.count == 0) {
                continue;
            }
            BlockPos renderOffset = section.getRenderOrigin();
            int sectionInfoDataIndex = sectionInfos.size();
            sectionInfos.add(new DynamicGpuData.ChunkSectionInfo(
                renderOffset.getX(), renderOffset.getY(), renderOffset.getZ(), section.getVisibility(now, fadeDuration)
            ));
            for (int slot = 0; slot < record.count; slot++) {
                LayerDraw draw = record.slot(slot);
                if (!draw.customIndexBuffer() && draw.indexCount() > largestIndexCount) {
                    largestIndexCount = draw.indexCount();
                }
                ChunkSectionLayer layer = draw.layer();
                int groupHash = draw.groupHash();
                LevelRenderer.ChunkDrawGroup drawGroup = null;
                if (layer.translucent() && respectTranslucentOrder) {
                    if (groupHash == lastTransparentGroupHash) {
                        drawGroup = drawGroups.get(layer).getLast();
                    }
                    lastTransparentGroupHash = groupHash;
                } else {
                    drawGroup = drawGroupCache.getOrDefault(groupHash, null);
                }
                if (drawGroup == null) {
                    GpuBuffer indexBuffer = draw.indexBuffer();
                    drawGroup = new LevelRenderer.ChunkDrawGroup(
                        draw.vertexBuffer().slice(), indexBuffer != null ? indexBuffer.slice() : null, draw.indexType(), new ArrayList<>()
                    );
                    drawGroupCache.put(groupHash, drawGroup);
                    drawGroups.get(layer).add(drawGroup);
                }
                drawGroup.draws().add(new DynamicGpuData.IndexedDraw(draw.indexCount(), 1, draw.firstIndex(), draw.baseVertex(), sectionInfoDataIndex));
            }
        }
        return largestIndexCount;
    }
}
