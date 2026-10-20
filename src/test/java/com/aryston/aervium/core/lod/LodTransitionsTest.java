package com.aryston.aervium.core.lod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import org.junit.jupiter.api.Test;

class LodTransitionsTest {
    private static final long START = 1L;

    @Test
    void subdivisionUsesComplementaryWeightsOverTheWholeParent() {
        LodTransitions transitions = new LodTransitions();
        long parent = LodStore.key(1, -1, -1);
        transitions.update(LongArrayList.of(parent), START);
        long changed = START + LodTransitions.DURATION_NANOS;
        transitions.update(LongArrayList.of(LodStore.key(0, -2, -2), LodStore.key(0, -1, -2), LodStore.key(0, -2, -1), LodStore.key(0, -1, -1)), changed);
        assertEquals(4, transitions.regions().size());
        assertEquals(128L * 128L, area(transitions));
        assertEquals(0.5F, transitions.progress(changed + LodTransitions.DURATION_NANOS / 2));
        assertTrue(transitions.regions().stream().allMatch(region -> region.previous() == parent));
        assertTrue(transitions.retains(parent));
    }

    @Test
    void coarseningAfterSparseCoverageFillsMissingAreasAndEventuallyReleasesChildren() {
        LodTransitions transitions = new LodTransitions();
        long child = LodStore.key(0, 0, 0);
        long parent = LodStore.key(1, 0, 0);
        transitions.update(LongArrayList.of(child), START);
        long changed = START + LodTransitions.DURATION_NANOS;
        transitions.update(LongArrayList.of(parent), changed);
        assertEquals(128L * 128L, area(transitions));
        transitions.update(LongArrayList.of(parent), changed + LodTransitions.DURATION_NANOS);
        assertEquals(1, transitions.regions().size());
        assertEquals(parent, transitions.regions().getFirst().previous());
        assertEquals(false, transitions.retains(child));
        transitions.clear();
        assertTrue(transitions.regions().isEmpty());
    }

    private static long area(LodTransitions transitions) {
        return transitions.regions().stream().mapToLong(region -> (long) (region.maxX() - region.minX()) * (region.maxZ() - region.minZ())).sum();
    }
}
