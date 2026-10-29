package com.aryston.kinetic.core.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class JobLoadMeterTest {
    private static final int JOBS = 32;
    private static final long WAIT_SECONDS = 5;
    private static final long JOB_NANOS = 200_000;

    @Test
    void countsJobsAndBusyTimeOfTheService() throws InterruptedException {
        try (JobSystem jobs = new JobSystem("Meter", 2, Thread.NORM_PRIORITY, (thread, failure) -> { })) {
            long start = System.nanoTime();
            JobLoadMeter meter = new JobLoadMeter(jobs, start);
            CountDownLatch done = new CountDownLatch(JOBS);
            for (int i = 0; i < JOBS; i++) {
                jobs.service().execute(() -> {
                    long until = System.nanoTime() + JOB_NANOS;
                    while (System.nanoTime() < until) {
                        Thread.onSpinWait();
                    }
                    done.countDown();
                });
            }
            assertTrue(done.await(WAIT_SECONDS, TimeUnit.SECONDS));
            while (jobs.timedJobs() < JOBS) {
                Thread.onSpinWait();
            }
            assertTrue(jobs.busyNanos() >= JOBS * JOB_NANOS);
            JobLoadMeter.JobLoad load = meter.take(System.nanoTime());
            assertTrue(load.jobsPerSecond() > 0.0);
            assertTrue(load.busyShare() > 0.0 && load.busyShare() <= 1.0);
            JobLoadMeter.JobLoad idle = meter.take(System.nanoTime() + 1);
            assertEquals(0.0, idle.jobsPerSecond());
        }
    }

    @Test
    void reportsNoLoadForAnEmptyInterval() {
        try (JobSystem jobs = new JobSystem("Empty", 1, (thread, failure) -> { })) {
            JobLoadMeter meter = new JobLoadMeter(jobs, 100);
            JobLoadMeter.JobLoad load = meter.take(100);
            assertEquals(0.0, load.jobsPerSecond());
            assertEquals(0.0, load.busyShare());
        }
    }

    @Test
    void serviceDropsJobsAfterClosing() {
        JobSystem jobs = new JobSystem("Closed", 1, (thread, failure) -> { });
        jobs.close();
        jobs.service().execute(() -> { });
        assertTrue(jobs.service().isShutdown());
        assertEquals(0, jobs.timedJobs());
    }
}
