package com.aryston.aervium.core.telemetry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GcPauseRecorderTest {
    private static final double EPSILON = 1e-9;

    @Test
    void classifiesStopTheWorldPauses() {
        assertTrue(GcPauseClassifier.isPause("G1 Young Generation", "end of minor GC"));
        assertTrue(GcPauseClassifier.isPause("ZGC Major Pauses", "end of GC pause"));
        assertFalse(GcPauseClassifier.isPause("ZGC Major Cycles", "end of GC cycle"));
        assertFalse(GcPauseClassifier.isPause("Shenandoah Cycles", "end of GC cycle"));
    }

    @Test
    void sumsPausesAndIgnoresConcurrentCycles() {
        GcPauseRecorder recorder = new GcPauseRecorder();
        recorder.record("G1 Young Generation", "end of minor GC", 4);
        recorder.record("ZGC Major Cycles", "end of GC cycle", 300);
        recorder.record("G1 Old Generation", "end of major GC", 12);
        GcPauseTotals total = recorder.total();
        assertEquals(2, total.pauses());
        assertEquals(16.0, total.totalMs(), EPSILON);
        assertEquals(12.0, total.longestMs(), EPSILON);
    }

    @Test
    void reportsOnlyPausesAfterAMark() {
        GcPauseRecorder recorder = new GcPauseRecorder();
        recorder.record("G1 Young Generation", "end of minor GC", 20);
        long mark = recorder.mark();
        recorder.record("G1 Young Generation", "end of minor GC", 3);
        recorder.record("G1 Young Generation", "end of minor GC", 5);
        GcPauseTotals since = recorder.since(mark);
        assertEquals(2, since.pauses());
        assertEquals(8.0, since.totalMs(), EPSILON);
        assertEquals(5.0, since.longestMs(), EPSILON);
    }
}
