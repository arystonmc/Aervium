package com.aryston.aervium.world.collision;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Debug check, on with -Daervium.collisionAudit=true: every entity collision query the section counts answer as empty
 * also runs the vanilla entity search, and the queries where vanilla found an entity are counted.
 */
public final class CollisionAudit {
    public static final boolean ENABLED = Boolean.getBoolean("aervium.collisionAudit");

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
