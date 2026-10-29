package com.aryston.kinetic.core.lod;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.List;

public final class LodTransitions {
    public static final long DURATION_NANOS = 250_000_000L;

    private LongOpenHashSet selected = new LongOpenHashSet();
    private final LongOpenHashSet retained = new LongOpenHashSet();
    private List<Region> regions = List.of();
    private long started;
    private boolean settled = true;

    public void update(LongArrayList wanted, long now) {
        if (progress(now) < 1.0F) {
            return;
        }
        if (!settled) {
            regions = selected.longStream().mapToObj(key -> overlap(key, key)).toList();
            retained.clear();
            retained.addAll(selected);
            settled = true;
        }
        LongOpenHashSet next = new LongOpenHashSet(wanted);
        if (selected.equals(next)) {
            return;
        }
        ArrayList<Region> result = new ArrayList<>();
        retained.clear();
        for (long key : next) {
            ArrayList<Region> uncovered = new ArrayList<>();
            uncovered.add(overlap(key, key));
            for (long previous : selected) {
                Region overlap = overlap(previous, key);
                if (overlap != null) {
                    result.add(overlap);
                    retained.add(previous);
                    ArrayList<Region> remaining = new ArrayList<>();
                    for (Region area : uncovered) {
                        subtract(area, overlap, remaining);
                    }
                    uncovered = remaining;
                }
            }
            result.addAll(uncovered);
        }
        retained.addAll(next);
        regions = List.copyOf(result);
        selected = next;
        started = now;
        settled = false;
    }

    public List<Region> regions() {
        return regions;
    }

    public boolean retains(long key) {
        return retained.contains(key);
    }

    public float progress(long now) {
        return started == 0L ? 1.0F : Math.clamp((float) (now - started) / DURATION_NANOS, 0.0F, 1.0F);
    }

    public void clear() {
        selected.clear();
        retained.clear();
        regions = List.of();
        started = 0L;
        settled = true;
    }

    private static void subtract(Region area, Region cut, List<Region> remaining) {
        int x0 = Math.max(area.minX(), cut.minX());
        int z0 = Math.max(area.minZ(), cut.minZ());
        int x1 = Math.min(area.maxX(), cut.maxX());
        int z1 = Math.min(area.maxZ(), cut.maxZ());
        if (x0 >= x1 || z0 >= z1) {
            remaining.add(area);
            return;
        }
        add(remaining, area.next(), area.minX(), area.minZ(), x0, area.maxZ());
        add(remaining, area.next(), x1, area.minZ(), area.maxX(), area.maxZ());
        add(remaining, area.next(), x0, area.minZ(), x1, z0);
        add(remaining, area.next(), x0, z1, x1, area.maxZ());
    }

    private static void add(List<Region> result, long key, int x0, int z0, int x1, int z1) {
        if (x0 < x1 && z0 < z1) {
            result.add(new Region(key, key, x0, z0, x1, z1));
        }
    }

    private static Region overlap(long previous, long next) {
        int previousSize = LodSelection.tileBlocks(LodStore.levelOf(previous));
        int nextSize = LodSelection.tileBlocks(LodStore.levelOf(next));
        int ax = LodStore.tileXOf(previous) * previousSize;
        int az = LodStore.tileZOf(previous) * previousSize;
        int bx = LodStore.tileXOf(next) * nextSize;
        int bz = LodStore.tileZOf(next) * nextSize;
        int minX = Math.max(ax, bx);
        int minZ = Math.max(az, bz);
        int maxX = Math.min(ax + previousSize, bx + nextSize);
        int maxZ = Math.min(az + previousSize, bz + nextSize);
        return minX < maxX && minZ < maxZ ? new Region(previous, next, minX, minZ, maxX, maxZ) : null;
    }

    public record Region(long previous, long next, int minX, int minZ, int maxX, int maxZ) {
    }
}
