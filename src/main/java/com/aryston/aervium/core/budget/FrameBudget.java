package com.aryston.aervium.core.budget;

import com.aryston.aervium.core.telemetry.Durations;

public record FrameBudget(int targetFps) {
    public FrameBudget {
        if (targetFps <= 0) {
            throw new IllegalArgumentException("targetFps must be positive");
        }
    }

    public long frameNanos() {
        return Math.round(Durations.NANOS_PER_SECOND / targetFps);
    }

    public long budgetNanos(TimedSystem system) {
        long whole = system.kind() == TimedSystem.BudgetKind.TICK ? Durations.TICK_BUDGET_NANOS : frameNanos();
        return Math.round(whole * system.budgetShare());
    }

    public boolean isOverBudget(TimedSystem system, long nanos) {
        return nanos > budgetNanos(system);
    }
}
