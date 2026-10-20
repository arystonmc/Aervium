package com.aryston.aervium.core.lod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GenerationPowerTest {
    @Test
    void everySettingCanMakeProgressAndHigherSettingsAllowMoreWorkers() {
        for (int cores = 1; cores <= 64; cores++) {
            int previous = 0;
            for (GenerationPower power : GenerationPower.values()) {
                int threads = power.threads(cores);
                assertTrue(threads >= 1 && threads >= previous && threads <= cores);
                previous = threads;
            }
        }
    }

    @Test
    void lowAndBalancedReserveIdleTimeWithoutChangingTheResultQuality() {
        long work = 1000L;
        assertEquals(work * 3, GenerationPower.LOW.idleNanos(work));
        assertEquals(work, GenerationPower.BALANCED.idleNanos(work));
        assertEquals(0L, GenerationPower.HIGH.idleNanos(work));
        assertEquals(0L, GenerationPower.EXTREME.idleNanos(work));
        assertEquals(0L, GenerationPower.LOW.idleNanos(-work));
    }
}
