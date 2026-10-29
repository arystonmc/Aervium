package com.aryston.kinetic.client.lod;

import com.aryston.kinetic.core.lod.LodBorders;
import com.aryston.kinetic.core.lod.LodMesh;
import com.aryston.kinetic.core.lod.LodMesher;
import com.aryston.kinetic.core.lod.LodStore;
import com.aryston.kinetic.core.lod.LodTile;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.jspecify.annotations.Nullable;

final class TileMeshing {
    private static final long PUBLISH_INTERVAL_NANOS = 250_000_000L;
    private static final int MESHES_PER_REQUEST = 48;
    private static final int NOT_MESHED = -1;
    private static final int READY_CAPACITY = 48;
    private static final int READY_BYTES_LIMIT = 32 * 1024 * 1024;

    private static final Long2IntOpenHashMap MESHED_VERSIONS = new Long2IntOpenHashMap();
    private static final Long2ObjectOpenHashMap<LodBorders> MESHED_BORDERS = new Long2ObjectOpenHashMap<>();
    private static final ArrayBlockingQueue<ReadyMesh> READY = new ArrayBlockingQueue<>(READY_CAPACITY);
    private static final AtomicBoolean REQUEST_PENDING = new AtomicBoolean();
    private static final AtomicInteger EPOCH = new AtomicInteger();

    private static volatile LongOpenHashSet existing = new LongOpenHashSet();
    private static long lastPublish;
    private static volatile int readyBytes;

    static {
        MESHED_VERSIONS.defaultReturnValue(NOT_MESHED);
    }

    private TileMeshing() {
    }

    static boolean exists(long key) {
        return existing.contains(key);
    }

    static @Nullable LodMesh nextReady() {
        synchronized (READY) {
            ReadyMesh ready;
            while ((ready = READY.poll()) != null) {
                readyBytes -= ready.mesh().vertices().remaining();
                if (ready.epoch() == EPOCH.get()) {
                    return ready.mesh();
                }
            }
            return null;
        }
    }

    static int epoch() {
        return EPOCH.get();
    }

    static void invalidate() {
        synchronized (READY) {
            EPOCH.incrementAndGet();
            READY.clear();
            readyBytes = 0;
        }
        existing = new LongOpenHashSet();
    }

    static void request(long[] keys) {
        if (!REQUEST_PENDING.compareAndSet(false, true)) {
            return;
        }
        int epoch = EPOCH.get();
        TerrainWorker.submit(store -> {
            try {
                if (epoch == EPOCH.get()) {
                    meshChanged(store, keys, epoch);
                    publish(store);
                }
            } finally {
                REQUEST_PENDING.set(false);
            }
        });
    }

    static void forget(long key) {
        TerrainWorker.submit(store -> {
            MESHED_VERSIONS.remove(key);
            MESHED_BORDERS.remove(key);
        });
    }

    static void tilesChanged(LodStore store) {
        if (System.nanoTime() - lastPublish >= PUBLISH_INTERVAL_NANOS) {
            publish(store);
        }
    }

    static void forgetAll() {
        MESHED_VERSIONS.clear();
        MESHED_BORDERS.clear();
        synchronized (READY) {
            READY.clear();
            readyBytes = 0;
        }
        existing = new LongOpenHashSet();
    }

    private static void meshChanged(LodStore store, long[] keys, int epoch) {
        int meshed = 0;
        for (long key : keys) {
            if (meshed >= MESHES_PER_REQUEST || READY.remainingCapacity() == 0 || readyBytes >= READY_BYTES_LIMIT || epoch != EPOCH.get()) {
                return;
            }
            LodTile tile = store.tile(LodStore.levelOf(key), LodStore.tileXOf(key), LodStore.tileZOf(key));
            if (tile == null || tile.present() == 0 || (TerrainGenerator.active() && !tile.complete())) {
                continue;
            }
            LodBorders borders = store.borders(tile);
            LodBorders previousBorders = MESHED_BORDERS.get(key);
            if (MESHED_VERSIONS.get(key) == tile.version() && previousBorders != null && previousBorders.matches(borders)) {
                continue;
            }
            LodMesh mesh = LodMesher.mesh(key, tile, borders);
            synchronized (READY) {
                int bytes = mesh.vertices().remaining();
                if (epoch != EPOCH.get() || readyBytes + bytes > READY_BYTES_LIMIT || !READY.offer(new ReadyMesh(mesh, epoch))) {
                    return;
                }
                readyBytes += bytes;
            }
            MESHED_VERSIONS.put(key, tile.version());
            MESHED_BORDERS.put(key, borders);
            meshed++;
        }
    }

    private static void publish(LodStore store) {
        existing = store.copyKeys();
        MESHED_VERSIONS.keySet().removeIf(key -> !existing.contains(key));
        MESHED_BORDERS.keySet().removeIf(key -> !existing.contains(key));
        lastPublish = System.nanoTime();
    }

    private record ReadyMesh(LodMesh mesh, int epoch) {
    }
}
