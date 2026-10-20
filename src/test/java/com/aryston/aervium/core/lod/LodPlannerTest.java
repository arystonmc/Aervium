package com.aryston.aervium.core.lod;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LodPlannerTest {
    @Test
    void previewsFillTheHorizonBeforeFinerLevels() {
        LodPlanner planner = new LodPlanner();
        long[] preview = planner.plan(0.0, 0.0, 512.0, LodSelection.ClipBox.NONE, LodQuality.ULTRA,
            key -> LodStore.levelOf(key) <= 2, 1);
        assertEquals(2, LodStore.levelOf(preview[0]));
    }
}
