package com.aryston.kinetic.core.lod;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import java.util.function.LongPredicate;

public final class LodPlanner {
    private static final int TOP_LEVEL = LodStore.LEVELS - 1;
    private static final int CHILDREN_PER_SIDE = 2;
    private static final double LEVEL_PRIORITY_BLOCKS = 256.0;

    private final LongArrayList keys = new LongArrayList();
    private final DoubleArrayList scores = new DoubleArrayList();
    private int incomplete;
    private double nearestIncomplete;

    public long[] plan(double cameraX, double cameraZ, double maxDistance, LodSelection.ClipBox clip, LodQuality quality, LongPredicate needed, int limit) {
        return plan(cameraX, cameraZ, maxDistance, clip, quality, needed, needed, limit);
    }

    public long[] plan(
        double cameraX, double cameraZ, double maxDistance, LodSelection.ClipBox clip, LodQuality quality, LongPredicate needed, LongPredicate missing, int limit
    ) {
        keys.clear();
        scores.clear();
        incomplete = 0;
        nearestIncomplete = Double.POSITIVE_INFINITY;
        int topSize = LodSelection.tileBlocks(TOP_LEVEL);
        int minTileX = (int) Math.floor((cameraX - maxDistance) / topSize);
        int maxTileX = (int) Math.floor((cameraX + maxDistance) / topSize);
        int minTileZ = (int) Math.floor((cameraZ - maxDistance) / topSize);
        int maxTileZ = (int) Math.floor((cameraZ + maxDistance) / topSize);
        for (int tileX = minTileX; tileX <= maxTileX; tileX++) {
            for (int tileZ = minTileZ; tileZ <= maxTileZ; tileZ++) {
                visit(TOP_LEVEL, tileX, tileZ, cameraX, cameraZ, maxDistance, clip, quality, needed, missing);
            }
        }
        return best(limit);
    }

    public int incomplete() {
        return incomplete;
    }

    public double nearestIncomplete() {
        return nearestIncomplete;
    }

    private void visit(
        int level, int tileX, int tileZ, double cameraX, double cameraZ, double maxDistance, LodSelection.ClipBox clip, LodQuality quality,
        LongPredicate needed, LongPredicate missing
    ) {
        int size = LodSelection.tileBlocks(level);
        double minX = (double) tileX * size;
        double minZ = (double) tileZ * size;
        if (clip.contains(minX, minZ, minX + size, minZ + size)) {
            return;
        }
        double distance = LodSelection.distance(cameraX, cameraZ, minX, minZ, size);
        if (distance > maxDistance) {
            return;
        }
        long key = LodStore.key(level, tileX, tileZ);
        if (missing.test(key)) {
            incomplete++;
            nearestIncomplete = Math.min(nearestIncomplete, distance);
        }
        if (needed.test(key)) {
            keys.add(key);
            scores.add(distance - level * LEVEL_PRIORITY_BLOCKS);
        }
        if (level > quality.finestLevel() && distance < quality.splitFactor() * size) {
            for (int dz = 0; dz < CHILDREN_PER_SIDE; dz++) {
                for (int dx = 0; dx < CHILDREN_PER_SIDE; dx++) {
                    visit(level - 1, tileX * CHILDREN_PER_SIDE + dx, tileZ * CHILDREN_PER_SIDE + dz, cameraX, cameraZ, maxDistance, clip, quality, needed, missing);
                }
            }
        }
    }

    private long[] best(int limit) {
        int count = Math.min(limit, keys.size());
        long[] result = new long[count];
        boolean[] taken = new boolean[keys.size()];
        for (int slot = 0; slot < count; slot++) {
            int best = -1;
            for (int index = 0; index < keys.size(); index++) {
                if (!taken[index] && (best < 0 || scores.getDouble(index) < scores.getDouble(best))) {
                    best = index;
                }
            }
            taken[best] = true;
            result[slot] = keys.getLong(best);
        }
        return result;
    }
}
