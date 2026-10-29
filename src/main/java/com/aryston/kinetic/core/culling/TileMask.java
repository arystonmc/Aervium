package com.aryston.kinetic.core.culling;

import java.util.Arrays;

public final class TileMask {
    private boolean[] marked = new boolean[0];
    private int tilesX;
    private boolean everything;

    public void clear(int nextTilesX, int nextTilesY) {
        int size = nextTilesX * nextTilesY;
        if (marked.length != size) {
            marked = new boolean[size];
        } else {
            Arrays.fill(marked, false);
        }
        tilesX = nextTilesX;
        everything = false;
    }

    public void markEverything() {
        everything = true;
    }

    public void mark(int[] range) {
        for (int tileY = range[1]; tileY <= range[3]; tileY++) {
            Arrays.fill(marked, tileY * tilesX + range[0], tileY * tilesX + range[2] + 1, true);
        }
    }

    public boolean any(int[] range) {
        if (everything) {
            return true;
        }
        for (int tileY = range[1]; tileY <= range[3]; tileY++) {
            for (int tileX = range[0]; tileX <= range[2]; tileX++) {
                if (marked[tileY * tilesX + tileX]) {
                    return true;
                }
            }
        }
        return false;
    }
}
