package com.aryston.aervium.client.lod;

import com.aryston.aervium.Aervium;
import com.aryston.aervium.core.lod.DistantTerrainLimits;
import com.aryston.aervium.core.lod.GenerationPower;
import com.aryston.aervium.core.lod.LodQuality;
import com.aryston.aervium.core.lod.LodStore;
import com.aryston.aervium.core.lod.LodLayers;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;

public final class DistantTerrain {
    private static final long CAPTURE_BUDGET_NANOS = 1_000_000L;
    private static final int COLUMNS = LodStore.CHUNK_SIZE * LodStore.CHUNK_SIZE;
    private static final long BYTES_PER_MEGABYTE = 1024L * 1024L;
    private static final float FAR_PLANE_MARGIN = 1.5F;
    private static final long TRIM_INTERVAL_NANOS = 1_000_000_000L;
    private static final int MAX_PENDING_CAPTURES = 32;
    private static final int DISCOVERY_PER_TICK = 16;

    private static final LongLinkedOpenHashSet QUEUED = new LongLinkedOpenHashSet();
    private static final AtomicInteger PENDING_CAPTURES = new AtomicInteger();

    private static volatile boolean enabled;
    private static volatile int distanceChunks = DistantTerrainLimits.DEFAULT_CHUNKS;
    private static volatile LodQuality quality = LodQuality.HIGH;
    private static volatile Statistics statistics = Statistics.NONE;
    private static @Nullable ClientLevel capturedLevel;
    private static @Nullable ColumnSampler sampler;
    private static volatile int capturedChunks;
    private static long lastTrim;
    private static @Nullable Capture capturing;
    private static int discoveryCursor;
    private static int discoveryWidth;
    private static int discoveryMinX;
    private static int discoveryMinZ;
    private static int lastCameraChunkX = Integer.MIN_VALUE;
    private static int lastCameraChunkZ = Integer.MIN_VALUE;
    private static int lastDiscoveryRadius = -1;

    private DistantTerrain() {
    }

    public static void setEnabled(boolean value) {
        enabled = value;
        if (!value) {
            reset();
        }
    }

    public static boolean enabled() {
        return enabled;
    }

    public static void setDistanceChunks(int chunks) {
        distanceChunks = chunks;
    }

    public static void setQuality(LodQuality value) {
        if (quality != value) {
            quality = value;
            resetTiles();
        }
    }

    public static LodQuality quality() {
        return quality;
    }

    public static void setPower(GenerationPower value) {
        TerrainGenerator.setPower(value);
    }

    public static boolean drawing() {
        return enabled;
    }

    public static float distanceBlocks() {
        return distanceChunks * (float) LodStore.CHUNK_SIZE;
    }

    public static float farPlaneBlocks() {
        return distanceBlocks() * FAR_PLANE_MARGIN;
    }

    public static Statistics statistics() {
        return statistics;
    }

    public static void chunkLoaded(LevelChunk chunk) {
        if (enabled) {
            DistantTerrainCoverage.loaded(chunk.getPos().pack());
            QUEUED.add(chunk.getPos().pack());
        }
    }

    public static void chunkChanged(int x, int z) {
        if (enabled) {
            QUEUED.add(ChunkPos.pack(x, z));
        }
    }

    public static void chunkUnloaded(int x, int z) {
        QUEUED.remove(ChunkPos.pack(x, z));
        DistantTerrainCoverage.forget(x, z);
    }

    public static void resourcesReloaded() {
        reset();
        TerrainMaterials.invalidate();
    }

    public static void shutdown() {
        enabled = false;
        reset();
        TerrainGenerator.shutdown();
        DistantTerrainRenderer.shutdown();
        TerrainWorker.shutdown();
    }

    public static void clientTick(Minecraft minecraft) {
        if (!enabled) {
            return;
        }
        TerrainMaterials.prepare(minecraft);
        ClientLevel level = minecraft.level;
        if (level == null || !TerrainMaterials.ready()) {
            return;
        }
        if (level != capturedLevel) {
            long[] waiting = QUEUED.toLongArray();
            reset();
            for (long packed : waiting) {
                DistantTerrainCoverage.loaded(packed);
                QUEUED.add(packed);
            }
            capturedLevel = level;
            sampler = new ColumnSampler(minecraft.getBlockColors());
            TerrainGenerator.bind(generatedColumns(minecraft, level));
            lastCameraChunkX = Integer.MIN_VALUE;
            lastCameraChunkZ = Integer.MIN_VALUE;
            lastDiscoveryRadius = -1;
        }
        var camera = minecraft.gameRenderer.mainCamera().position();
        int radius = minecraft.options.getEffectiveRenderDistance();
        int cameraChunkX = Math.floorDiv((int) Math.floor(camera.x), LodStore.CHUNK_SIZE);
        int cameraChunkZ = Math.floorDiv((int) Math.floor(camera.z), LodStore.CHUNK_SIZE);
        if (cameraChunkX != lastCameraChunkX || cameraChunkZ != lastCameraChunkZ || radius != lastDiscoveryRadius) {
            lastCameraChunkX = cameraChunkX;
            lastCameraChunkZ = cameraChunkZ;
            lastDiscoveryRadius = radius;
            discoveryWidth = radius * 2 + 1;
            discoveryMinX = cameraChunkX - radius;
            discoveryMinZ = cameraChunkZ - radius;
            discoveryCursor = 0;
        }
        trim(minecraft);
        long deadline = System.nanoTime() + CAPTURE_BUDGET_NANOS;
        discover(level, deadline);
        ColumnSampler columns = sampler;
        if (columns == null) {
            return;
        }
        while ((capturing != null || !QUEUED.isEmpty()) && System.nanoTime() < deadline) {
            if (capturing == null) {
                if (PENDING_CAPTURES.get() >= MAX_PENDING_CAPTURES) {
                    return;
                }
                long packed = QUEUED.removeFirstLong();
                LevelChunk chunk = level.getChunkSource().getChunk(ChunkPos.getX(packed), ChunkPos.getZ(packed), false);
                if (chunk == null) {
                    continue;
                }
                capturing = new Capture(chunk, columns.view(chunk));
            }
            Capture current = capturing;
            ChunkPos pos = current.chunk.getPos();
            if (level.getChunkSource().getChunk(pos.x(), pos.z(), false) != current.chunk) {
                capturing = null;
                continue;
            }
            int index = current.next;
            int x = index % LodStore.CHUNK_SIZE;
            int z = index / LodStore.CHUNK_SIZE;
            current.columns[index] = current.view.sample(x, z);
            current.view.layers(x, z, current.layers);
            current.layers.finishColumn();
            current.next++;
            if (current.next == COLUMNS) {
                captured(current);
                capturing = null;
            }
        }
    }

    public static void reset() {
        Statistics recorded = statistics;
        if (recorded.tiles() > 0) {
            Aervium.LOGGER.info("Aervium distant terrain: {} chunks recorded, {} tiles generated, {} tiles, {} MB of samples",
                recorded.chunks(), recorded.generated(), recorded.tiles(), recorded.bytes() / BYTES_PER_MEGABYTE);
        }
        QUEUED.clear();
        DistantTerrainCoverage.clear();
        capturedLevel = null;
        sampler = null;
        capturedChunks = 0;
        lastTrim = 0L;
        capturing = null;
        discoveryCursor = 0;
        discoveryWidth = 0;
        TerrainGenerator.bind(null);
        TileMeshing.invalidate();
        TerrainWorker.submit(store -> {
            store.clear();
            TileMeshing.forgetAll();
            recorded(store);
        });
    }

    private static void resetTiles() {
        TerrainGenerator.bind(null);
        TileMeshing.invalidate();
        TerrainWorker.submit(store -> {
            store.clear();
            TileMeshing.forgetAll();
            recorded(store);
        });
        if (capturedLevel != null) {
            TerrainGenerator.bind(generatedColumns(Minecraft.getInstance(), capturedLevel));
        }
    }

    static void recorded(LodStore store) {
        statistics = new Statistics(store.tileCount(), capturedChunks, TerrainGenerator.generatedTiles(), store.sampleBytes());
    }

    private static void trim(Minecraft minecraft) {
        long now = System.nanoTime();
        if (now - lastTrim < TRIM_INTERVAL_NANOS) {
            return;
        }
        lastTrim = now;
        var camera = minecraft.gameRenderer.mainCamera().position();
        double cameraX = camera.x;
        double cameraZ = camera.z;
        double distance = distanceBlocks();
        int epoch = TileMeshing.epoch();
        TerrainWorker.submit(store -> {
            if (epoch == TileMeshing.epoch()) {
                store.trim(cameraX, cameraZ, distance, DistantTerrainLimits.MAX_RESIDENT_TILES);
                recorded(store);
                TileMeshing.tilesChanged(store);
            }
        });
    }

    private static @Nullable GeneratedColumns generatedColumns(Minecraft minecraft, ClientLevel level) {
        IntegratedServer server = minecraft.getSingleplayerServer();
        if (server == null) {
            return null;
        }
        ServerLevel serverLevel = server.getLevel(level.dimension());
        return serverLevel == null ? null : GeneratedColumns.of(serverLevel);
    }

    private static void discover(ClientLevel level, long deadline) {
        for (int count = 0; count < DISCOVERY_PER_TICK && discoveryCursor < discoveryWidth * discoveryWidth && System.nanoTime() < deadline; count++) {
            int x = discoveryMinX + discoveryCursor % discoveryWidth;
            int z = discoveryMinZ + discoveryCursor / discoveryWidth;
            discoveryCursor++;
            LevelChunk chunk = level.getChunkSource().getChunk(x, z, false);
            if (chunk != null) {
                DistantTerrainCoverage.loaded(chunk.getPos().pack());
                QUEUED.add(chunk.getPos().pack());
            }
        }
    }

    private static void captured(Capture capture) {
        int chunkX = capture.chunk.getPos().x();
        int chunkZ = capture.chunk.getPos().z();
        long[] copied = capture.columns;
        LodLayers layers = capture.layers.build();
        DistantTerrainCoverage.captured(chunkX, chunkZ, copied);
        int epoch = TileMeshing.epoch();
        capturedChunks++;
        PENDING_CAPTURES.incrementAndGet();
        TerrainWorker.submit(store -> {
            try {
                if (epoch != TileMeshing.epoch()) {
                    return;
                }
                store.putChunk(chunkX, chunkZ, copied, layers);
                store.downsamplePending(Integer.MAX_VALUE);
                recorded(store);
                TileMeshing.tilesChanged(store);
            } finally {
                PENDING_CAPTURES.decrementAndGet();
            }
        });
    }

    private static final class Capture {
        private final LevelChunk chunk;
        private final SavedColumnView view;
        private final long[] columns = new long[COLUMNS];
        private final LodLayers.Builder layers = new LodLayers.Builder(COLUMNS);
        private int next;

        private Capture(LevelChunk chunk, SavedColumnView view) {
            this.chunk = chunk;
            this.view = view;
        }
    }

    public record Statistics(int tiles, int chunks, int generated, long bytes) {
        static final Statistics NONE = new Statistics(0, 0, 0, 0L);
    }
}
