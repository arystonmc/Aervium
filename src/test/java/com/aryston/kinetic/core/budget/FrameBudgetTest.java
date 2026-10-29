package com.aryston.kinetic.core.budget;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FrameBudgetTest {
    @Test
    void splitsTheFrameTimeBetweenSystems() {
        FrameBudget budget = new FrameBudget(100);
        assertEquals(10_000_000L, budget.frameNanos());
        assertEquals(10_000_000L, budget.budgetNanos(TimedSystem.FRAME));
        assertEquals(2_500_000L, budget.budgetNanos(TimedSystem.CLIENT_TICK));
    }

    @Test
    void serverTicksAlwaysGetFiftyMilliseconds() {
        assertEquals(50_000_000L, new FrameBudget(30).budgetNanos(TimedSystem.SERVER_TICK));
        assertEquals(50_000_000L, new FrameBudget(240).budgetNanos(TimedSystem.SERVER_TICK));
    }

    @Test
    void flagsTimesAboveTheBudget() {
        FrameBudget budget = new FrameBudget(100);
        assertFalse(budget.isOverBudget(TimedSystem.FRAME, 10_000_000L));
        assertTrue(budget.isOverBudget(TimedSystem.FRAME, 10_000_001L));
    }

    @Test
    void rejectsAZeroTarget() {
        assertThrows(IllegalArgumentException.class, () -> new FrameBudget(0));
    }
}
