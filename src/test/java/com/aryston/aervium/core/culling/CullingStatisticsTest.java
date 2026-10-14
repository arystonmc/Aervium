package com.aryston.aervium.core.culling;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CullingStatisticsTest {
    @Test
    void shadowHiddenPercentCoversEveryFrameSinceTheLastTake() {
        CullingStatistics statistics = new CullingStatistics();
        shadowFrame(statistics, 100, 50);
        shadowFrame(statistics, 300, 0);

        assertEquals(13, statistics.takeShadowHiddenPercent());
        assertEquals(0, statistics.takeShadowHiddenPercent());
    }

    @Test
    void shadowHiddenPercentSumsTheCascadesOfAFrame() {
        CullingStatistics statistics = new CullingStatistics();
        statistics.beginShadows();
        statistics.shadowCascade(40, 10);
        statistics.shadowCascade(60, 30);
        statistics.endShadows();

        assertEquals(40, statistics.takeShadowHiddenPercent());
        assertEquals(40, statistics.shadowHidden());
        assertEquals(100, statistics.shadowCasters());
    }

    private static void shadowFrame(CullingStatistics statistics, int casters, int hidden) {
        statistics.beginShadows();
        statistics.shadowCascade(casters, hidden);
        statistics.endShadows();
    }
}
