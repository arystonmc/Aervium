package com.aryston.kinetic.client.lod;

import com.aryston.kinetic.Kinetic;
import com.aryston.kinetic.core.lod.LodStore;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Consumer;

final class TerrainWorker {
    private static final String NAME = "Kinetic Distant Terrain";
    private static final LodStore STORE = new LodStore();
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, NAME);
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.setUncaughtExceptionHandler((failed, failure) -> Kinetic.LOGGER.error("Kinetic distant terrain worker failed", failure));
        return thread;
    });

    private TerrainWorker() {
    }

    static void submit(Consumer<LodStore> work) {
        try {
            WORKER.execute(() -> work.accept(STORE));
        } catch (RejectedExecutionException failure) {
            if (!WORKER.isShutdown()) {
                throw failure;
            }
        }
    }

    static void shutdown() {
        WORKER.shutdown();
    }
}
