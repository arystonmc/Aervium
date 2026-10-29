package com.aryston.kinetic.world.collision;

import java.util.concurrent.atomic.AtomicLong;

public final class CollisionAudit {
    public static final boolean ENABLED = Boolean.getBoolean("kinetic.collisionAudit");

    private static final AtomicLong SKIPPED = new AtomicLong();
    private static final AtomicLong WRONG = new AtomicLong();

    private CollisionAudit() {
    }

    public static void skipped(boolean vanillaEmpty) {
        SKIPPED.incrementAndGet();
        if (!vanillaEmpty) {
            WRONG.incrementAndGet();
        }
    }

    public static String summary() {
        return SKIPPED.get() + " entity collision searches skipped, " + WRONG.get() + " where vanilla found an entity";
    }
}
