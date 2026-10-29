package com.aryston.kinetic.debug.benchmark;

public record BlockPoint(int x, int y, int z) {
    public BlockPoint offset(int dx, int dy, int dz) {
        return new BlockPoint(x + dx, y + dy, z + dz);
    }

    public String coordinates() {
        return x + " " + y + " " + z;
    }
}
