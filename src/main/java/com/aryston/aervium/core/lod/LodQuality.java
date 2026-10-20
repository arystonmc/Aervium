package com.aryston.aervium.core.lod;

public enum LodQuality {
    LOW(1.25, 3, 16, false),
    MEDIUM(1.5, 2, 24, false),
    HIGH(2.5, 0, 48, false),
    ULTRA(3.0, 0, 64, true);

    private final double splitFactor;
    private final int finestLevel;
    private final int generatedGrid;
    private final boolean textured;

    LodQuality(double splitFactor, int finestLevel, int generatedGrid, boolean textured) {
        this.splitFactor = splitFactor;
        this.finestLevel = finestLevel;
        this.generatedGrid = generatedGrid;
        this.textured = textured;
    }

    public double splitFactor() {
        return splitFactor;
    }

    public int finestLevel() {
        return finestLevel;
    }

    public int generatedGrid() {
        return generatedGrid;
    }

    public boolean textured() {
        return textured;
    }
}
