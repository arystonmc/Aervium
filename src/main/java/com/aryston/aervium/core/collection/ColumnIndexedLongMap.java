package com.aryston.aervium.core.collection;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.function.Predicate;

/**
 * A section map that also knows, per column, how many of its sections are stored and which hold a value. A search
 * over a column can then skip straight to the occupied sections instead of looking up every section of the column.
 * Only {@code put(long, V)} and {@code remove(long)} keep the index; the map is meant for owners that use only those.
 */
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

    /** True when every section of the column is stored, so a missing occupied bit really means no value. */
    public boolean complete(long column) {
        return stored.get(column) == sectionCount;
    }

    /** Bit i is set when the section at index i of the column holds a value. */
    public long occupiedMask(long column) {
        return occupiedMasks.get(column);
    }

    public interface Layout {
        long column(long key);

        /** Index of the section inside its column, from 0 to the section count. */
        int index(long key);
    }
}
