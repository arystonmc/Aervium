package com.aryston.aervium.core.lod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LodCoverageTest {
    private static final long START = 1L;
    private static final long READY = START + LodTransitions.DURATION_NANOS * 2;

    @Test
    void loadedDataDoesNotHideDistantTerrainUntilAllSurfaceSectionsHaveMeshes() {
        LodCoverage coverage = new LodCoverage();
        long[] samples = {LodSample.of(70, 0, 0, 0), LodSample.of(110, 0, 0, 0)};
        coverage.require(-1, -2, samples, START);
        assertEquals(0, coverage.value(-1, -2, READY));
        coverage.compiled(-1, 4, -2, true, START);
        assertEquals(0, coverage.value(-1, -2, READY));
        coverage.compiled(-1, 6, -2, true, START);
        assertEquals(0, coverage.value(-1, -2, START));
        assertEquals(255, coverage.value(-1, -2, READY));
        byte[] pixels = new byte[LodCoverage.SIZE * LodCoverage.SIZE];
        coverage.rasterize(pixels, READY);
        assertEquals(255, Byte.toUnsignedInt(pixels[(LodCoverage.SIZE - 2) * LodCoverage.SIZE + LodCoverage.SIZE - 1]));
        coverage.compiled(-1, 4, -2, false, READY);
        assertEquals(0, coverage.value(-1, -2, READY));
    }

    @Test
    void revisitingAColumnPreservesItsFadeAndUnloadingClearsCoverage() {
        LodCoverage coverage = new LodCoverage();
        long[] samples = {LodSample.of(70, 0, 0, 0)};
        assertFalse(coverage.known(0, 0));
        coverage.compiled(0, 4, 0, true, START);
        coverage.compiled(0, 5, 0, true, START);
        coverage.require(0, 0, samples, START);
        assertTrue(coverage.known(0, 0));
        coverage.require(0, 0, new long[]{LodSample.of(70, 0, 0, 0), LodSample.of(84, 0, 0, 0)}, READY);
        assertEquals(255, coverage.value(0, 0, READY));
        coverage.forget(0, 0);
        assertFalse(coverage.known(0, 0));
        assertEquals(0, coverage.value(0, 0, READY));
        coverage.require(-20, -20, new long[0], START);
        coverage.trim(0, 0, 10);
        assertFalse(coverage.known(-20, -20));
        assertEquals(0, coverage.value(-20, -20, READY));
    }
}
