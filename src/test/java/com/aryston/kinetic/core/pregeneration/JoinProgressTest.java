package com.aryston.kinetic.core.pregeneration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class JoinProgressTest {
    private static final float DELTA = 1.0E-6F;

    @Test
    void theServerFillsTheFirstPart() {
        assertEquals(0.0F, JoinProgress.blend(0.0F, 0.0F), DELTA);
        assertEquals(JoinProgress.SERVER_SHARE, JoinProgress.blend(1.0F, 0.0F), DELTA);
    }

    @Test
    void terrainFillsTheRest() {
        assertEquals(1.0F, JoinProgress.blend(1.0F, 1.0F), DELTA);
        assertEquals(0.8F, JoinProgress.blend(1.0F, 0.5F), DELTA);
    }

    @Test
    void ignoresValuesOutsideTheRange() {
        assertEquals(0.0F, JoinProgress.blend(Float.NaN, -1.0F), DELTA);
        assertEquals(1.0F, JoinProgress.blend(2.0F, 3.0F), DELTA);
    }
}
