package com.aryston.aervium.core.budget;

public enum TimedSystem {
    FRAME("Frame", BudgetKind.FRAME, 1.0),
    CLIENT_TICK("Client tick", BudgetKind.FRAME, 0.25),
    SERVER_TICK("Server tick", BudgetKind.TICK, 1.0),
    OVERLAY("Overlay", BudgetKind.FRAME, 0.02),
    TELEMETRY("Telemetry", BudgetKind.FRAME, 0.1);

    private final String label;
    private final BudgetKind kind;
    private final double budgetShare;

    TimedSystem(String label, BudgetKind kind, double budgetShare) {
        this.label = label;
        this.kind = kind;
        this.budgetShare = budgetShare;
    }

    public String label() {
        return label;
    }

    public BudgetKind kind() {
        return kind;
    }

    public double budgetShare() {
        return budgetShare;
    }

    public enum BudgetKind {
        FRAME,
        TICK
    }
}
