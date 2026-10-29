package com.aryston.kinetic.core.pregeneration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TerrainProgressTest {
    private static final float DELTA = 1.0E-6F;

    @Test
    void startsAtZero() {
        assertEquals(0.0F, TerrainProgress.NONE.fraction(), DELTA);
        assertFalse(TerrainProgress.NONE.complete());
    }

    @Test
    void chunksFillTheFirstHalf() {
        assertEquals(0.25F, new TerrainProgress(50, 100, 0, 10, 0).fraction(), DELTA);
        assertEquals(0.5F, new TerrainProgress(100, 100, 0, 10, 0).fraction(), DELTA);
    }

    @Test
    void builtSectionsFillTheSecondHalf() {
        assertEquals(0.75F, new TerrainProgress(100, 100, 5, 10, 0).fraction(), DELTA);
        assertEquals(1.0F, new TerrainProgress(100, 100, 10, 10, 0).fraction(), DELTA);
    }

    @Test
    void isCompleteOnlyWithEveryChunkAndSection() {
        assertFalse(new TerrainProgress(99, 100, 10, 10, 0).complete());
        assertFalse(new TerrainProgress(100, 100, 9, 10, 0).complete());
        assertTrue(new TerrainProgress(100, 100, 10, 10, 0).complete());
        assertTrue(new TerrainProgress(100, 100, 0, 0, 0).complete());
    }

    @Test
    void waitsForPendingLightUpdates() {
        assertFalse(new TerrainProgress(100, 100, 10, 10, 3).complete());
        assertTrue(new TerrainProgress(100, 100, 10, 10, 2).advancedFrom(new TerrainProgress(100, 100, 10, 10, 3)));
    }

    @Test
    void notesEveryAdvance() {
        TerrainProgress earlier = new TerrainProgress(10, 100, 5, 20, 0);
        assertTrue(new TerrainProgress(11, 100, 5, 20, 0).advancedFrom(earlier));
        assertTrue(new TerrainProgress(10, 100, 6, 20, 0).advancedFrom(earlier));
        assertTrue(new TerrainProgress(10, 100, 5, 30, 0).advancedFrom(earlier));
        assertFalse(new TerrainProgress(10, 100, 5, 20, 0).advancedFrom(earlier));
    }
}
