package com.aryston.aervium.client.chunk;

import com.aryston.aervium.Aervium;
import com.aryston.aervium.core.job.JobLoadMeter;
import com.aryston.aervium.core.job.JobSystem;
import net.minecraft.TracingExecutor;

public final class MeshWorkers {
    private static final JobSystem JOBS = new JobSystem(
        "Aervium Mesh",
        JobSystem.defaultWorkers(Runtime.getRuntime().availableProcessors()),
        Thread.NORM_PRIORITY,
        (thread, failure) -> Aervium.LOGGER.error("Aervium chunk mesh worker {} failed", thread.getName(), failure)
    );
    private static final TracingExecutor EXECUTOR = new TracingExecutor(JOBS.service());
    private static final JobLoadMeter OVERLAY_METER = new JobLoadMeter(JOBS, System.nanoTime());

    private static volatile boolean active;

    private MeshWorkers() {
    }

    public static TracingExecutor executor() {
        active = true;
        return EXECUTOR;
    }

    public static boolean active() {
        return active;
    }

    public static int workers() {
        return JOBS.workers();
    }

    public static JobLoadMeter meter(long nowNanos) {
        return new JobLoadMeter(JOBS, nowNanos);
    }

    public static JobLoadMeter.JobLoad overlayLoad(long nowNanos) {
        return OVERLAY_METER.take(nowNanos);
    }
}
