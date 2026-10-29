package com.aryston.kinetic.core.lod;

import java.util.Arrays;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;

public final class LodLayers {
    public static final int MAX_LAYERS = 8;

    private final int[] offsets;
    private final long[] surfaces;
    private final short[] bottoms;
    private final int[] sides;

    public LodLayers(int[] offsets, long[] surfaces, short[] bottoms) {
        this(offsets, surfaces, bottoms, defaultSides(surfaces));
    }

    public LodLayers(int[] offsets, long[] surfaces, short[] bottoms, int[] sides) {
        if (offsets.length < 1 || offsets[0] != 0 || surfaces.length != bottoms.length || surfaces.length != sides.length || offsets[offsets.length - 1] != surfaces.length) {
            throw new IllegalArgumentException("Invalid distant terrain layers");
        }
        for (int column = 0; column + 1 < offsets.length; column++) {
            int from = offsets[column];
            int to = offsets[column + 1];
            if (from < 0 || to < from || to > surfaces.length || to - from > MAX_LAYERS) {
                throw new IllegalArgumentException("Invalid distant terrain column offsets");
            }
            int previousBottom = Integer.MAX_VALUE;
            for (int span = from; span < to; span++) {
                int top = LodSample.height(surfaces[span]);
                if (!LodSample.visible(surfaces[span]) || bottoms[span] > top || top >= previousBottom) {
                    throw new IllegalArgumentException("Overlapping distant terrain layers");
                }
                previousBottom = bottoms[span];
            }
        }
        this.offsets = offsets;
        this.surfaces = surfaces;
        this.bottoms = bottoms;
        this.sides = sides;
    }

    public int columns() {
        return offsets.length - 1;
    }

    public int start(int column) {
        return offsets[column];
    }

    public int end(int column) {
        return offsets[column + 1];
    }

    public long surface(int span) {
        return surfaces[span];
    }

    public int bottom(int span) {
        return bottoms[span];
    }

    public int side(int span) {
        return sides[span];
    }

    public int spans() {
        return surfaces.length;
    }

    public long bytes() {
        return (long) offsets.length * Integer.BYTES + (long) surfaces.length * (Long.BYTES + Short.BYTES + Integer.BYTES);
    }

    public boolean matches(LodLayers other) {
        return Arrays.equals(offsets, other.offsets) && Arrays.equals(surfaces, other.surfaces) && Arrays.equals(bottoms, other.bottoms) && Arrays.equals(sides, other.sides);
    }

    public void copyColumn(int column, Builder builder) {
        for (int span = start(column); span < end(column); span++) {
            builder.add(surface(span), bottom(span), side(span));
        }
    }

    private static int[] defaultSides(long[] surfaces) {
        int[] sides = new int[surfaces.length];
        for (int index = 0; index < sides.length; index++) {
            sides[index] = LodSample.rgb(surfaces[index]);
        }
        return sides;
    }

    public static final class Builder {
        private final int[] offsets;
        private final LongArrayList surfaces = new LongArrayList();
        private final ShortArrayList bottoms = new ShortArrayList();
        private final IntArrayList sides = new IntArrayList();
        private int column;
        private int count;

        public Builder(int columns) {
            offsets = new int[columns + 1];
        }

        public void add(long surface, int bottom) {
            add(surface, bottom, LodSample.rgb(surface));
        }

        public void add(long surface, int bottom, int side) {
            if (count >= MAX_LAYERS || bottom < Short.MIN_VALUE || bottom > Short.MAX_VALUE || bottom > LodSample.height(surface)) {
                throw new IllegalArgumentException("Invalid distant terrain span");
            }
            surfaces.add(surface);
            bottoms.add((short) bottom);
            sides.add(side);
            count++;
        }

        public void finishColumn() {
            offsets[++column] = surfaces.size();
            count = 0;
        }

        public LodLayers build() {
            if (column + 1 != offsets.length) {
                throw new IllegalStateException("Incomplete distant terrain layer snapshot");
            }
            return new LodLayers(offsets, surfaces.toLongArray(), bottoms.toShortArray(), sides.toIntArray());
        }
    }
}
