package com.aryston.aervium.client.chunk;

import com.mojang.renderpearl.api.buffers.GpuBuffer;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import com.aryston.aervium.client.culling.TerrainOcclusion;
import com.aryston.aervium.core.culling.CullingStatistics;
import com.aryston.aervium.core.mesh.FaceRanges;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.DynamicGpuData;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;

public final class SectionDrawLists {
    private static final int INDICES_PER_QUAD = 6;

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
        boolean occlusion = TerrainOcclusion.cullsCameraSections();
        CameraRenderState faceCamera = FaceCulling.cameraPass();
        long faceQuads = 0L;
        long faceSkipped = 0L;
        for (int index = 0; index < size; index++) {
            SectionRenderDispatcher.RenderSection section = (SectionRenderDispatcher.RenderSection) sections[index];
            if (occlusion && TerrainOcclusion.hiddenFromCamera(section)) {
                continue;
            }
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
                FaceRanges faces = draw.faces();
                int visible = faces == null || faceCamera == null ? FaceRanges.ALL : faces.visibleBuckets(
                    faceCamera.pos.x - renderOffset.getX(), faceCamera.pos.y - renderOffset.getY(), faceCamera.pos.z - renderOffset.getZ()
                );
                if (faces != null && faceCamera != null) {
                    faceQuads += draw.indexCount() / INDICES_PER_QUAD;
                }
                if (visible == FaceRanges.ALL) {
                    drawGroup.draws().add(new DynamicGpuData.IndexedDraw(draw.indexCount(), 1, draw.firstIndex(), draw.baseVertex(), sectionInfoDataIndex));
                } else {
                    faceSkipped += draw.indexCount() / INDICES_PER_QUAD - addFacingDraws(drawGroup.draws(), draw, faces, visible, sectionInfoDataIndex);
                }
            }
        }
        if (faceCamera != null) {
            CullingStatistics.SHARED.faces(faceQuads, faceSkipped);
        }
        return largestIndexCount;
    }

    private static int addFacingDraws(List<DynamicGpuData.IndexedDraw> draws, LayerDraw draw, FaceRanges faces, int visible, int sectionInfoDataIndex) {
        int runStart = -1;
        int drawn = 0;
        for (int bucket = 0; bucket <= FaceRanges.BUCKETS; bucket++) {
            boolean include = bucket < FaceRanges.BUCKETS && ((visible >> bucket & 1) != 0 || faces.quadCount(bucket) == 0);
            if (include) {
                if (runStart < 0) {
                    runStart = bucket;
                }
                continue;
            }
            if (runStart >= 0) {
                int firstQuad = faces.firstQuad(runStart);
                int quads = faces.firstQuad(bucket) - firstQuad;
                if (quads > 0) {
                    drawn += quads;
                    draws.add(new DynamicGpuData.IndexedDraw(
                        quads * INDICES_PER_QUAD, 1, draw.firstIndex() + firstQuad * INDICES_PER_QUAD, draw.baseVertex(), sectionInfoDataIndex
                    ));
                }
                runStart = -1;
            }
        }
        return drawn;
    }
}
