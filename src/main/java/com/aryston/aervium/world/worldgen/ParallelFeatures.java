package com.aryston.aervium.world.worldgen;

import com.aryston.aervium.Aervium;
import com.aryston.aervium.core.job.JobSystem;
import com.aryston.aervium.core.job.RegionLocks;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;

public final class ParallelFeatures {
    private static final int WRITE_RADIUS = 1;
    private static final JobSystem JOBS = new JobSystem(
        "Aervium Worldgen",
        JobSystem.defaultWorkers(Runtime.getRuntime().availableProcessors()),
        (thread, failure) -> Aervium.LOGGER.error("Aervium feature generation failed on {}", thread.getName(), failure)
    );
    private static final RegionLocks LOCKS = new RegionLocks();

    private ParallelFeatures() {
    }

    public static CompletableFuture<ChunkAccess> place(ChunkPos pos, Supplier<CompletableFuture<ChunkAccess>> features) {
        return CompletableFuture.supplyAsync(() -> {
            LOCKS.acquire(pos.x(), pos.z(), WRITE_RADIUS);
            try {
                return features.get().join();
            } finally {
                LOCKS.release(pos.x(), pos.z(), WRITE_RADIUS);
            }
        }, JOBS.service());
    }
}
