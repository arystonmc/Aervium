package com.aryston.kinetic.core.budget;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LapTimerTest {
    @Test
    void theFirstLapIsEmpty() {
        assertEquals(0.0, new LapTimer().lapMillis(123_456_789L));
    }

    @Test
    void lapsKeepTheFractionOfAMillisecond() {
        LapTimer timer = new LapTimer();
        timer.lapMillis(10_000_000L);
        assertEquals(3.25, timer.lapMillis(13_250_000L), 1e-9);
        assertEquals(0.4, timer.lapMillis(13_650_000L), 1e-9);
    }

    @Test
    void elapsedTimeDoesNotStartANewLap() {
        LapTimer timer = new LapTimer();
        assertEquals(0.0, timer.elapsedMillis(5_000_000L));
        timer.lapMillis(10_000_000L);
        assertEquals(1.5, timer.elapsedMillis(11_500_000L), 1e-9);
        assertEquals(2.0, timer.lapMillis(12_000_000L), 1e-9);
    }

    @Test
    void lapsAddUpToTheWholeTime() {
        LapTimer timer = new LapTimer();
        timer.lapMillis(0L);
        double sum = 0.0;
        for (long now = 1_337L; now < 1_000_000_000L; now += 1_337_011L) {
            sum += timer.lapMillis(now);
        }
        sum += timer.lapMillis(1_000_000_000L);
        assertEquals(1000.0, sum, 1e-6);
    }
}
