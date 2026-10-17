package com.aryston.aervium.client.chunk;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.lighting.ChunkSkyLightSources;
import net.minecraft.world.ticks.ProtoChunkTicks;

public record DecodedChunk(Level level, LevelChunkSection[] sections, ChunkSkyLightSources skyLightSources) {
    static DecodedChunk decode(Level level, ClientboundLevelChunkPacketData data, int chunkX, int chunkZ) {
        LevelChunkSection[] sections = new LevelChunkSection[level.getSectionsCount()];
        FriendlyByteBuf buffer = data.getReadBuffer();
        for (int index = 0; index < sections.length; index++) {
            LevelChunkSection section = new LevelChunkSection(level.palettedContainerFactory());
            section.read(buffer);
            sections[index] = section;
        }
        ProtoChunk chunk = new ProtoChunk(
            new ChunkPos(chunkX, chunkZ), UpgradeData.EMPTY, sections, new ProtoChunkTicks<>(), new ProtoChunkTicks<>(), level, level.palettedContainerFactory(), null
        );
        chunk.initializeLightSources();
        return new DecodedChunk(level, sections, chunk.getSkyLightSources());
    }
}
