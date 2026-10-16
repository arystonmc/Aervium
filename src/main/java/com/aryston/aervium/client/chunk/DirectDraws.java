package com.aryston.aervium.client.chunk;

import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import java.util.List;
import net.minecraft.client.renderer.DynamicGpuData;

public final class DirectDraws {
    private static final PackedDrawPool POOL = new PackedDrawPool();
    private static final PackedSectionInfos SECTIONS = new PackedSectionInfos();
    private static final DrawCommandRing COMMANDS = new DrawCommandRing();
    private static final SectionInfoStorage SECTION_STORAGE = new SectionInfoStorage();

    private static boolean indirectPass;
    private static boolean packedSections;

    private DirectDraws() {
    }

    public static void beginPass(boolean indirect) {
        indirectPass = indirect;
        packedSections = false;
    }

    static boolean beginExtract() {
        POOL.reset();
        SECTIONS.clear();
        packedSections = indirectPass;
        return packedSections;
    }

    static PackedDraws nextDraws() {
        return POOL.next();
    }

    static int addSection(int x, int y, int z, float visibility) {
        return SECTIONS.add(x, y, z, visibility);
    }

    public static GpuBufferSlice[] writeCommands(DynamicGpuData data, List<List<DynamicGpuData.IndexedDraw>> lists) {
        for (List<DynamicGpuData.IndexedDraw> draws : lists) {
            if (!PackedDraws.packed(draws)) {
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
        return SECTION_STORAGE.write(SECTIONS);
    }

    public static void endFrame() {
        COMMANDS.endFrame();
        SECTION_STORAGE.endFrame();
    }
}
