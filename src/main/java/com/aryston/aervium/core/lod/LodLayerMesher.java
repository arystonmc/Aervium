package com.aryston.aervium.core.lod;

import java.util.HashMap;
import java.util.Map;

final class LodLayerMesher {
    private LodLayerMesher() {
    }

    static void emit(LodTile tile, LodMesher.Writer writer, LodBorders borders, boolean water) {
        int scale = 1 << tile.level();
        int skirt = Math.max(LodMesher.MIN_SKIRT, LodMesher.SKIRT_SAMPLES * scale);
        surfaces(tile, writer, water, scale, skirt);
        for (int side = 0; side < LodBorders.SIDES; side++) {
            boolean horizontal = side < LodBorders.WEST;
            for (int line = 0; line < LodTile.SIZE; line++) {
                Map<Face, Run> runs = new HashMap<>();
                for (int along = 0; along < LodTile.SIZE; along++) {
                    int x = horizontal ? along : line;
                    int z = horizontal ? line : along;
                    int column = z * LodTile.SIZE + x;
                    for (int layer = 0; layer < count(tile, column); layer++) {
                        long sample = surface(tile, column, layer);
                        if (LodSample.water(sample) != water) {
                            continue;
                        }
                        int bottom = bottom(tile, column, layer, skirt);
                        int top = LodSample.height(sample) + 1;
                        LodLayers layers = tile.layers();
                        if (layers != null && layer < layers.end(column) - layers.start(column)) {
                            sample = LodSample.withColor(sample, layers.side(layers.start(column) + layer));
                        }
                        int nx = x + (side == LodBorders.WEST ? -1 : side == LodBorders.EAST ? 1 : 0);
                        int nz = z + (side == LodBorders.NORTH ? -1 : side == LodBorders.SOUTH ? 1 : 0);
                        if (nx < 0 || nz < 0 || nx >= LodTile.SIZE || nz >= LodTile.SIZE) {
                            int neighbour = borders.top(side, along);
                            if (layer + 1 == count(tile, column) && !water && neighbour != LodBorders.UNKNOWN) {
                                bottom = Math.min(bottom, neighbour - LodMesher.EDGE_OVERLAP);
                            }
                            add(runs, writer, sample, top, bottom, along, side, line, scale);
                        } else {
                            exposed(tile, runs, writer, sample, top, bottom, nz * LodTile.SIZE + nx, along, side, line, scale, skirt);
                        }
                    }
                }
                for (var entry : runs.entrySet()) {
                    wall(writer, entry.getKey(), entry.getValue(), side, line, scale);
                }
            }
        }
    }

    private static void surfaces(LodTile tile, LodMesher.Writer writer, boolean water, int scale, int skirt) {
        for (int layer = 0; layer < LodLayers.MAX_LAYERS; layer++) {
            boolean[] emitted = new boolean[LodTile.SAMPLES];
            for (int z = 0; z < LodTile.SIZE; z++) {
                for (int x = 0; x < LodTile.SIZE; x++) {
                    int column = z * LodTile.SIZE + x;
                    long sample = surface(tile, column, layer);
                    if (emitted[column] || !LodSample.visible(sample) || LodSample.water(sample) != water) {
                        continue;
                    }
                    int lower = bottom(tile, column, layer, skirt);
                    int width = 1;
                    while (x + width < LodTile.SIZE && matches(tile, emitted, sample, lower, z * LodTile.SIZE + x + width, layer, skirt)) {
                        width++;
                    }
                    int depth = 1;
                    while (z + depth < LodTile.SIZE && row(tile, emitted, sample, lower, x, z + depth, width, layer, skirt)) {
                        depth++;
                    }
                    for (int dz = 0; dz < depth; dz++) {
                        for (int dx = 0; dx < width; dx++) {
                            emitted[(z + dz) * LodTile.SIZE + x + dx] = true;
                        }
                    }
                    float x0 = x * scale;
                    float z0 = z * scale;
                    float x1 = (x + width) * scale;
                    float z1 = (z + depth) * scale;
                    float top = LodSample.height(sample) + 1;
                    writer.quad(sample, x0, top, z0, x0, top, z1, x1, top, z1, x1, top, z0);
                    if (!water) {
                        writer.quad(sample, x0, lower, z0, x1, lower, z0, x1, lower, z1, x0, lower, z1);
                    }
                }
            }
        }
    }

    private static boolean row(LodTile tile, boolean[] emitted, long sample, int bottom, int x, int z, int width, int layer, int skirt) {
        for (int dx = 0; dx < width; dx++) {
            if (!matches(tile, emitted, sample, bottom, z * LodTile.SIZE + x + dx, layer, skirt)) {
                return false;
            }
        }
        return true;
    }

    private static boolean matches(LodTile tile, boolean[] emitted, long sample, int lower, int column, int layer, int skirt) {
        return !emitted[column] && LodMesher.sameSurface(sample, surface(tile, column, layer)) && lower == bottom(tile, column, layer, skirt);
    }

    private static void exposed(LodTile tile, Map<Face, Run> runs, LodMesher.Writer writer, long sample, int top, int lower,
        int neighbour, int along, int side, int line, int scale, int skirt) {
        int cursor = top;
        for (int layer = 0; layer < count(tile, neighbour) && cursor > lower; layer++) {
            long other = surface(tile, neighbour, layer);
            if (!LodSample.water(sample) && LodSample.water(other)) {
                continue;
            }
            int upper = LodSample.height(other) + 1;
            int bottom = bottom(tile, neighbour, layer, skirt);
            if (bottom >= cursor || upper <= lower) {
                continue;
            }
            add(runs, writer, sample, cursor, Math.max(lower, upper), along, side, line, scale);
            cursor = Math.min(cursor, bottom);
        }
        add(runs, writer, sample, cursor, lower, along, side, line, scale);
    }

    private static void add(Map<Face, Run> runs, LodMesher.Writer writer, long sample, int top, int bottom, int along, int side, int line, int scale) {
        if (bottom >= top) {
            return;
        }
        Face face = new Face(sample, top, bottom);
        Run previous = runs.get(face);
        if (previous != null && previous.end() == along) {
            runs.put(face, new Run(previous.start(), along + 1));
        } else {
            if (previous != null) {
                wall(writer, face, previous, side, line, scale);
            }
            runs.put(face, new Run(along, along + 1));
        }
    }

    private static void wall(LodMesher.Writer writer, Face face, Run run, int side, int line, int scale) {
        LodMesher.wall(writer, face.sample(), side, line * scale, run.start() * scale, run.end() * scale, scale, face.top(), face.bottom());
    }

    private static int count(LodTile tile, int column) {
        LodLayers layers = tile.layers();
        int count = layers == null ? 0 : layers.end(column) - layers.start(column);
        return count == 0 && LodSample.visible(tile.samples()[column]) ? 1 : count;
    }

    private static long surface(LodTile tile, int column, int layer) {
        LodLayers layers = tile.layers();
        if (layers != null && layers.end(column) > layers.start(column)) {
            return layer < layers.end(column) - layers.start(column) ? layers.surface(layers.start(column) + layer) : LodSample.EMPTY;
        }
        return layer == 0 ? tile.samples()[column] : LodSample.EMPTY;
    }

    private static int bottom(LodTile tile, int column, int layer, int skirt) {
        LodLayers layers = tile.layers();
        return layers != null && layer < layers.end(column) - layers.start(column)
            ? layers.bottom(layers.start(column) + layer) : LodSample.height(surface(tile, column, layer)) + 1 - skirt;
    }

    private record Face(long sample, int top, int bottom) {
    }

    private record Run(int start, int end) {
    }
}
