package com.aryston.aervium.core.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class JobSystemTest {
    private static final int JOBS = 64;
    private static final long WAIT_SECONDS = 5;

    @Test
    void keepsCoresFreeForTheGameThreads() {
        assertEquals(1, JobSystem.defaultWorkers(1));
        assertEquals(1, JobSystem.defaultWorkers(3));
        assertEquals(6, JobSystem.defaultWorkers(8));
        assertEquals(8, JobSystem.defaultWorkers(32));
    }

    @Test
    void runsEverySubmittedJobOnNamedWorkers() throws InterruptedException {
        AtomicInteger completed = new AtomicInteger();
        AtomicReference<String> workerName = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(JOBS);
        try (JobSystem jobs = new JobSystem("Test", 2, (thread, failure) -> { })) {
            for (int i = 0; i < JOBS; i++) {
                jobs.submit(() -> {
                    workerName.set(Thread.currentThread().getName());
                    completed.incrementAndGet();
                    done.countDown();
                });
            }
            assertTrue(done.await(WAIT_SECONDS, TimeUnit.SECONDS));
        }
        assertEquals(JOBS, completed.get());
        assertTrue(workerName.get().startsWith("Test Worker-"));
    }

    @Test
    void ignoresJobsAfterClosing() {
        JobSystem jobs = new JobSystem("Closed", 1, (thread, failure) -> { });
        jobs.close();
        AtomicInteger ran = new AtomicInteger();
        jobs.submit(ran::incrementAndGet);
        assertEquals(0, ran.get());
    }
}
