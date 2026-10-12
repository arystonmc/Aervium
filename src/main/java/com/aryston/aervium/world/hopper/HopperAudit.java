package com.aryston.aervium.world.hopper;

import java.util.concurrent.atomic.AtomicLong;

public final class HopperAudit {
    public static final boolean ENABLED = Boolean.getBoolean("aervium.hopperAudit");

    private static final AtomicLong ITEM_SKIPS = new AtomicLong();
    private static final AtomicLong ITEM_WRONG = new AtomicLong();
    private static final AtomicLong CONTAINER_SKIPS = new AtomicLong();
    private static final AtomicLong CONTAINER_WRONG = new AtomicLong();
    private static final AtomicLong ITEM_SEARCHED = new AtomicLong();
    private static final AtomicLong CONTAINER_SEARCHED = new AtomicLong();

    private HopperAudit() {
    }

    public static void itemSearch(boolean vanillaEmpty) {
        ITEM_SKIPS.incrementAndGet();
        if (!vanillaEmpty) {
            ITEM_WRONG.incrementAndGet();
        }
    }

    public static void containerSearch(boolean vanillaEmpty) {
        CONTAINER_SKIPS.incrementAndGet();
        if (!vanillaEmpty) {
            CONTAINER_WRONG.incrementAndGet();
        }
    }

    public static void itemSearched() {
        ITEM_SEARCHED.incrementAndGet();
    }

    public static void containerSearched() {
        CONTAINER_SEARCHED.incrementAndGet();
    }

    public static String summary() {
        return ITEM_SKIPS.get() + " of " + (ITEM_SKIPS.get() + ITEM_SEARCHED.get()) + " item searches skippable, "
            + CONTAINER_SKIPS.get() + " of " + (CONTAINER_SKIPS.get() + CONTAINER_SEARCHED.get()) + " container searches skippable, "
            + (ITEM_WRONG.get() + CONTAINER_WRONG.get()) + " where vanilla found an entity";
    }
}
