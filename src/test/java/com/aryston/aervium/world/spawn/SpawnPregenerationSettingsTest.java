package com.aryston.aervium.world.spawn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class SpawnPregenerationSettingsTest {
    @AfterEach
    void restoreDefaults() {
        SpawnPregenerationSettings.apply(false, SpawnPregenerationSettings.DEFAULT_RADIUS);
        SpawnPregenerationSettings.useRenderDistance(SpawnPregenerationSettings.MAX_RADIUS);
    }

    @Test
    void keepsTheRadiusInRange() {
        SpawnPregenerationSettings.apply(true, 2);
        assertEquals(SpawnPregenerationSettings.MIN_RADIUS, SpawnPregenerationSettings.joinRadius());
        SpawnPregenerationSettings.apply(true, 64);
        assertEquals(SpawnPregenerationSettings.MAX_RADIUS, SpawnPregenerationSettings.joinRadius());
    }

    @Test
    void neverGoesPastTheRenderDistance() {
        SpawnPregenerationSettings.useRenderDistance(12);
        SpawnPregenerationSettings.apply(true, 32);
        assertEquals(12, SpawnPregenerationSettings.joinRadius());
        SpawnPregenerationSettings.apply(true, 8);
        assertEquals(8, SpawnPregenerationSettings.joinRadius());
    }

    @Test
    void followsTheSwitch() {
        SpawnPregenerationSettings.apply(true, 16);
        assertTrue(SpawnPregenerationSettings.enabled());
        SpawnPregenerationSettings.apply(false, 16);
        assertFalse(SpawnPregenerationSettings.enabled());
    }
}
