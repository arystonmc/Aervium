package com.aryston.kinetic.core.lod;

import it.unimi.dsi.fastutil.longs.LongArrayList;

public final class LodSelection {
    private static final int TOP_LEVEL = LodStore.LEVELS - 1;
    private static final int CHILDREN_PER_SIDE = 2;

    private final LongArrayList drawn = new LongArrayList();
    private final LongArrayList wanted = new LongArrayList();

    public void select(TileIndex index, double cameraX, double cameraZ, double maxDistance, ClipBox clip, LodQuality quality) {
        drawn.clear();
        wanted.clear();
        int topSize = tileBlocks(TOP_LEVEL);
        int minTileX = (int) Math.floor((cameraX - maxDistance) / topSize);
        int maxTileX = (int) Math.floor((cameraX + maxDistance) / topSize);
        int minTileZ = (int) Math.floor((cameraZ - maxDistance) / topSize);
        int maxTileZ = (int) Math.floor((cameraZ + maxDistance) / topSize);
        for (int tileX = minTileX; tileX <= maxTileX; tileX++) {
            for (int tileZ = minTileZ; tileZ <= maxTileZ; tileZ++) {
                visit(index, TOP_LEVEL, tileX, tileZ, cameraX, cameraZ, maxDistance, clip, quality);
            }
        }
    }

    public LongArrayList drawn() {
        return drawn;
    }

    public LongArrayList wanted() {
        return wanted;
    }

    public static int tileBlocks(int level) {
        return LodTile.SIZE << level;
    }

    private void visit(TileIndex index, int level, int tileX, int tileZ, double cameraX, double cameraZ, double maxDistance, ClipBox clip, LodQuality quality) {
        int size = tileBlocks(level);
        double minX = (double) tileX * size;
        double minZ = (double) tileZ * size;
        if (clip.contains(minX, minZ, minX + size, minZ + size)) {
            return;
        }
        double distance = distance(cameraX, cameraZ, minX, minZ, size);
        if (distance > maxDistance) {
            return;
        }
        long key = LodStore.key(level, tileX, tileZ);
        boolean meshed = index.meshed(key);
        if (!meshed && !index.exists(level, tileX, tileZ)) {
            return;
        }
        boolean split = level > quality.finestLevel() && distance < quality.splitFactor() * size;
        if (split && childrenReady(index, level - 1, tileX * CHILDREN_PER_SIDE, tileZ * CHILDREN_PER_SIDE, cameraX, cameraZ, maxDistance, clip, true)) {
            for (int dz = 0; dz < CHILDREN_PER_SIDE; dz++) {
                for (int dx = 0; dx < CHILDREN_PER_SIDE; dx++) {
                    visit(index, level - 1, tileX * CHILDREN_PER_SIDE + dx, tileZ * CHILDREN_PER_SIDE + dz, cameraX, cameraZ, maxDistance, clip, quality);
                }
            }
            return;
        }
        if (meshed) {
            drawn.add(key);
            if (split) {
                wantDescendants(index, level - 1, tileX * CHILDREN_PER_SIDE, tileZ * CHILDREN_PER_SIDE, cameraX, cameraZ, maxDistance, clip, quality);
            }
            return;
        }
        wanted.add(key);
        if (split) {
            for (int dz = 0; dz < CHILDREN_PER_SIDE; dz++) {
                for (int dx = 0; dx < CHILDREN_PER_SIDE; dx++) {
                    drawFallback(index, level - 1, tileX * CHILDREN_PER_SIDE + dx, tileZ * CHILDREN_PER_SIDE + dz, cameraX, cameraZ, maxDistance, clip, quality);
                }
            }
            wantDescendants(index, level - 1, tileX * CHILDREN_PER_SIDE, tileZ * CHILDREN_PER_SIDE, cameraX, cameraZ, maxDistance, clip, quality);
        }
    }

    private void drawFallback(TileIndex index, int level, int tileX, int tileZ, double cameraX, double cameraZ, double maxDistance, ClipBox clip, LodQuality quality) {
        int size = tileBlocks(level);
        double minX = (double) tileX * size;
        double minZ = (double) tileZ * size;
        if (clip.contains(minX, minZ, minX + size, minZ + size)) {
            return;
        }
        double distance = distance(cameraX, cameraZ, minX, minZ, size);
        if (distance > maxDistance) {
            return;
        }
        long key = LodStore.key(level, tileX, tileZ);
        boolean meshed = index.meshed(key);
        if (!meshed && !index.exists(level, tileX, tileZ)) {
            return;
        }
        boolean split = level > quality.finestLevel() && distance < quality.splitFactor() * size;
        if (meshed && (!split || !childrenReady(index, level - 1, tileX * CHILDREN_PER_SIDE, tileZ * CHILDREN_PER_SIDE, cameraX, cameraZ, maxDistance, clip, false))) {
            drawn.add(key);
            return;
        }
        if (split) {
            for (int dz = 0; dz < CHILDREN_PER_SIDE; dz++) {
                for (int dx = 0; dx < CHILDREN_PER_SIDE; dx++) {
                    drawFallback(index, level - 1, tileX * CHILDREN_PER_SIDE + dx, tileZ * CHILDREN_PER_SIDE + dz, cameraX, cameraZ, maxDistance, clip, quality);
                }
            }
        }
    }

    private void wantDescendants(TileIndex index, int level, int firstX, int firstZ, double cameraX, double cameraZ, double maxDistance, ClipBox clip, LodQuality quality) {
        if (level <= quality.finestLevel()) {
            return;
        }
        int size = tileBlocks(level);
        for (int dz = 0; dz < CHILDREN_PER_SIDE; dz++) {
            for (int dx = 0; dx < CHILDREN_PER_SIDE; dx++) {
                int tileX = firstX + dx;
                int tileZ = firstZ + dz;
                double minX = (double) tileX * size;
                double minZ = (double) tileZ * size;
                if (clip.contains(minX, minZ, minX + size, minZ + size)) {
                    continue;
                }
                double distance = distance(cameraX, cameraZ, minX, minZ, size);
                if (distance <= maxDistance && distance < quality.splitFactor() * size) {
                    wantChildren(index, level - 1, tileX * CHILDREN_PER_SIDE, tileZ * CHILDREN_PER_SIDE, cameraX, cameraZ, maxDistance, clip, quality);
                }
            }
        }
    }

    private void wantChildren(TileIndex index, int level, int firstX, int firstZ, double cameraX, double cameraZ, double maxDistance, ClipBox clip, LodQuality quality) {
        int size = tileBlocks(level);
        for (int dz = 0; dz < CHILDREN_PER_SIDE; dz++) {
            for (int dx = 0; dx < CHILDREN_PER_SIDE; dx++) {
                int tileX = firstX + dx;
                int tileZ = firstZ + dz;
                double minX = (double) tileX * size;
                double minZ = (double) tileZ * size;
                if (clip.contains(minX, minZ, minX + size, minZ + size) || distance(cameraX, cameraZ, minX, minZ, size) > maxDistance) {
                    continue;
                }
                long key = LodStore.key(level, tileX, tileZ);
                boolean meshed = index.meshed(key);
                if (!meshed && !index.exists(level, tileX, tileZ)) {
                    continue;
                }
                if (!meshed || !index.covered(key)) {
                    wanted.add(key);
                }
            }
        }
        wantDescendants(index, level, firstX, firstZ, cameraX, cameraZ, maxDistance, clip, quality);
    }

    private boolean childrenReady(TileIndex index, int level, int firstX, int firstZ, double cameraX, double cameraZ, double maxDistance, ClipBox clip, boolean collectWanted) {
        boolean ready = true;
        int size = tileBlocks(level);
        for (int dz = 0; dz < CHILDREN_PER_SIDE; dz++) {
            for (int dx = 0; dx < CHILDREN_PER_SIDE; dx++) {
                int tileX = firstX + dx;
                int tileZ = firstZ + dz;
                double minX = (double) tileX * size;
                double minZ = (double) tileZ * size;
                if (clip.contains(minX, minZ, minX + size, minZ + size) || distance(cameraX, cameraZ, minX, minZ, size) > maxDistance) {
                    continue;
                }
                long key = LodStore.key(level, tileX, tileZ);
                if (!index.meshed(key) && !index.exists(level, tileX, tileZ)) {
                    ready &= index.gapsAllowed();
                    continue;
                }
                if (!index.meshed(key) || !index.covered(key)) {
                    if (collectWanted) {
                        wanted.add(key);
                    }
                    ready = false;
                }
            }
        }
        return ready;
    }

    static double distance(double cameraX, double cameraZ, double minX, double minZ, int size) {
        double dx = Math.max(0.0, Math.max(minX - cameraX, cameraX - (minX + size)));
        double dz = Math.max(0.0, Math.max(minZ - cameraZ, cameraZ - (minZ + size)));
        return Math.sqrt(dx * dx + dz * dz);
    }

    public interface TileIndex {
        boolean exists(int level, int tileX, int tileZ);

        boolean meshed(long key);

        boolean gapsAllowed();

        default boolean covered(long key) {
            return true;
        }
    }

    public record ClipBox(double minX, double minZ, double maxX, double maxZ) {
        public static final ClipBox NONE = new ClipBox(0.0, 0.0, 0.0, 0.0);

        boolean contains(double fromX, double fromZ, double toX, double toZ) {
            return fromX >= minX && fromZ >= minZ && toX <= maxX && toZ <= maxZ;
        }
    }
}
