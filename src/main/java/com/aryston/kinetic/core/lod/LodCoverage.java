package com.aryston.kinetic.core.lod;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.Arrays;

public final class LodCoverage {
    public static final int SIZE = 256;
    private static final int OPAQUE = 255;
    private static final long READY_DELAY_NANOS = LodTransitions.DURATION_NANOS;

    private final Long2ObjectOpenHashMap<IntOpenHashSet> compiled = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectOpenHashMap<Column> columns = new Long2ObjectOpenHashMap<>();

    public boolean known(int x, int z) {
        return columns.containsKey(key(x, z));
    }

    public void require(int x, int z, long[] samples, long now) {
        IntOpenHashSet required = new IntOpenHashSet();
        for (long sample : samples) {
            if (LodSample.visible(sample)) {
                required.add(Math.floorDiv(LodSample.height(sample), LodStore.CHUNK_SIZE));
            }
        }
        Column previous = columns.get(key(x, z));
        Column column = previous != null && previous.required.equals(required) ? previous : new Column(required, previous == null ? 0L : previous.readyAt);
        columns.put(key(x, z), column);
        check(x, z, column, now);
    }

    public void compiled(int x, int y, int z, boolean ready, long now) {
        long key = key(x, z);
        IntOpenHashSet sections = compiled.computeIfAbsent(key, ignored -> new IntOpenHashSet());
        if (ready) {
            sections.add(y);
        } else {
            sections.remove(y);
        }
        if (sections.isEmpty()) {
            compiled.remove(key);
        }
        Column column = columns.get(key);
        if (column != null) {
            check(x, z, column, now);
        }
    }

    public int value(int x, int z, long now) {
        Column column = columns.get(key(x, z));
        return column == null || column.readyAt == 0L ? 0
            : Math.round(OPAQUE * Math.clamp((float) (now - column.readyAt - READY_DELAY_NANOS) / LodTransitions.DURATION_NANOS, 0.0F, 1.0F));
    }

    public void rasterize(byte[] pixels, long now) {
        if (pixels.length != SIZE * SIZE) {
            throw new IllegalArgumentException("Invalid distant terrain coverage image");
        }
        Arrays.fill(pixels, (byte) 0);
        for (long key : columns.keySet()) {
            int x = (int) (key >> Integer.SIZE);
            int z = (int) key;
            pixels[Math.floorMod(z, SIZE) * SIZE + Math.floorMod(x, SIZE)] = (byte) value(x, z, now);
        }
    }

    public void trim(int x, int z, int radius) {
        columns.keySet().removeIf(key -> outside(key, x, z, radius));
        compiled.keySet().removeIf(key -> outside(key, x, z, radius));
    }

    public void forget(int x, int z) {
        columns.remove(key(x, z));
        compiled.remove(key(x, z));
    }

    public void clear() {
        columns.clear();
        compiled.clear();
    }

    private void check(int x, int z, Column column, long now) {
        IntOpenHashSet sections = compiled.get(key(x, z));
        boolean ready = column.required.isEmpty() || sections != null && sections.containsAll(column.required);
        column.readyAt = ready ? column.readyAt == 0L ? now : column.readyAt : 0L;
    }

    private static boolean outside(long key, int x, int z, int radius) {
        return Math.abs((int) (key >> Integer.SIZE) - x) > radius || Math.abs((int) key - z) > radius;
    }

    private static long key(int x, int z) {
        return (long) x << Integer.SIZE | Integer.toUnsignedLong(z);
    }

    private static final class Column {
        private final IntOpenHashSet required;
        private long readyAt;

        private Column(IntOpenHashSet required, long readyAt) {
            this.required = required;
            this.readyAt = readyAt;
        }
    }
}
