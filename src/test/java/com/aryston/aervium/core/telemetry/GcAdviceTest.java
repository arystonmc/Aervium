package com.aryston.aervium.core.telemetry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class GcAdviceTest {
    private static final long GIGABYTE = 1L << 30;

    @Test
    void recognizesCollectorsFromBeanNames() {
        assertEquals(GarbageCollector.G1, GarbageCollector.fromBeanNames(List.of("G1 Young Generation", "G1 Concurrent GC", "G1 Old Generation")));
        assertEquals(GarbageCollector.ZGC, GarbageCollector.fromBeanNames(List.of("ZGC Minor Cycles", "ZGC Minor Pauses", "ZGC Major Cycles", "ZGC Major Pauses")));
        assertEquals(GarbageCollector.SHENANDOAH, GarbageCollector.fromBeanNames(List.of("Shenandoah Pauses", "Shenandoah Cycles")));
        assertEquals(GarbageCollector.PARALLEL, GarbageCollector.fromBeanNames(List.of("PS Scavenge", "PS MarkSweep")));
        assertEquals(GarbageCollector.SERIAL, GarbageCollector.fromBeanNames(List.of("Copy", "MarkSweepCompact")));
        assertEquals(GarbageCollector.UNKNOWN, GarbageCollector.fromBeanNames(List.of()));
    }

    @Test
    void staysQuietForALowLatencyCollectorWithEnoughHeap() {
        assertTrue(GcAdvice.advise(GarbageCollector.ZGC, 4 * GIGABYTE, 16 * GIGABYTE).isEmpty());
    }

    @Test
    void recommendsZgcInsteadOfG1() {
        String advice = GcAdvice.advise(GarbageCollector.G1, 4 * GIGABYTE, 16 * GIGABYTE).orElseThrow();
        assertTrue(advice.contains(GcAdvice.RECOMMENDED_FLAGS));
        assertFalse(advice.contains("-Xmx"));
    }

    @Test
    void recommendsMoreHeapOnlyWhenThePcHasTheMemory() {
        assertTrue(GcAdvice.advise(GarbageCollector.ZGC, 2 * GIGABYTE, 16 * GIGABYTE).orElseThrow().contains("-Xmx4G"));
        assertTrue(GcAdvice.advise(GarbageCollector.ZGC, 2 * GIGABYTE, 8 * GIGABYTE).isEmpty());
    }
}
