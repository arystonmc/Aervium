package com.aryston.aervium.client.chunk;

import com.aryston.aervium.client.gpu.GpuCulling;
import com.aryston.aervium.client.gpu.GpuDrawList;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import java.util.List;
import net.minecraft.client.renderer.DynamicGpuData;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.jspecify.annotations.Nullable;

public final class DirectDraws {
    private static final PackedDrawPool POOL = new PackedDrawPool();
    private static final PackedSectionInfos SECTIONS = new PackedSectionInfos();
    private static final DrawCommandRing COMMANDS = new DrawCommandRing();
    private static final SectionInfoStorage SECTION_STORAGE = new SectionInfoStorage();

    private static boolean indirectPass;
    private static boolean packedSections;
    private static @Nullable CameraRenderState cameraPass;

    private DirectDraws() {
    }

    public static void beginPass(boolean indirect, @Nullable CameraRenderState camera) {
        indirectPass = indirect;
        cameraPass = camera;
        packedSections = false;
    }

    static boolean beginExtract() {
        POOL.reset();
        SECTIONS.clear();
        packedSections = indirectPass;
        return packedSections;
    }

    static @Nullable CameraRenderState cameraPass() {
        return cameraPass;
    }

    static PackedDraws nextDraws() {
        return POOL.next();
    }

    static int addSection(int x, int y, int z, float visibility) {
        return SECTIONS.add(x, y, z, visibility);
    }

    public static GpuBufferSlice[] writeCommands(DynamicGpuData data, List<List<DynamicGpuData.IndexedDraw>> lists) {
        for (List<DynamicGpuData.IndexedDraw> draws : lists) {
            if (!PackedDraws.packed(draws) && !(draws instanceof GpuDrawList)) {
                return data.writeChunkSectionCommands(lists);
            }
        }
        return COMMANDS.write(lists);
    }

    public static GpuBufferSlice writeSectionInfos(DynamicGpuData data, List<DynamicGpuData.ChunkSectionInfo> infos) {
        if (!packedSections || !infos.isEmpty()) {
            return data.writeChunkSectionsInstanced(infos);
        }
        packedSections = false;
        return SECTION_STORAGE.write(SECTIONS, GpuCulling.instanceSlots());
    }

    public static void endFrame() {
        COMMANDS.endFrame();
        SECTION_STORAGE.endFrame();
        GpuCulling.endFrame();
    }
}
