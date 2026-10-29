package com.aryston.kinetic.core.telemetry;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.Collection;

public enum GarbageCollector {
    ZGC("ZGC", true),
    SHENANDOAH("Shenandoah", true),
    G1("G1", false),
    PARALLEL("Parallel", false),
    SERIAL("Serial", false),
    UNKNOWN("unknown", false);

    private final String displayName;
    private final boolean lowLatency;

    GarbageCollector(String displayName, boolean lowLatency) {
        this.displayName = displayName;
        this.lowLatency = lowLatency;
    }

    public String displayName() {
        return displayName;
    }

    public boolean lowLatency() {
        return lowLatency;
    }

    public static GarbageCollector current() {
        return fromBeanNames(ManagementFactory.getGarbageCollectorMXBeans().stream().map(GarbageCollectorMXBean::getName).toList());
    }

    public static GarbageCollector fromBeanNames(Collection<String> beanNames) {
        for (String name : beanNames) {
            if (name.startsWith("ZGC")) {
                return ZGC;
            }
            if (name.startsWith("Shenandoah")) {
                return SHENANDOAH;
            }
            if (name.startsWith("G1")) {
                return G1;
            }
            if (name.startsWith("PS ")) {
                return PARALLEL;
            }
            if (name.equals("Copy") || name.equals("MarkSweepCompact")) {
                return SERIAL;
            }
        }
        return UNKNOWN;
    }
}
