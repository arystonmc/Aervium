package com.aryston.aervium.core.job;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class JobSystem implements AutoCloseable {
    private static final int RESERVED_CORES = 2;
    private static final int MAX_WORKERS = 8;
    private static final long SHUTDOWN_WAIT_MILLIS = 2_000;

    private final ForkJoinPool pool;

    public JobSystem(String name, int workers, Thread.UncaughtExceptionHandler failureHandler) {
        AtomicInteger counter = new AtomicInteger();
        ForkJoinPool.ForkJoinWorkerThreadFactory factory = forkJoinPool -> {
            ForkJoinWorkerThread thread = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(forkJoinPool);
            thread.setName(name + " Worker-" + counter.incrementAndGet());
            thread.setDaemon(true);
            thread.setPriority(Thread.NORM_PRIORITY - 1);
            return thread;
        };
        this.pool = new ForkJoinPool(workers, factory, failureHandler, true);
    }

    public static int defaultWorkers(int availableCores) {
        return Math.clamp(availableCores - RESERVED_CORES, 1, MAX_WORKERS);
    }

    public void submit(Runnable job) {
        if (!pool.isShutdown()) {
            pool.execute(job);
        }
    }

    public int workers() {
        return pool.getParallelism();
    }

    @Override
    public void close() {
        pool.shutdown();
        try {
            if (!pool.awaitTermination(SHUTDOWN_WAIT_MILLIS, TimeUnit.MILLISECONDS)) {
                pool.shutdownNow();
            }
        } catch (InterruptedException interrupted) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
