package com.aryston.aervium.client.chunk;

import com.aryston.aervium.Aervium;
import com.aryston.aervium.core.job.JobSystem;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import java.util.concurrent.CompletableFuture;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.lighting.ChunkSkyLightSources;
import org.jspecify.annotations.Nullable;

public final class ChunkPacketDecoding {
    private static final int WORKERS = 2;
    private static final JobSystem JOBS = new JobSystem(
        "Aervium Chunk Decode",
        WORKERS,
        (thread, failure) -> Aervium.LOGGER.error("Aervium chunk decode worker {} failed", thread.getName(), failure)
    );

    private static final boolean AUDIT = Boolean.getBoolean("aervium.chunkDecodeAudit");
    private static final int COLUMNS_PER_SIDE = 16;
    private static final int AUDIT_LOG_INTERVAL = 256;

    private static @Nullable DecodedChunk applying;
    private static int auditedChunks;
    private static int auditedMismatches;
    private static int auditedMisses;
    private static boolean failureLogged;

    private ChunkPacketDecoding() {
    }

    public static void start(@Nullable Level level, ClientboundLevelChunkPacketData data, int chunkX, int chunkZ) {
        if (level == null) {
            return;
        }
        CompletableFuture<DecodedChunk> decoded = new CompletableFuture<>();
        ((DecodedChunkHolder) data).aervium$setDecoded(decoded);
        JOBS.submit(() -> {
            try {
                decoded.complete(DecodedChunk.decode(level, data, chunkX, chunkZ));
            } catch (RuntimeException failure) {
                decoded.completeExceptionally(failure);
            }
        });
    }

    public static void replaceFresh(LevelChunk chunk, int chunkX, int chunkZ, ClientboundLevelChunkPacketData data, Operation<Void> original) {
        DecodedChunk decoded = ready(chunk, data);
        applying = decoded;
        try {
            original.call(chunk, chunkX, chunkZ, data);
        } finally {
            applying = null;
        }
        if (AUDIT) {
            audit(chunk, decoded != null);
        }
    }

    public static @Nullable DecodedChunk applying() {
        return applying;
    }

    private static @Nullable DecodedChunk ready(LevelChunk chunk, ClientboundLevelChunkPacketData data) {
        CompletableFuture<DecodedChunk> decoded = ((DecodedChunkHolder) data).aervium$decoded();
        if (decoded == null || !decoded.isDone()) {
            return null;
        }
        if (decoded.isCompletedExceptionally()) {
            logFailure(decoded);
            return null;
        }
        DecodedChunk result = decoded.join();
        boolean matches = result.level() == chunk.getLevel() && result.sections().length == chunk.getSections().length;
        return matches ? result : null;
    }

    private static void audit(LevelChunk chunk, boolean decodedAhead) {
        if (!decodedAhead) {
            auditedMisses++;
            return;
        }
        ChunkSkyLightSources vanilla = new ChunkSkyLightSources(chunk);
        vanilla.fillFrom(chunk);
        for (int z = 0; z < COLUMNS_PER_SIDE; z++) {
            for (int x = 0; x < COLUMNS_PER_SIDE; x++) {
                if (vanilla.getLowestSourceY(x, z) != chunk.getSkyLightSources().getLowestSourceY(x, z)) {
                    auditedMismatches++;
                }
            }
        }
        auditedChunks++;
        if (auditedChunks % AUDIT_LOG_INTERVAL == 0) {
            Aervium.LOGGER.info("Aervium chunk decode audit: {} chunks decoded ahead, {} built without, {} columns differ from vanilla", auditedChunks, auditedMisses, auditedMismatches);
        }
    }

    private static void logFailure(CompletableFuture<DecodedChunk> decoded) {
        if (failureLogged) {
            return;
        }
        failureLogged = true;
        decoded.exceptionally(failure -> {
            Aervium.LOGGER.warn("Aervium could not decode a chunk packet ahead; the game decodes it as usual", failure);
            return null;
        });
    }
}
