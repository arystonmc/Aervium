package com.aryston.kinetic.world.spawn;

import com.aryston.kinetic.Kinetic;
import com.aryston.kinetic.core.telemetry.Durations;
import java.util.concurrent.CompletableFuture;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkLoadCounter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.level.ChunkPos;
import org.jspecify.annotations.Nullable;

public final class SpawnPregeneration {
    private static final int SENDABLE_MARGIN = 1;

    private final ServerLevel level;
    private final ChunkPos center;
    private final int radius;
    private final int ticketRadius;
    private final ChunkLoadCounter counter = new ChunkLoadCounter();
    private final long startedAt = System.nanoTime();
    private CompletableFuture<?> loaded = CompletableFuture.completedFuture(null);
    private boolean reported;

    private SpawnPregeneration(ServerLevel level, ChunkPos center, int radius) {
        this.level = level;
        this.center = center;
        this.radius = radius;
        this.ticketRadius = radius + SENDABLE_MARGIN;
    }

    public static @Nullable SpawnPregeneration start(MinecraftServer server, NameAndId player, ServerLevel level, ChunkPos center) {
        if (!SpawnPregenerationSettings.enabled() || server.isDedicatedServer() || !server.isSingleplayerOwner(player)) {
            return null;
        }
        int radius = SpawnPregenerationSettings.joinRadius();
        if (radius <= SpawnPregenerationSettings.VANILLA_RADIUS) {
            return null;
        }
        SpawnPregeneration pregeneration = new SpawnPregeneration(level, center, radius);
        pregeneration.counter.track(level, pregeneration::addTicket);
        Kinetic.LOGGER.info("Kinetic pregenerates {} chunks within {} chunks of the spawn in {}",
            pregeneration.counter.totalChunks(), radius, level.dimension().identifier());
        return pregeneration;
    }

    private void addTicket() {
        loaded = level.getChunkSource().addTicketAndLoadWithRadius(TicketType.PLAYER_SPAWN, center, ticketRadius);
    }

    public void keepAlive() {
        level.getChunkSource().addTicketWithRadius(TicketType.PLAYER_SPAWN, center, ticketRadius);
    }

    public boolean isDone() {
        boolean done = loaded.isDone();
        if (done && !reported) {
            reported = true;
            Kinetic.LOGGER.info("Kinetic pregenerated {} chunks within {} chunks of the spawn in {} ms",
                counter.totalChunks(), radius, Math.round(Durations.millis(System.nanoTime() - startedAt)));
        }
        return done;
    }

    public int readyChunks() {
        return counter.readyChunks();
    }

    public int totalChunks() {
        return counter.totalChunks();
    }
}
