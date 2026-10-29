package com.aryston.kinetic.core.lod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class LodMesherTest {
    private static final int GREEN = 0x00FF00;

    @Test
    void aSingleColumnGetsATopAndFourSkirts() {
        LodTile tile = new LodTile(0, 0, 0);
        tile.set(10, 10, LodSample.of(70, GREEN, 0, 0));
        LodMesh mesh = LodMesher.mesh(LodStore.key(0, 0, 0), tile);
        assertEquals(5, mesh.quads());
        assertEquals(5 * LodMesher.VERTICES_PER_QUAD * LodMesher.VERTEX_BYTES, mesh.vertices().remaining());
    }

    @Test
    void aFlatAreaOnlyNeedsWallsAtItsEdges() {
        LodTile tile = new LodTile(0, 0, 0);
        for (int z = 0; z < LodTile.SIZE; z++) {
            for (int x = 0; x < LodTile.SIZE; x++) {
                tile.set(x, z, LodSample.of(64, GREEN, 0, 0));
            }
        }
        LodMesh mesh = LodMesher.mesh(LodStore.key(0, 0, 0), tile);
        assertEquals(5, mesh.quads());
        assertEquals(320, mesh.vertices().remaining());
        assertTrue(mesh.complete());
    }

    @Test
    void rectanglesPreserveHeightColourAndEveryCoveredCell() {
        LodTile tile = new LodTile(0, 0, 0);
        int half = LodTile.SIZE / 2;
        for (int z = 0; z < LodTile.SIZE; z++) {
            for (int x = 0; x < LodTile.SIZE; x++) {
                tile.set(x, z, LodSample.of(x < half ? 64 : 70, z < half ? GREEN : 0x0000FF, 0, 0));
            }
        }
        LodMesh mesh = LodMesher.mesh(LodStore.key(0, 0, 0), tile);
        ByteBuffer vertices = mesh.vertices();
        int topQuads = 0;
        float area = 0.0F;
        int quadBytes = LodMesher.VERTEX_BYTES * LodMesher.VERTICES_PER_QUAD;
        for (int offset = 0; offset < vertices.limit(); offset += quadBytes) {
            float top = vertices.getFloat(offset + Float.BYTES);
            boolean horizontal = true;
            for (int corner = 1; corner < LodMesher.VERTICES_PER_QUAD; corner++) {
                horizontal &= vertices.getFloat(offset + corner * LodMesher.VERTEX_BYTES + Float.BYTES) == top;
            }
            if (horizontal) {
                topQuads++;
                float width = vertices.getFloat(offset + 2 * LodMesher.VERTEX_BYTES) - vertices.getFloat(offset);
                float depth = vertices.getFloat(offset + LodMesher.VERTEX_BYTES + 2 * Float.BYTES) - vertices.getFloat(offset + 2 * Float.BYTES);
                area += width * depth;
                assertTrue(top == 65.0F || top == 71.0F);
            }
        }
        assertEquals(4, topQuads);
        assertEquals((float) LodTile.SAMPLES, area);
    }

    @Test
    void knownEmptyTerrainIsCompleteWithoutGeometry() {
        LodTile tile = new LodTile(0, 0, 0);
        for (int z = 0; z < LodTile.SIZE; z++) {
            for (int x = 0; x < LodTile.SIZE; x++) {
                tile.set(x, z, LodSample.of(Short.MIN_VALUE, 0, 0, LodSample.FLAG_VOID));
            }
        }
        LodMesh mesh = LodMesher.mesh(LodStore.key(0, 0, 0), tile);
        assertEquals(0, mesh.quads());
        assertEquals(0, mesh.vertices().remaining());
        assertTrue(mesh.complete());
    }

    @Test
    void skirtsReachLowerNeighboursAcrossDetailLevelsAndNegativeCoordinates() {
        LodStore store = new LodStore();
        long[] high = new long[LodTile.SAMPLES];
        long[] low = new long[LodTile.SAMPLES];
        Arrays.fill(high, LodSample.of(150, GREEN, 0, 0));
        Arrays.fill(low, LodSample.of(-20, GREEN, 0, 0));
        store.fillTile(0, 0, 0, high);
        store.fillTile(1, -1, 0, low);
        LodTile tile = store.tile(0, 0, 0);
        LodBorders borders = store.borders(tile);
        assertEquals(-19, borders.top(LodBorders.WEST, 0));
        assertEquals(LodBorders.UNKNOWN, borders.top(LodBorders.EAST, 0));
        LodMesh mesh = LodMesher.mesh(LodStore.key(0, 0, 0), tile, borders);
        float lowest = Float.POSITIVE_INFINITY;
        for (int offset = 0; offset < mesh.vertices().limit(); offset += LodMesher.VERTEX_BYTES) {
            lowest = Math.min(lowest, mesh.vertices().getFloat(offset + Float.BYTES));
        }
        assertEquals(-20.0F, lowest);
        store.putChunk(-1, 0, filledColumns(-40));
        assertFalse(borders.matches(store.borders(tile)));
    }

    private static long[] filledColumns(int height) {
        long[] columns = new long[LodStore.CHUNK_SIZE * LodStore.CHUNK_SIZE];
        Arrays.fill(columns, LodSample.of(height, GREEN, 0, 0));
        return columns;
    }

    @Test
    void selectionKeepsUploadedDetailAfterSampleEviction() {
        LodSelection selection = new LodSelection();
        boolean[] resident = {true};
        LodSelection.TileIndex tiles = new LodSelection.TileIndex() {
            @Override
            public boolean exists(int level, int x, int z) {
                return resident[0] && level >= LodQuality.LOW.finestLevel() && x == 0 && z == 0;
            }

            @Override
            public boolean meshed(long key) {
                return LodStore.levelOf(key) >= LodQuality.LOW.finestLevel()
                    && LodStore.tileXOf(key) == 0 && LodStore.tileZOf(key) == 0;
            }

            @Override
            public boolean gapsAllowed() {
                return false;
            }
        };
        long detail = LodStore.key(LodQuality.LOW.finestLevel(), 0, 0);
        selection.select(tiles, 8.0, 8.0, 32.0, LodSelection.ClipBox.NONE, LodQuality.LOW);
        assertEquals(1, selection.drawn().size());
        assertEquals(detail, selection.drawn().getLong(0));
        resident[0] = false;
        selection.select(tiles, 8.0, 8.0, 32.0, LodSelection.ClipBox.NONE, LodQuality.LOW);
        assertEquals(1, selection.drawn().size());
        assertEquals(detail, selection.drawn().getLong(0));
        assertTrue(selection.wanted().isEmpty());
    }

    @Test
    void selectionKeepsTheParentUntilUploadedChildrenCoverTheirFootprints() {
        LodSelection selection = new LodSelection();
        boolean[] covered = {false};
        LodSelection.TileIndex tiles = new LodSelection.TileIndex() {
            @Override
            public boolean exists(int level, int x, int z) {
                return level >= LodStore.LEVELS - 2;
            }

            @Override
            public boolean meshed(long key) {
                return exists(LodStore.levelOf(key), LodStore.tileXOf(key), LodStore.tileZOf(key));
            }

            @Override
            public boolean gapsAllowed() {
                return false;
            }

            @Override
            public boolean covered(long key) {
                return covered[0];
            }
        };
        int rootSize = LodSelection.tileBlocks(LodStore.LEVELS - 1);
        double center = rootSize / 2.0;
        long parent = LodStore.key(LodStore.LEVELS - 1, 0, 0);
        selection.select(tiles, center, center, 100.0, LodSelection.ClipBox.NONE, LodQuality.ULTRA);
        assertTrue(selection.drawn().contains(parent));
        assertEquals(4, selection.wanted().size());
        covered[0] = true;
        selection.select(tiles, center, center, 100.0, LodSelection.ClipBox.NONE, LodQuality.ULTRA);
        assertFalse(selection.drawn().contains(parent));
        assertEquals(4, selection.drawn().size());
    }

    @Test
    void selectionSplitsNearTilesAndSkipsTheRealTerrain() {
        LodStore store = new LodStore();
        long[] columns = new long[LodStore.CHUNK_SIZE * LodStore.CHUNK_SIZE];
        Arrays.fill(columns, LodSample.of(64, GREEN, 0, 0));
        for (int chunk = 0; chunk < 64; chunk++) {
            store.putChunk(chunk, 0, columns);
        }
        store.downsamplePending(Integer.MAX_VALUE);
        LodSelection selection = new LodSelection();
        LodSelection.TileIndex everything = new LodSelection.TileIndex() {
            @Override
            public boolean exists(int level, int tileX, int tileZ) {
                return store.tile(level, tileX, tileZ) != null;
            }

            @Override
            public boolean meshed(long key) {
                return exists(LodStore.levelOf(key), LodStore.tileXOf(key), LodStore.tileZOf(key));
            }

            @Override
            public boolean gapsAllowed() {
                return true;
            }
        };
        selection.select(everything, 0.0, 8.0, 4096.0, new LodSelection.ClipBox(-64.0, -64.0, 64.0, 64.0), LodQuality.ULTRA);
        assertFalse(selection.drawn().isEmpty());
        assertTrue(selection.wanted().isEmpty());
        assertFalse(selection.drawn().contains(LodStore.key(0, 0, 0)));
        assertTrue(selection.drawn().contains(LodStore.key(0, 2, 0)));
    }
}
