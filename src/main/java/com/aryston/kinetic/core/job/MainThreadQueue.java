package com.aryston.kinetic.core.job;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.LongSupplier;

public final class MainThreadQueue {
    private final Queue<Runnable> pending = new ConcurrentLinkedQueue<>();
    private final LongSupplier clock;

    public MainThreadQueue(LongSupplier clock) {
        this.clock = clock;
    }

    public void post(Runnable task) {
        pending.add(task);
    }

    public int drain(long budgetNanos) {
        long deadline = clock.getAsLong() + budgetNanos;
        int completed = 0;
        for (Runnable task = pending.peek(); task != null; task = pending.peek()) {
            if (completed > 0 && clock.getAsLong() >= deadline) {
                break;
            }
            pending.poll();
            task.run();
            completed++;
        }
        return completed;
    }
}
