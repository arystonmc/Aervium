package com.aryston.kinetic.core.culling;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TileMaskTest {
    @Test
    void reportsOnlyRangesThatTouchAMarkedTile() {
        TileMask mask = new TileMask();
        mask.clear(8, 4);
        mask.mark(new int[] {2, 1, 3, 2});

        assertTrue(mask.any(new int[] {3, 2, 5, 3}));
        assertTrue(mask.any(new int[] {0, 0, 7, 3}));
        assertFalse(mask.any(new int[] {4, 0, 7, 3}));
        assertFalse(mask.any(new int[] {0, 3, 7, 3}));
    }

    @Test
    void clearingForgetsEverything() {
        TileMask mask = new TileMask();
        mask.clear(4, 4);
        mask.markEverything();
        mask.mark(new int[] {0, 0, 0, 0});
        assertTrue(mask.any(new int[] {3, 3, 3, 3}));

        mask.clear(4, 4);
        assertFalse(mask.any(new int[] {0, 0, 3, 3}));
    }
}
