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
            SectionDrawRecordHolder holder = (SectionDrawRecordHolder) section;
            int meshVersion = holder.aervium$meshVersion();
            SectionDrawRecord record = holder.aervium$drawRecord();
            if (record == null || !record.isFor(meshVersion)) {
                SectionMesh mesh = section.getSectionMesh();
                record = SectionDrawRecord.build(mesh, meshVersion, section.getRenderOrigin(), dispatcher);
                holder.aervium$setDrawRecord(record);
                if (mesh instanceof SectionSliceHolder slices) {
                    slices.aervium$trackRecord(record);
                }
            }
            if (record.count == 0) {
                continue;
            }
            int sectionInfoDataIndex = sectionInfos.size();
            sectionInfos.add(new DynamicGpuData.ChunkSectionInfo(
                record.originX, record.originY, record.originZ, section.getVisibility(now, fadeDuration)
            ));

            int[] packed = record.packed;
            for (int slot = 0; slot < record.count; slot++) {
                int offset = slot * SectionDrawRecord.STRIDE;
                int indexCount = packed[offset + SectionDrawRecord.INDEX_COUNT];
                int flags = packed[offset + SectionDrawRecord.FLAGS];
                if ((flags & SectionDrawRecord.CUSTOM_INDEX_BUFFER) == 0 && indexCount > largestIndexCount) {
                    largestIndexCount = indexCount;
                }
                ChunkSectionLayer layer = SectionLayers.ALL[packed[offset + SectionDrawRecord.LAYER]];
                int groupHash = packed[offset + SectionDrawRecord.GROUP_HASH];
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
                    LayerDraw draw = record.draws[slot];
                    GpuBuffer indexBuffer = draw.indexBuffer();
                    drawGroup = new LevelRenderer.ChunkDrawGroup(
                        draw.vertexBuffer().slice(), indexBuffer != null ? indexBuffer.slice() : null, draw.indexType(), new ArrayList<>()
                    );
                    drawGroupCache.put(groupHash, drawGroup);
                    drawGroups.get(layer).add(drawGroup);
                }
                boolean hasFaces = (flags & SectionDrawRecord.HAS_FACES) != 0;
                int faces = offset + SectionDrawRecord.FACES;
                int visible = !hasFaces || faceCamera == null ? FaceRanges.ALL : FaceRanges.visibleBuckets(
                    packed, faces, faceCamera.pos.x - record.originX, faceCamera.pos.y - record.originY, faceCamera.pos.z - record.originZ
                );
                if (hasFaces && faceCamera != null) {
                    faceQuads += indexCount / INDICES_PER_QUAD;
                }
                int firstIndex = packed[offset + SectionDrawRecord.FIRST_INDEX];
                int baseVertex = packed[offset + SectionDrawRecord.BASE_VERTEX];
                if (visible == FaceRanges.ALL) {
                    drawGroup.draws().add(new DynamicGpuData.IndexedDraw(indexCount, 1, firstIndex, baseVertex, sectionInfoDataIndex));
                } else {
                    faceSkipped += indexCount / INDICES_PER_QUAD
                        - addFacingDraws(drawGroup.draws(), packed, faces, firstIndex, baseVertex, visible, sectionInfoDataIndex);
                }
            }
        }
        if (faceCamera != null) {
            CullingStatistics.SHARED.faces(faceQuads, faceSkipped);
        }
        return largestIndexCount;
    }

    private static int addFacingDraws(
        List<DynamicGpuData.IndexedDraw> draws, int[] packed, int faces, int firstIndex, int baseVertex, int visible, int sectionInfoDataIndex
    ) {
        int runStart = -1;
        int drawn = 0;
        for (int bucket = 0; bucket <= FaceRanges.BUCKETS; bucket++) {
            boolean include = bucket < FaceRanges.BUCKETS && ((visible >> bucket & 1) != 0 || FaceRanges.quadCount(packed, faces, bucket) == 0);
            if (include) {
                if (runStart < 0) {
                    runStart = bucket;
                }
                continue;
            }
            if (runStart >= 0) {
                int firstQuad = FaceRanges.firstQuad(packed, faces, runStart);
                int quads = FaceRanges.firstQuad(packed, faces, bucket) - firstQuad;
                if (quads > 0) {
                    drawn += quads;
                    draws.add(new DynamicGpuData.IndexedDraw(
                        quads * INDICES_PER_QUAD, 1, firstIndex + firstQuad * INDICES_PER_QUAD, baseVertex, sectionInfoDataIndex
                    ));
                }
                runStart = -1;
            }
        }
        return drawn;
    }
}
