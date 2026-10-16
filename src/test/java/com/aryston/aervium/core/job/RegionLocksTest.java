package com.aryston.aervium.core.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class RegionLocksTest {
    private static final int RADIUS = 1;
    private static final int THREADS = 8;
    private static final int ROUNDS = 400;
    private static final int GRID = 6;
    private static final long WAIT_MILLIS = 200;

    @Test
    void separateRegionsAreHeldTogether() {
        RegionLocks locks = new RegionLocks();
        locks.acquire(0, 0, RADIUS);
        locks.acquire(3, 0, RADIUS);
        assertEquals(18, locks.heldCells());
        locks.release(0, 0, RADIUS);
        locks.release(3, 0, RADIUS);
        assertEquals(0, locks.heldCells());
    }

    @Test
    void anOverlappingRegionWaitsForTheRelease() throws Exception {
        RegionLocks locks = new RegionLocks();
        locks.acquire(0, 0, RADIUS);
        CountDownLatch acquired = new CountDownLatch(1);
        Thread waiter = new Thread(() -> {
            locks.acquire(2, 2, RADIUS);
            acquired.countDown();
            locks.release(2, 2, RADIUS);
        });
        waiter.start();
        assertFalse(acquired.await(WAIT_MILLIS, TimeUnit.MILLISECONDS));
        locks.release(0, 0, RADIUS);
        assertTrue(acquired.await(WAIT_MILLIS * 10, TimeUnit.MILLISECONDS));
        waiter.join();
    }

    @Test
    void noTwoHoldersEverShareACell() throws Exception {
        RegionLocks locks = new RegionLocks();
        AtomicInteger[][] owners = new AtomicInteger[GRID + 2][GRID + 2];
        for (AtomicInteger[] row : owners) {
            for (int z = 0; z < row.length; z++) {
                row[z] = new AtomicInteger();
            }
        }
        AtomicBoolean overlap = new AtomicBoolean();
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        List<Future<?>> jobs = new ArrayList<>();
        for (int job = 0; job < ROUNDS; job++) {
            int centerX = 1 + job % GRID;
            int centerZ = 1 + job * 7 % GRID;
            jobs.add(pool.submit(() -> {
                locks.acquire(centerX, centerZ, RADIUS);
                try {
                    for (int x = centerX - RADIUS; x <= centerX + RADIUS; x++) {
                        for (int z = centerZ - RADIUS; z <= centerZ + RADIUS; z++) {
                            if (owners[x][z].incrementAndGet() != 1) {
                                overlap.set(true);
                            }
                        }
                    }
                    Thread.onSpinWait();
                    for (int x = centerX - RADIUS; x <= centerX + RADIUS; x++) {
                        for (int z = centerZ - RADIUS; z <= centerZ + RADIUS; z++) {
                            owners[x][z].decrementAndGet();
                        }
                    }
                } finally {
                    locks.release(centerX, centerZ, RADIUS);
                }
            }));
        }
        for (Future<?> job : jobs) {
            job.get();
        }
        pool.shutdown();
        assertFalse(overlap.get());
        assertEquals(0, locks.heldCells());
    }
}
