package com.aryston.aervium.client.lod;

import com.aryston.aervium.Aervium;
import com.aryston.aervium.core.lod.GenerationPower;
import com.aryston.aervium.core.lod.LodData;
import com.aryston.aervium.core.lod.LodPlanner;
import com.aryston.aervium.core.lod.LodQuality;
import com.aryston.aervium.core.lod.LodSelection;
import com.aryston.aervium.core.lod.LodStore;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import org.jspecify.annotations.Nullable;

final class TerrainGenerator {
    private static final String NAME = "Aervium Terrain Generator";
    private static final int CORES = Runtime.getRuntime().availableProcessors();
    private static final int POOL_THREADS = GenerationPower.EXTREME.threads(CORES);
    private static final long FAILURE_RETRY_NANOS = 5_000_000_000L;
    private static final long IDLE_SLICE_NANOS = 10_000_000L;

    private static final LodPlanner PLANNER = new LodPlanner();
    private static final LongOpenHashSet REQUESTED = new LongOpenHashSet();
    private static final AtomicInteger IN_FLIGHT = new AtomicInteger();
    private static final AtomicInteger GENERATED = new AtomicInteger();
    private static final AtomicInteger EPOCH = new AtomicInteger();
    private static final AtomicBoolean PLAN_PENDING = new AtomicBoolean();
    private static final ExecutorService POOL = Executors.newFixedThreadPool(POOL_THREADS, runnable -> {
        Thread thread = new Thread(runnable, NAME);
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.setUncaughtExceptionHandler((failed, failure) -> Aervium.LOGGER.error("Aervium terrain generator failed", failure));
        return thread;
    });

    private static volatile @Nullable GeneratedColumns source;
    private static volatile GenerationPower power = GenerationPower.BALANCED;
    private static volatile long retryAfter;

    private TerrainGenerator() {
    }

    static void bind(@Nullable GeneratedColumns columns) {
        source = columns;
        EPOCH.incrementAndGet();
        GENERATED.set(0);
        retryAfter = 0L;
        TerrainWorker.submit(store -> REQUESTED.clear());
    }

    static void shutdown() {
        source = null;
        EPOCH.incrementAndGet();
        POOL.shutdownNow();
    }

    static void setPower(GenerationPower value) {
        power = value;
    }

    static boolean active() {
        return source != null;
    }

    static int generatedTiles() {
        return GENERATED.get();
    }

    static int inFlight() {
        return IN_FLIGHT.get();
    }

    static void request(double cameraX, double cameraZ, double distance, LodSelection.ClipBox clip, LodQuality quality) {
        GeneratedColumns columns = source;
        int capacity = power.threads(CORES) - IN_FLIGHT.get();
        if (columns == null || capacity <= 0 || System.nanoTime() < retryAfter || !PLAN_PENDING.compareAndSet(false, true)) {
            return;
        }
        int epoch = EPOCH.get();
        TerrainWorker.submit(store -> {
            try {
                if (EPOCH.get() != epoch) {
                    return;
                }
                int available = Math.min(capacity, power.threads(CORES) - IN_FLIGHT.get());
                if (available <= 0) {
                    return;
                }
                long[] keys = PLANNER.plan(cameraX, cameraZ, distance, clip, quality, key -> !REQUESTED.contains(key) && !store.complete(key), available);
                for (long key : keys) {
                    REQUESTED.add(key);
                    IN_FLIGHT.incrementAndGet();
                    try {
                        POOL.execute(() -> generate(columns, key, quality.generatedGrid(), epoch));
                    } catch (RejectedExecutionException failure) {
                        IN_FLIGHT.decrementAndGet();
                        REQUESTED.remove(key);
                        if (!POOL.isShutdown()) {
                            throw failure;
                        }
                    }
                }
            } finally {
                PLAN_PENDING.set(false);
            }
        });
    }

    private static void generate(GeneratedColumns columns, long key, int grid, int epoch) {
        long started = System.nanoTime();
        try {
            if (EPOCH.get() != epoch) {
                return;
            }
            int level = LodStore.levelOf(key);
            int tileX = LodStore.tileXOf(key);
            int tileZ = LodStore.tileZOf(key);
            LodData data = columns.tile(level, tileX, tileZ, grid, () -> EPOCH.get() != epoch);
            TerrainWorker.submit(store -> {
                if (EPOCH.get() != epoch) {
                    return;
                }
                store.fillTile(level, tileX, tileZ, data);
                store.downsamplePending(Integer.MAX_VALUE);
                GENERATED.incrementAndGet();
                DistantTerrain.recorded(store);
                TileMeshing.tilesChanged(store);
            });
        } catch (CancellationException failure) {
            return;
        } catch (RuntimeException failure) {
            if (EPOCH.get() == epoch) {
                retryAfter = System.nanoTime() + FAILURE_RETRY_NANOS;
                Aervium.LOGGER.warn("Aervium distant terrain generation paused after a failed tile", failure);
            }
        } finally {
            idle(started, epoch);
            IN_FLIGHT.decrementAndGet();
            TerrainWorker.submit(store -> {
                if (EPOCH.get() == epoch) {
                    REQUESTED.remove(key);
                }
            });
        }
    }

    private static void idle(long started, int epoch) {
        long deadline = System.nanoTime() + power.idleNanos(System.nanoTime() - started);
        while (EPOCH.get() == epoch && !Thread.currentThread().isInterrupted()) {
            long remaining = deadline - System.nanoTime();
            if (remaining <= 0L) {
                return;
            }
            LockSupport.parkNanos(Math.min(remaining, IDLE_SLICE_NANOS));
        }
    }
}
