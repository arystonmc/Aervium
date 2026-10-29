package com.aryston.kinetic.debug.benchmark;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class JoinTimelineTest {
    private static final long MILLI = 1_000_000L;

    @Test
    void startsWithTheFirstFrame() {
        JoinTimeline timeline = new JoinTimeline();
        assertFalse(timeline.started());
        timeline.frame(5 * MILLI, "reading world data");
        assertTrue(timeline.started());
        assertEquals(10 * MILLI, timeline.elapsedNanos(15 * MILLI));
    }

    @Test
    void chargesEachFrameToThePhaseItStartedIn() {
        JoinTimeline timeline = new JoinTimeline();
        timeline.frame(0L, "reading world data");
        timeline.frame(10 * MILLI, "reading world data");
        timeline.frame(1210 * MILLI, "loading data packs");
        timeline.frame(1220 * MILLI, "server");
        List<String> lines = timeline.lines();
        assertEquals("phase reading world data: starts 0 ms, lasts 1210 ms, 2 frames, longest 1200.0 ms, 1 over 50 ms", lines.get(0));
        assertEquals("phase loading data packs: starts 1210 ms, lasts 10 ms, 1 frames, longest 10.0 ms, 0 over 50 ms", lines.get(1));
        assertEquals("1 frames over 50 ms; 1200 ms at 10 ms (reading world data to loading data packs)", lines.get(3));
        assertEquals(1, timeline.gapCount());
        assertEquals(1200 * MILLI, timeline.longestNanos());
    }
}
