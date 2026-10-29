package com.aryston.kinetic.core.job;

import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

public final class JobSystem implements AutoCloseable {
    private static final int RESERVED_CORES = 2;
    private static final int MAX_WORKERS = 8;
    private static final long SHUTDOWN_WAIT_MILLIS = 2_000;

    private final ForkJoinPool pool;
    private final LongAdder timedJobs = new LongAdder();
    private final LongAdder busyNanos = new LongAdder();
    private final ExecutorService service = new TimedService();

    public JobSystem(String name, int workers, Thread.UncaughtExceptionHandler failureHandler) {
        this(name, workers, Thread.NORM_PRIORITY - 1, failureHandler);
    }

    public JobSystem(String name, int workers, int priority, Thread.UncaughtExceptionHandler failureHandler) {
        AtomicInteger counter = new AtomicInteger();
        ForkJoinPool.ForkJoinWorkerThreadFactory factory = forkJoinPool -> {
            ForkJoinWorkerThread thread = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(forkJoinPool);
            thread.setName(name + " Worker-" + counter.incrementAndGet());
            thread.setDaemon(true);
            thread.setPriority(priority);
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

    public ExecutorService service() {
        return service;
    }

    public int workers() {
        return pool.getParallelism();
    }

    public long timedJobs() {
        return timedJobs.sum();
    }

    public long busyNanos() {
        return busyNanos.sum();
    }

    private void runTimed(Runnable job) {
        long start = System.nanoTime();
        try {
            job.run();
        } finally {
            busyNanos.add(System.nanoTime() - start);
            timedJobs.increment();
        }
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

    private final class TimedService extends AbstractExecutorService {
        @Override
        public void execute(Runnable job) {
            if (!pool.isShutdown()) {
                pool.execute(() -> runTimed(job));
            }
        }

        @Override
        public void shutdown() {
            pool.shutdown();
        }

        @Override
        public List<Runnable> shutdownNow() {
            return pool.shutdownNow();
        }

        @Override
        public boolean isShutdown() {
            return pool.isShutdown();
        }

        @Override
        public boolean isTerminated() {
            return pool.isTerminated();
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return pool.awaitTermination(timeout, unit);
        }
    }
}
