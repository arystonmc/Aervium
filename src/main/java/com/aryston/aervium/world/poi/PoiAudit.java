package com.aryston.aervium.world.poi;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public final class PoiAudit {
    public static final boolean ENABLED = Boolean.getBoolean("aervium.poiAudit");

    private static final AtomicLong SEARCHES = new AtomicLong();
    private static final AtomicLong RECORDS = new AtomicLong();
    private static final AtomicLong MISMATCHES = new AtomicLong();

    private PoiAudit() {
    }

    public static void compare(List<?> indexed, List<?> vanilla) {
        SEARCHES.incrementAndGet();
        RECORDS.addAndGet(vanilla.size());
        if (!indexed.equals(vanilla)) {
            MISMATCHES.incrementAndGet();
        }
    }

    public static String summary() {
        return SEARCHES.get() + " column searches, " + RECORDS.get() + " records, " + MISMATCHES.get() + " mismatches";
    }
}
