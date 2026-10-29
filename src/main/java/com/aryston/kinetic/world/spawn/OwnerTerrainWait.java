package com.aryston.kinetic.world.spawn;

public final class OwnerTerrainWait {
    private static volatile boolean holding;

    private OwnerTerrainWait() {
    }

    public static void set(boolean waiting) {
        holding = waiting;
    }

    public static boolean holding() {
        return holding;
    }
}
