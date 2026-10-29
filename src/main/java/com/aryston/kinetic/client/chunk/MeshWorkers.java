package com.aryston.kinetic.client.chunk;

import com.aryston.kinetic.Kinetic;
import com.aryston.kinetic.core.job.JobLoadMeter;
import com.aryston.kinetic.core.job.JobSystem;
import net.minecraft.TracingExecutor;

public final class MeshWorkers {
    private static final JobSystem JOBS = new JobSystem(
        "Kinetic Mesh",
        JobSystem.defaultWorkers(Runtime.getRuntime().availableProcessors()),
        (thread, failure) -> Kinetic.LOGGER.error("Kinetic chunk mesh worker {} failed", thread.getName(), failure)
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
