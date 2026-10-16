package com.aryston.aervium.client.chunk;

import com.mojang.renderpearl.api.buffers.GpuBuffer;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import com.aryston.aervium.client.gpu.GpuCulling;
import com.aryston.aervium.client.culling.TerrainOcclusion;
import com.aryston.aervium.core.culling.CullingStatistics;
import com.aryston.aervium.core.mesh.FaceRanges;
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
    private static final Int2ObjectOpenHashMap<LevelRenderer.ChunkDrawGroup> DRAW_GROUP_CACHE = new Int2ObjectOpenHashMap<>();

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
        boolean packedSections = DirectDraws.beginExtract();
        Int2ObjectOpenHashMap<LevelRenderer.ChunkDrawGroup> drawGroupCache = DRAW_GROUP_CACHE;
        drawGroupCache.clear();
        int largestIndexCount = 0;
        int lastTransparentGroupHash = 0;
        Object[] sections = visibleSections.elements();
        int size = visibleSections.size();
        boolean occlusion = TerrainOcclusion.cullsCameraSections();
        CameraRenderState faceCamera = FaceCulling.cameraPass();
        CameraRenderState camera = DirectDraws.cameraPass();
        boolean gpu = packedSections && camera != null && GpuCulling.beginPass(size, camera.pos, faceCamera != null);
        long faceQuads = 0L;
        long faceSkipped = 0L;
        for (int index = 0; index < size; index++) {
            SectionRenderDispatcher.RenderSection section = (SectionRenderDispatcher.RenderSection) sections[index];
            if (occlusion && TerrainOcclusion.hiddenFromCamera(section)) {
                continue;
            }
            int cpuSlots = gpu ? GpuSections.cpuSlots(section, dispatcher, now, fadeDuration) : GpuSections.CPU_ALL;
            if (cpuSlots == GpuSections.CPU_NONE) {
                continue;
            }
            int[] packed = currentDraws(section, dispatcher);
            int count = packed[SectionDrawRecord.COUNT];
            if (count == 0) {
                continue;
            }
            int originX = packed[SectionDrawRecord.ORIGIN_X];
            int originY = packed[SectionDrawRecord.ORIGIN_Y];
            int originZ = packed[SectionDrawRecord.ORIGIN_Z];
            float visibility = section.getVisibility(now, fadeDuration);
            int sectionInfoDataIndex = packedSections
                ? DirectDraws.addSection(originX, originY, originZ, visibility)
                : addRecord(sectionInfos, originX, originY, originZ, visibility);

            for (int slot = 0; slot < count; slot++) {
                int offset = SectionDrawRecord.slotOffset(slot);
                ChunkSectionLayer layer = SectionLayers.ALL[packed[offset + SectionDrawRecord.LAYER]];
                if (cpuSlots == GpuSections.CPU_TRANSLUCENT && !layer.translucent()) {
                    continue;
                }
                int indexCount = packed[offset + SectionDrawRecord.INDEX_COUNT];
                int flags = packed[offset + SectionDrawRecord.FLAGS];
                if ((flags & SectionDrawRecord.CUSTOM_INDEX_BUFFER) == 0 && indexCount > largestIndexCount) {
                    largestIndexCount = indexCount;
                }
                int groupHash = packed[offset + SectionDrawRecord.GROUP_HASH];
                LevelRenderer.ChunkDrawGroup drawGroup = null;
                if (layer.translucent() && respectTranslucentOrder) {
                    if (groupHash == lastTransparentGroupHash) {
                        drawGroup = drawGroups.get(layer).getLast();
                    }
                    lastTransparentGroupHash = groupHash;
                } else {
                    drawGroup = drawGroupCache.get(groupHash);
                }
                if (drawGroup == null) {
                    LayerDraw draw = ((SectionDrawRecordHolder) section).aervium$drawRecord().draws[slot];
                    GpuBuffer indexBuffer = draw.indexBuffer();
                    drawGroup = new LevelRenderer.ChunkDrawGroup(
                        draw.vertexBuffer().slice(), indexBuffer != null ? indexBuffer.slice() : null, draw.indexType(), DirectDraws.nextDraws()
                    );
                    drawGroupCache.put(groupHash, drawGroup);
                    drawGroups.get(layer).add(drawGroup);
                }
                PackedDraws draws = (PackedDraws) drawGroup.draws();
                boolean hasFaces = (flags & SectionDrawRecord.HAS_FACES) != 0;
                int faces = offset + SectionDrawRecord.FACES;
                int visible = !hasFaces || faceCamera == null ? FaceRanges.ALL : FaceRanges.visibleBuckets(
                    packed, faces, faceCamera.pos.x - originX, faceCamera.pos.y - originY, faceCamera.pos.z - originZ
                );
                if (hasFaces && faceCamera != null) {
                    faceQuads += indexCount / INDICES_PER_QUAD;
                }
                int firstIndex = packed[offset + SectionDrawRecord.FIRST_INDEX];
                int baseVertex = packed[offset + SectionDrawRecord.BASE_VERTEX];
                if (visible == FaceRanges.ALL) {
                    draws.add(indexCount, firstIndex, baseVertex, sectionInfoDataIndex);
                } else {
                    faceSkipped += indexCount / INDICES_PER_QUAD
                        - addFacingDraws(draws, packed, faces, firstIndex, baseVertex, visible, sectionInfoDataIndex);
                }
            }
        }
        if (gpu) {
            return Math.max(largestIndexCount, GpuCulling.finishPass(drawGroups));
        }
        if (faceCamera != null) {
            CullingStatistics.SHARED.faces(faceQuads, faceSkipped);
        }
        return largestIndexCount;
    }

    static int[] currentDraws(SectionRenderDispatcher.RenderSection section, SectionRenderDispatcher dispatcher) {
        SectionDrawRecordHolder holder = (SectionDrawRecordHolder) section;
        int meshVersion = ((MeshVersionHolder) section).aervium$meshVersion();
        int[] packed = holder.aervium$packedDraws();
        if (packed != null && SectionDrawRecord.current(packed, meshVersion)) {
            return packed;
        }
        SectionMesh mesh = section.getSectionMesh();
        SectionDrawRecord record = SectionDrawRecord.build(mesh, meshVersion, section.getRenderOrigin(), dispatcher);
        holder.aervium$setDrawRecord(record);
        if (mesh instanceof SectionSliceHolder slices) {
            slices.aervium$trackRecord(record);
        }
        return record.packedDraws();
    }

    private static int addRecord(List<DynamicGpuData.ChunkSectionInfo> sectionInfos, int originX, int originY, int originZ, float visibility) {
        int index = sectionInfos.size();
        sectionInfos.add(new DynamicGpuData.ChunkSectionInfo(originX, originY, originZ, visibility));
        return index;
    }

    private static int addFacingDraws(
        PackedDraws draws, int[] packed, int faces, int firstIndex, int baseVertex, int visible, int sectionInfoDataIndex
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
                    draws.add(quads * INDICES_PER_QUAD, firstIndex + firstQuad * INDICES_PER_QUAD, baseVertex, sectionInfoDataIndex);
                }
                runStart = -1;
            }
        }
        return drawn;
    }
}
