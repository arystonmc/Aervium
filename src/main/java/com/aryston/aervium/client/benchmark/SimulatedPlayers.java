package com.aryston.aervium.client.benchmark;

import com.aryston.aervium.debug.benchmark.BlockPoint;
import com.aryston.aervium.debug.benchmark.SimulatedPlayerRoutes;
import com.aryston.aervium.debug.benchmark.SimulatedPlayerRoutes.ChunkPoint;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;

final class SimulatedPlayers {
    private SimulatedPlayers() {
    }

    static void step(IntegratedServer server, BlockPoint spawn, int tick) {
        if (!SimulatedPlayerRoutes.moves(tick)) {
            return;
        }
        int spawnChunkX = SectionPos.blockToSectionCoord(spawn.x());
        int spawnChunkZ = SectionPos.blockToSectionCoord(spawn.z());
        server.execute(() -> {
            ServerChunkCache chunks = server.overworld().getChunkSource();
            for (int player = 0; player < SimulatedPlayerRoutes.PLAYERS; player++) {
                ChunkPoint position = SimulatedPlayerRoutes.position(player, tick, spawnChunkX, spawnChunkZ);
                chunks.addTicketWithRadius(TicketType.ENDER_PEARL, new ChunkPos(position.x(), position.z()), SimulatedPlayerRoutes.VIEW_RADIUS_CHUNKS);
            }
        });
    }
}
