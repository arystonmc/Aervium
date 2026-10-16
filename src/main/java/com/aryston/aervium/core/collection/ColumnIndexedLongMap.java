package com.aryston.aervium.core.collection;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.function.Predicate;

public final class ColumnIndexedLongMap<V> extends Long2ObjectOpenHashMap<V> {
    public static final int MAX_SECTIONS = Long.SIZE;

    private final Layout layout;
    private final Predicate<V> occupied;
    private final int sectionCount;
    private final Long2IntOpenHashMap stored = new Long2IntOpenHashMap();
    private final Long2LongOpenHashMap occupiedMasks = new Long2LongOpenHashMap();

    public ColumnIndexedLongMap(Layout layout, Predicate<V> occupied, int sectionCount) {
        if (sectionCount <= 0 || sectionCount > MAX_SECTIONS) {
            throw new IllegalArgumentException("Column index supports 1 to " + MAX_SECTIONS + " sections, got " + sectionCount);
        }
        this.layout = layout;
        this.occupied = occupied;
        this.sectionCount = sectionCount;
    }

    public static boolean supports(int sectionCount) {
        return sectionCount > 0 && sectionCount <= MAX_SECTIONS;
    }

    @Override
    public V put(long key, V value) {
        V previous = super.put(key, value);
        long column = layout.column(key);
        if (previous == null) {
            stored.addTo(column, 1);
        }
        long bit = 1L << layout.index(key);
        long mask = occupiedMasks.get(column);
        mask = occupied.test(value) ? mask | bit : mask & ~bit;
        if (mask == 0L) {
            occupiedMasks.remove(column);
        } else {
            occupiedMasks.put(column, mask);
        }
        return previous;
    }

    @Override
    public V remove(long key) {
        V previous = super.remove(key);
        if (previous != null) {
            long column = layout.column(key);
            if (stored.addTo(column, -1) == 1) {
                stored.remove(column);
            }
            long mask = occupiedMasks.get(column) & ~(1L << layout.index(key));
            if (mask == 0L) {
                occupiedMasks.remove(column);
            } else {
                occupiedMasks.put(column, mask);
            }
        }
        return previous;
    }

    public boolean complete(long column) {
        return stored.get(column) == sectionCount;
    }

    public long occupiedMask(long column) {
        return occupiedMasks.get(column);
    }

    public interface Layout {
        long column(long key);

        int index(long key);
    }
}
