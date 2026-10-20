package com.aryston.aervium.core.lod;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.Comparator;
import org.jspecify.annotations.Nullable;

public final class LodStore {
    public static final int LEVELS = 6;
    public static final int CHUNK_SIZE = 16;

    private static final int COORDINATE_BITS = 30;
    private static final long COORDINATE_MASK = (1L << COORDINATE_BITS) - 1;
    private static final int HALF = LodTile.SIZE / 2;
    private static final int MAX_LAYER_LEVEL = 2;

    private final Long2ObjectOpenHashMap<LodTile> tiles = new Long2ObjectOpenHashMap<>();
    private final LongLinkedOpenHashSet pending = new LongLinkedOpenHashSet();

    public void putChunk(int chunkX, int chunkZ, long[] columns) {
        putChunk(chunkX, chunkZ, columns, null);
    }

    public void putChunk(int chunkX, int chunkZ, long[] columns, @Nullable LodLayers layers) {
        int blockX = chunkX * CHUNK_SIZE;
        int blockZ = chunkZ * CHUNK_SIZE;
        LodTile tile = tileAt(0, Math.floorDiv(blockX, LodTile.SIZE), Math.floorDiv(blockZ, LodTile.SIZE));
        int before = tile.version();
        int originX = Math.floorMod(blockX, LodTile.SIZE);
        int originZ = Math.floorMod(blockZ, LodTile.SIZE);
        for (int z = 0; z < CHUNK_SIZE; z++) {
            for (int x = 0; x < CHUNK_SIZE; x++) {
                tile.set(originX + x, originZ + z, columns[z * CHUNK_SIZE + x]);
            }
        }
        replaceLayers(tile, layers, originX, originZ, CHUNK_SIZE, CHUNK_SIZE);
        if (tile.version() != before) {
            pending.add(key(0, tile.tileX(), tile.tileZ()));
        }
    }

    public void fillTile(int level, int tileX, int tileZ, long[] samples) {
        fillTile(level, tileX, tileZ, new LodData(samples, null));
    }

    public void fillTile(int level, int tileX, int tileZ, LodData data) {
        LodTile tile = tileAt(level, tileX, tileZ);
        int before = tile.version();
        long[] samples = data.samples();
        long[] current = tile.samples();
        LodLayers previous = tile.layers();
        LodLayers.Builder layers = previous == null && data.layers() == null ? null : new LodLayers.Builder(LodTile.SAMPLES);
        for (int index = 0; index < LodTile.SAMPLES; index++) {
            boolean replace = replaces(current[index], samples[index]);
            if (replace) {
                tile.set(index % LodTile.SIZE, index / LodTile.SIZE, samples[index]);
            }
            if (layers != null) {
                LodLayers source = replace ? data.layers() : previous;
                if (source != null) {
                    source.copyColumn(index, layers);
                }
                layers.finishColumn();
            }
        }
        if (layers != null) {
            tile.setLayers(layers.build());
        }
        if (tile.version() != before) {
            pending.add(key(level, tileX, tileZ));
        }
    }

    public boolean complete(long key) {
        LodTile tile = tiles.get(key);
        return tile != null && tile.complete();
    }

    public boolean exact(long key) {
        LodTile tile = tiles.get(key);
        return tile != null && tile.exact();
    }

    public int downsamplePending(int maxTiles) {
        int done = 0;
        while (done < maxTiles && !pending.isEmpty()) {
            long key = pending.removeFirstLong();
            LodTile child = tiles.get(key);
            if (child != null && child.level() + 1 < LEVELS) {
                downsample(child);
            }
            done++;
        }
        return done;
    }

    public LodTile tile(int level, int tileX, int tileZ) {
        return tiles.get(key(level, tileX, tileZ));
    }

    public int tileCount() {
        return tiles.size();
    }

    public int pendingCount() {
        return pending.size();
    }

    public long sampleBytes() {
        long bytes = 0L;
        for (LodTile tile : tiles.values()) {
            bytes += tile.bytes();
        }
        return bytes;
    }

    public LongOpenHashSet copyKeys() {
        return new LongOpenHashSet(tiles.keySet());
    }

    public void clear() {
        tiles.clear();
        pending.clear();
    }

    public void trim(double cameraX, double cameraZ, double distance, int limit) {
        trim(cameraX, cameraZ, distance, limit, DistantTerrainLimits.MAX_RESIDENT_BYTES);
    }

    void trim(double cameraX, double cameraZ, double distance, int limit, long byteLimit) {
        ArrayList<Resident> residents = new ArrayList<>(tiles.size());
        double reach = distance + LodSelection.tileBlocks(LEVELS - 1);
        for (var entry : tiles.long2ObjectEntrySet()) {
            LodTile tile = entry.getValue();
            int size = LodSelection.tileBlocks(tile.level());
            double away = LodSelection.distance(cameraX, cameraZ, (double) tile.tileX() * size, (double) tile.tileZ() * size, size);
            residents.add(new Resident(entry.getLongKey(), away > reach ? Double.POSITIVE_INFINITY : away / size));
        }
        residents.sort(Comparator.comparingDouble(Resident::score).reversed());
        long bytes = sampleBytes();
        for (Resident resident : residents) {
            if (resident.score() != Double.POSITIVE_INFINITY && tiles.size() <= limit && bytes <= byteLimit) {
                break;
            }
            LodTile removed = tiles.remove(resident.key());
            bytes -= removed.bytes();
            pending.remove(resident.key());
        }
    }

    public LodBorders borders(LodTile tile) {
        int scale = 1 << tile.level();
        int size = LodSelection.tileBlocks(tile.level());
        int originX = tile.tileX() * size;
        int originZ = tile.tileZ() * size;
        int[] tops = new int[LodBorders.SIDES * LodTile.SIZE];
        for (int side = 0; side < LodBorders.SIDES; side++) {
            boolean horizontal = side < LodBorders.WEST;
            int across = horizontal ? originZ : originX;
            across += side == LodBorders.NORTH || side == LodBorders.WEST ? -1 : size;
            for (int cell = 0; cell < LodTile.SIZE; cell++) {
                int start = (horizontal ? originX : originZ) + cell * scale;
                tops[side * LodTile.SIZE + cell] = borderTop(horizontal, across, start, scale);
            }
        }
        return new LodBorders(tops);
    }

    private int borderTop(boolean horizontal, int across, int start, int length) {
        int lowest = LodBorders.UNKNOWN;
        for (int level = 0; level < LEVELS; level++) {
            int scale = 1 << level;
            int first = Math.floorDiv(start, scale);
            int last = Math.floorDiv(start + length - 1, scale);
            int cross = Math.floorDiv(across, scale);
            for (int along = first; along <= last; along++) {
                int x = horizontal ? along : cross;
                int z = horizontal ? cross : along;
                LodTile neighbour = tile(level, Math.floorDiv(x, LodTile.SIZE), Math.floorDiv(z, LodTile.SIZE));
                if (neighbour != null) {
                    long sample = neighbour.sample(Math.floorMod(x, LodTile.SIZE), Math.floorMod(z, LodTile.SIZE));
                    if (LodSample.visible(sample)) {
                        lowest = Math.min(lowest, LodSample.height(sample) + 1);
                    }
                }
            }
        }
        return lowest;
    }

    private record Resident(long key, double score) {
    }

    public static long key(int level, int tileX, int tileZ) {
        return ((long) level << (2 * COORDINATE_BITS)) | ((tileX & COORDINATE_MASK) << COORDINATE_BITS) | (tileZ & COORDINATE_MASK);
    }

    public static int levelOf(long key) {
        return (int) (key >>> (2 * COORDINATE_BITS));
    }

    public static int tileXOf(long key) {
        return signed(key >>> COORDINATE_BITS);
    }

    public static int tileZOf(long key) {
        return signed(key);
    }

    private static int signed(long field) {
        int shift = Long.SIZE - COORDINATE_BITS;
        return (int) ((field & COORDINATE_MASK) << shift >> shift);
    }

    private void downsample(LodTile child) {
        LodTile parent = tileAt(child.level() + 1, Math.floorDiv(child.tileX(), 2), Math.floorDiv(child.tileZ(), 2));
        int before = parent.version();
        int offsetX = Math.floorMod(child.tileX(), 2) * HALF;
        int offsetZ = Math.floorMod(child.tileZ(), 2) * HALF;
        long[] samples = child.samples();
        LodLayers.Builder layers = child.level() < MAX_LAYER_LEVEL && (child.layers() != null || parent.layers() != null)
            ? new LodLayers.Builder(HALF * HALF) : null;
        for (int z = 0; z < HALF; z++) {
            for (int x = 0; x < HALF; x++) {
                long combined = LodCombine.of(samples, 2 * z * LodTile.SIZE + 2 * x, LodTile.SIZE);
                long previous = parent.sample(offsetX + x, offsetZ + z);
                boolean replace = replaces(previous, combined)
                    || (LodSample.priority(combined) == LodSample.priority(previous) && LodSample.priority(combined) >= LodSample.SEEDED_PRIORITY);
                if (replace) {
                    parent.set(offsetX + x, offsetZ + z, combined);
                }
                if (layers != null) {
                    LodLayers source = child.layers();
                    int selected = 2 * z * LodTile.SIZE + 2 * x;
                    for (int dz = 0; dz < 2; dz++) {
                        for (int dx = 0; dx < 2; dx++) {
                            int candidate = (2 * z + dz) * LodTile.SIZE + 2 * x + dx;
                            if (LodSample.height(samples[candidate]) > LodSample.height(samples[selected])) {
                                selected = candidate;
                            }
                        }
                    }
                    if (replace && source != null) {
                        source.copyColumn(selected, layers);
                    } else if (!replace && parent.layers() != null) {
                        parent.layers().copyColumn((offsetZ + z) * LodTile.SIZE + offsetX + x, layers);
                    }
                    layers.finishColumn();
                }
            }
        }
        if (layers != null) {
            replaceLayers(parent, layers.build(), offsetX, offsetZ, HALF, HALF);
        }
        if (parent.version() != before) {
            pending.add(key(parent.level(), parent.tileX(), parent.tileZ()));
        }
    }

    private static void replaceLayers(LodTile tile, @Nullable LodLayers incoming, int fromX, int fromZ, int width, int depth) {
        LodLayers previous = tile.layers();
        if (previous == null && incoming == null) {
            return;
        }
        LodLayers.Builder result = new LodLayers.Builder(LodTile.SAMPLES);
        for (int z = 0; z < LodTile.SIZE; z++) {
            for (int x = 0; x < LodTile.SIZE; x++) {
                boolean inside = x >= fromX && z >= fromZ && x < fromX + width && z < fromZ + depth;
                LodLayers source = inside ? incoming : previous;
                if (source != null) {
                    int column = inside ? (z - fromZ) * width + x - fromX : z * LodTile.SIZE + x;
                    source.copyColumn(column, result);
                }
                result.finishColumn();
            }
        }
        tile.setLayers(result.build());
    }

    private static boolean replaces(long previous, long incoming) {
        if (!LodSample.present(incoming)) {
            return false;
        }
        if (LodSample.complete(previous) != LodSample.complete(incoming)) {
            return LodSample.complete(incoming);
        }
        int previousPriority = LodSample.priority(previous);
        int incomingPriority = LodSample.priority(incoming);
        if (previousPriority != incomingPriority) {
            return previousPriority < incomingPriority;
        }
        return !LodSample.complete(previous) || incomingPriority >= LodSample.SAVED_PRIORITY;
    }

    private LodTile tileAt(int level, int tileX, int tileZ) {
        return tiles.computeIfAbsent(key(level, tileX, tileZ), ignored -> new LodTile(level, tileX, tileZ));
    }
}
