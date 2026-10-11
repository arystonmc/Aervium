package com.aryston.aervium.core.budget;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aryston.aervium.core.telemetry.TelemetrySnapshot.SystemTiming;
import java.util.List;
import org.junit.jupiter.api.Test;

class SystemTimingsTest {
    private static final double EPSILON = 1e-9;

    @Test
    void smoothsTimesAndKeepsThePeakUntilSampled() {
        SystemTimings timings = new SystemTimings();
        timings.record(TimedSystem.CLIENT_TICK, 1_000_000L);
        timings.record(TimedSystem.CLIENT_TICK, 11_000_000L);
        SystemTiming first = timing(timings.sample(new FrameBudget(60)), TimedSystem.CLIENT_TICK);
        assertEquals(2.0, first.smoothedMs(), EPSILON);
        assertEquals(11.0, first.peakMs(), EPSILON);
        SystemTiming second = timing(timings.sample(new FrameBudget(60)), TimedSystem.CLIENT_TICK);
        assertEquals(0.0, second.peakMs(), EPSILON);
    }

    @Test
    void marksSystemsAboveTheirBudget() {
        SystemTimings timings = new SystemTimings();
        timings.record(TimedSystem.FRAME, 20_000_000L);
        timings.record(TimedSystem.SERVER_TICK, 20_000_000L);
        List<SystemTiming> sampled = timings.sample(new FrameBudget(60));
        assertTrue(timing(sampled, TimedSystem.FRAME).overBudget());
        assertFalse(timing(sampled, TimedSystem.SERVER_TICK).overBudget());
    }

    private static SystemTiming timing(List<SystemTiming> timings, TimedSystem system) {
        return timings.stream().filter(timing -> timing.system() == system).findFirst().orElseThrow();
    }
}
