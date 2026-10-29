package com.aryston.kinetic.core.culling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VoxelRayTest {
    @Test
    void emptyAirIsReached() {
        assertTrue(VoxelRay.reaches(0.5, 0.5, 0.5, 20.5, 3.5, -7.5, (x, y, z) -> false));
    }

    @Test
    void anOpaqueBlockOnTheWayStopsTheRay() {
        assertFalse(VoxelRay.reaches(0.5, 0.5, 0.5, 20.5, 0.5, 0.5, (x, y, z) -> x == 10));
        assertTrue(VoxelRay.reaches(0.5, 0.5, 0.5, 20.5, 0.5, 0.5, (x, y, z) -> x == 10 && y == 1));
    }

    @Test
    void theStartBlockIsIgnored() {
        assertTrue(VoxelRay.reaches(0.5, 0.5, 0.5, 5.5, 0.5, 0.5, (x, y, z) -> x == 0));
    }

    @Test
    void diagonalRaysVisitEveryCrossedBlock() {
        assertFalse(VoxelRay.reaches(0.2, 0.2, 0.5, 3.9, 3.7, 0.5, (x, y, z) -> x == 1 && y == 1));
    }

    @Test
    void entryIsWhereTheSegmentFirstTouchesTheBox() {
        assertEquals(0.5, VoxelRay.entry(0, 0, 0, 20, 0, 0, 10, -1, -1, 12, 1, 1), 1.0E-9);
        assertTrue(Double.isNaN(VoxelRay.entry(0, 0, 0, 20, 0, 0, 10, 2, -1, 12, 3, 1)));
        assertEquals(0.0, VoxelRay.entry(11, 0, 0, 20, 0, 0, 10, -1, -1, 12, 1, 1), 1.0E-9);
    }
}
