package com.aryston.aervium.client.particle;

import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class ParticleCollisions {
    private static final double EDGE = 1.0E-7;
    private static final int NEIGHBOUR_BLOCKS = 1;
    private static final int MAX_CELLS = 256;
    private static final int SECTION_MASK = 15;

    private ParticleCollisions() {
    }

    public static boolean throughAirOnly(Level level, AABB box, Vec3 movement) {
        AABB swept = box.expandTowards(movement);
        int minX = Mth.floor(swept.minX - EDGE) - NEIGHBOUR_BLOCKS;
        int maxX = Mth.floor(swept.maxX + EDGE) + NEIGHBOUR_BLOCKS;
        int minY = Math.max(level.getMinY(), Mth.floor(swept.minY - EDGE) - NEIGHBOUR_BLOCKS);
        int maxY = Math.min(level.getMaxY(), Mth.floor(swept.maxY + EDGE) + NEIGHBOUR_BLOCKS);
        int minZ = Mth.floor(swept.minZ - EDGE) - NEIGHBOUR_BLOCKS;
        int maxZ = Mth.floor(swept.maxZ + EDGE) + NEIGHBOUR_BLOCKS;
        if ((long) (maxX - minX + 1) * Math.max(0, maxY - minY + 1) * (maxZ - minZ + 1) > MAX_CELLS) {
            return false;
        }
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                ChunkAccess chunk = level.getChunkSource().getChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z), ChunkStatus.FULL, false);
                if (chunk == null || !columnIsAir(level, chunk, x, minY, maxY, z)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean columnIsAir(Level level, ChunkAccess chunk, int x, int minY, int maxY, int z) {
        for (int y = minY; y <= maxY; y++) {
            LevelChunkSection section = chunk.getSection(level.getSectionIndex(y));
            if (!section.hasOnlyAir() && !section.getBlockState(x & SECTION_MASK, y & SECTION_MASK, z & SECTION_MASK).isAir()) {
                return false;
            }
        }
        return true;
    }
}
