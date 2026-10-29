package com.aryston.kinetic.core.job;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class MainThreadQueueTest {
    @Test
    void runsTasksInOrderWithinTheBudget() {
        AtomicLong clock = new AtomicLong();
        MainThreadQueue queue = new MainThreadQueue(clock::get);
        List<Integer> ran = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            int task = i;
            queue.post(() -> {
                ran.add(task);
                clock.addAndGet(10);
            });
        }
        assertEquals(3, queue.drain(25));
        assertEquals(List.of(0, 1, 2), ran);
        assertEquals(2, queue.drain(1_000));
        assertEquals(List.of(0, 1, 2, 3, 4), ran);
    }

    @Test
    void alwaysRunsAtLeastOneTask() {
        AtomicLong clock = new AtomicLong();
        MainThreadQueue queue = new MainThreadQueue(clock::get);
        queue.post(() -> clock.addAndGet(100));
        queue.post(() -> clock.addAndGet(100));
        assertEquals(1, queue.drain(0));
        assertEquals(0, new MainThreadQueue(clock::get).drain(1_000));
    }
}
