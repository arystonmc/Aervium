package com.aryston.kinetic.core.job;

import java.util.HashSet;
import java.util.Set;

public final class RegionLocks {
    private static final int COORDINATE_BITS = 32;
    private static final long COORDINATE_MASK = 0xFFFFFFFFL;

    private final Set<Long> held = new HashSet<>();

    public synchronized void acquire(int centerX, int centerZ, int radius) {
        boolean interrupted = false;
        while (!free(centerX, centerZ, radius)) {
            try {
                wait();
            } catch (InterruptedException interruption) {
                interrupted = true;
            }
        }
        forEach(centerX, centerZ, radius, held::add);
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    public synchronized void release(int centerX, int centerZ, int radius) {
        forEach(centerX, centerZ, radius, held::remove);
        notifyAll();
    }

    public synchronized int heldCells() {
        return held.size();
    }

    private boolean free(int centerX, int centerZ, int radius) {
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                if (held.contains(key(x, z))) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void forEach(int centerX, int centerZ, int radius, CellAction action) {
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                action.accept(key(x, z));
            }
        }
    }

    private static long key(int x, int z) {
        return (long) x << COORDINATE_BITS | z & COORDINATE_MASK;
    }

    @FunctionalInterface
    private interface CellAction {
        void accept(long cell);
    }
}
