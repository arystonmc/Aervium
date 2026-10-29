package com.aryston.kinetic.core.lod;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class LodMesher {
    public static final int VERTEX_BYTES = 16;
    public static final int VERTICES_PER_QUAD = 4;

    private static final int QUAD_BYTES = VERTEX_BYTES * VERTICES_PER_QUAD;
    static final int SKIRT_SAMPLES = 2;
    static final int MIN_SKIRT = 8;
    static final int EDGE_OVERLAP = 1;
    private static final int MAX_LAYER_QUADS = 65_536;
    private static final int CHANNEL_MASK = 0xFF;
    private static final int RED_SHIFT = 16;
    private static final int GREEN_SHIFT = 8;

    private LodMesher() {
    }

    public static LodMesh mesh(long key, LodTile tile) {
        return mesh(key, tile, LodBorders.NONE);
    }

    public static LodMesh mesh(long key, LodTile tile, LodBorders borders) {
        Writer counter = new Writer(ByteBuffer.allocate(0));
        boolean layered = tile.layers() != null;
        emit(tile, counter, borders, false, layered);
        int opaque = counter.quads;
        emit(tile, counter, borders, true, layered);
        if (counter.quads > MAX_LAYER_QUADS) {
            layered = false;
            counter = new Writer(ByteBuffer.allocate(0));
            emit(tile, counter, borders, false, false);
            opaque = counter.quads;
            emit(tile, counter, borders, true, false);
        }
        ByteBuffer vertices = ByteBuffer.allocateDirect(counter.quads * QUAD_BYTES).order(ByteOrder.nativeOrder());
        Writer writer = new Writer(vertices);
        emit(tile, writer, borders, false, layered);
        emit(tile, writer, borders, true, layered);
        vertices.flip();
        float minY = writer.quads == 0 ? 0.0F : writer.minY;
        float maxY = writer.quads == 0 ? 0.0F : writer.maxY;
        return new LodMesh(key, tile.version(), vertices, counter.quads, opaque, tile.complete(), minY, maxY);
    }

    private static void emit(LodTile tile, Writer writer, LodBorders borders, boolean water, boolean layered) {
        if (layered) {
            LodLayerMesher.emit(tile, writer, borders, water);
            return;
        }
        int scale = 1 << tile.level();
        int skirt = Math.max(MIN_SKIRT, SKIRT_SAMPLES * scale);
        tops(tile, writer, scale, water);
        for (int side = 0; side < LodBorders.SIDES; side++) {
            walls(tile, writer, scale, skirt, side, borders, water);
        }
    }

    private static void tops(LodTile tile, Writer writer, int scale, boolean water) {
        boolean[] emitted = new boolean[LodTile.SAMPLES];
        for (int z = 0; z < LodTile.SIZE; z++) {
            for (int x = 0; x < LodTile.SIZE; x++) {
                long sample = tile.sample(x, z);
                if (emitted[z * LodTile.SIZE + x] || !LodSample.visible(sample) || LodSample.water(sample) != water) {
                    continue;
                }
                int width = 1;
                while (x + width < LodTile.SIZE && matches(tile, emitted, sample, x + width, z)) {
                    width++;
                }
                int depth = 1;
                while (z + depth < LodTile.SIZE && rowMatches(tile, emitted, sample, x, z + depth, width)) {
                    depth++;
                }
                for (int dz = 0; dz < depth; dz++) {
                    for (int dx = 0; dx < width; dx++) {
                        emitted[(z + dz) * LodTile.SIZE + x + dx] = true;
                    }
                }
                float top = LodSample.height(sample) + 1;
                float x0 = x * scale;
                float z0 = z * scale;
                float x1 = (x + width) * scale;
                float z1 = (z + depth) * scale;
                writer.quad(sample, x0, top, z0, x0, top, z1, x1, top, z1, x1, top, z0);
            }
        }
    }

    private static boolean rowMatches(LodTile tile, boolean[] emitted, long sample, int x, int z, int width) {
        for (int dx = 0; dx < width; dx++) {
            if (!matches(tile, emitted, sample, x + dx, z)) {
                return false;
            }
        }
        return true;
    }

    private static boolean matches(LodTile tile, boolean[] emitted, long sample, int x, int z) {
        return !emitted[z * LodTile.SIZE + x] && sameSurface(sample, tile.sample(x, z));
    }

    static boolean sameSurface(long first, long second) {
        return LodSample.visible(second) && LodSample.height(first) == LodSample.height(second)
            && LodSample.rgb(first) == LodSample.rgb(second) && LodSample.vertexLight(first) == LodSample.vertexLight(second);
    }

    private static void walls(LodTile tile, Writer writer, int scale, int skirt, int side, LodBorders borders, boolean water) {
        boolean horizontal = side < LodBorders.WEST;
        for (int line = 0; line < LodTile.SIZE; line++) {
            int start = 0;
            while (start < LodTile.SIZE) {
                int x = horizontal ? start : line;
                int z = horizontal ? line : start;
                long sample = tile.sample(x, z);
                int top = LodSample.height(sample) + 1;
                int bottom = wallBottom(tile, x, z, side, skirt, top, borders);
                if (!LodSample.visible(sample) || LodSample.water(sample) != water || bottom >= top) {
                    start++;
                    continue;
                }
                int end = start + 1;
                while (end < LodTile.SIZE) {
                    int nextX = horizontal ? end : line;
                    int nextZ = horizontal ? line : end;
                    if (!sameSurface(sample, tile.sample(nextX, nextZ)) || wallBottom(tile, nextX, nextZ, side, skirt, top, borders) != bottom) {
                        break;
                    }
                    end++;
                }
                wall(writer, sample, side, line * scale, start * scale, end * scale, scale, top, bottom);
                start = end;
            }
        }
    }

    private static int wallBottom(LodTile tile, int x, int z, int side, int skirt, int top, LodBorders borders) {
        int neighbourX = x + (side == LodBorders.WEST ? -1 : side == LodBorders.EAST ? 1 : 0);
        int neighbourZ = z + (side == LodBorders.NORTH ? -1 : side == LodBorders.SOUTH ? 1 : 0);
        if (neighbourX < 0 || neighbourZ < 0 || neighbourX >= LodTile.SIZE || neighbourZ >= LodTile.SIZE) {
            int neighbourTop = borders.top(side, side < LodBorders.WEST ? x : z);
            return neighbourTop == LodBorders.UNKNOWN ? top - skirt : Math.min(top - skirt, neighbourTop - EDGE_OVERLAP);
        }
        long neighbour = tile.sample(neighbourX, neighbourZ);
        return LodSample.visible(neighbour) ? LodSample.height(neighbour) + 1 : top - skirt;
    }

    static void wall(Writer writer, long sample, int side, float line, float start, float end, int scale, float top, float bottom) {
        switch (side) {
            case LodBorders.NORTH -> writer.quad(sample, end, top, line, end, bottom, line, start, bottom, line, start, top, line);
            case LodBorders.SOUTH -> writer.quad(sample, start, top, line + scale, start, bottom, line + scale, end, bottom, line + scale, end, top, line + scale);
            case LodBorders.WEST -> writer.quad(sample, line, top, start, line, bottom, start, line, bottom, end, line, top, end);
            case LodBorders.EAST -> writer.quad(sample, line + scale, top, end, line + scale, bottom, end, line + scale, bottom, start, line + scale, top, start);
            default -> throw new IllegalArgumentException("Unknown terrain side");
        }
    }

    static final class Writer {
        private final ByteBuffer vertices;
        private int quads;
        private float minY = Float.POSITIVE_INFINITY;
        private float maxY = Float.NEGATIVE_INFINITY;

        private Writer(ByteBuffer vertices) {
            this.vertices = vertices;
        }

        void quad(long sample,
            float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, float dx, float dy, float dz) {
            quads++;
            if (vertices.capacity() == 0) {
                return;
            }
            int rgb = LodSample.rgb(sample);
            byte red = (byte) ((rgb >> RED_SHIFT) & CHANNEL_MASK);
            byte green = (byte) ((rgb >> GREEN_SHIFT) & CHANNEL_MASK);
            byte blue = (byte) (rgb & CHANNEL_MASK);
            byte light = LodSample.vertexLight(sample);
            vertex(ax, ay, az, red, green, blue, light);
            vertex(bx, by, bz, red, green, blue, light);
            vertex(cx, cy, cz, red, green, blue, light);
            vertex(dx, dy, dz, red, green, blue, light);
        }

        private void vertex(float x, float y, float z, byte red, byte green, byte blue, byte light) {
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
            vertices.putFloat(x).putFloat(y).putFloat(z).put(red).put(green).put(blue).put(light);
        }
    }
}
