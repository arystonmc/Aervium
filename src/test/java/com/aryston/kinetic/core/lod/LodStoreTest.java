package com.aryston.kinetic.core.lod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class LodStoreTest {
    private static final int GREEN = 0x00FF00;
    private static final int BLUE = 0x0000FF;

    @Test
    void samplesKeepTheirFields() {
        long sample = LodSample.of(-60, 0x123456, 7, LodSample.FLAG_WATER);
        assertTrue(LodSample.present(sample));
        assertTrue(LodSample.water(sample));
        assertEquals(-60, LodSample.height(sample));
        assertEquals(0x123456, LodSample.rgb(sample));
        assertEquals(7, LodSample.waterDepth(sample));
        assertFalse(LodSample.present(LodSample.EMPTY));
    }

    @Test
    void aChunkLandsInItsLevelZeroTile() {
        LodStore store = new LodStore();
        store.putChunk(-1, 4, filled(LodSample.of(70, GREEN, 0, 0)));
        LodTile tile = store.tile(0, -1, 1);
        assertEquals(LodStore.CHUNK_SIZE * LodStore.CHUNK_SIZE, tile.present());
        assertEquals(70, LodSample.height(tile.sample(48, 0)));
        assertFalse(LodSample.present(tile.sample(0, 0)));
    }

    @Test
    void downsamplingFillsEveryCoarserLevel() {
        LodStore store = new LodStore();
        store.putChunk(0, 0, filled(LodSample.of(64, GREEN, 0, 0)));
        store.downsamplePending(Integer.MAX_VALUE);
        for (int level = 1; level < LodStore.LEVELS; level++) {
            assertTrue(LodSample.present(store.tile(level, 0, 0).sample(0, 0)), "level " + level);
        }
        assertEquals(0, store.pendingCount());
        assertEquals(LodStore.LEVELS, store.tileCount());
    }

    @Test
    void combiningTakesTheHighestSurfaceAndTheAverageColour() {
        long[] samples = new long[LodTile.SIZE * 2];
        samples[0] = LodSample.of(60, GREEN, 0, 0);
        samples[1] = LodSample.of(80, BLUE, 0, 0);
        long combined = LodCombine.of(samples, 0, LodTile.SIZE);
        assertEquals(80, LodSample.height(combined));
        assertEquals(0x007F7F, LodSample.rgb(combined));
    }

    @Test
    void waterNeedsMostOfTheCells() {
        long[] samples = new long[LodTile.SIZE * 2];
        Arrays.fill(samples, LodSample.of(62, BLUE, 4, LodSample.FLAG_WATER));
        samples[0] = LodSample.of(63, GREEN, 0, 0);
        assertTrue(LodSample.water(LodCombine.of(samples, 0, LodTile.SIZE)));
        samples[1] = LodSample.of(63, GREEN, 0, 0);
        samples[LodTile.SIZE] = LodSample.of(63, GREEN, 0, 0);
        assertFalse(LodSample.water(LodCombine.of(samples, 0, LodTile.SIZE)));
    }

    @Test
    void sparseChildrenCannotReplaceACompleteCoarsePreview() {
        LodStore store = new LodStore();
        long preview = LodSample.of(90, BLUE, 0, LodSample.FLAG_APPROXIMATE);
        long[] samples = new long[LodTile.SAMPLES];
        Arrays.fill(samples, preview);
        store.fillTile(LodStore.LEVELS - 1, 0, 0, samples);
        store.putChunk(0, 0, filled(LodSample.of(64, GREEN, 0, 0)));
        store.downsamplePending(Integer.MAX_VALUE);
        assertEquals(preview, store.tile(LodStore.LEVELS - 1, 0, 0).sample(0, 0));
        assertTrue(store.complete(LodStore.key(LodStore.LEVELS - 1, 0, 0)));
        assertFalse(store.complete(LodStore.key(0, 0, 0)));
    }

    @Test
    void completePreviewsRepairSparseClientAncestorsWithoutReplacingClientColumns() {
        LodStore store = new LodStore();
        long client = LodSample.of(120, GREEN, 0, 0);
        long preview = LodSample.of(60, BLUE, 0, LodSample.FLAG_APPROXIMATE);
        store.putChunk(0, 0, filled(client));
        store.downsamplePending(Integer.MAX_VALUE);
        long[] samples = new long[LodTile.SAMPLES];
        Arrays.fill(samples, preview);
        for (int level = 0; level < LodStore.LEVELS; level++) {
            store.fillTile(level, 0, 0, samples);
            store.downsamplePending(Integer.MAX_VALUE);
            assertTrue(store.complete(LodStore.key(level, 0, 0)), "level " + level);
        }
        assertEquals(client, store.tile(0, 0, 0).sample(0, 0));
        for (int level = 0; level < LodStore.LEVELS; level++) {
            assertTrue(store.complete(LodStore.key(level, 0, 0)), "level " + level);
        }
    }

    @Test
    void savedSurfacesReplacePreviewsButPreviewsCannotReplaceSavedSurfaces() {
        LodStore store = new LodStore();
        long exact = LodSample.of(80, GREEN, 0, 0);
        long preview = LodSample.of(60, BLUE, 0, LodSample.FLAG_APPROXIMATE);
        long[] samples = new long[LodTile.SAMPLES];
        Arrays.fill(samples, preview);
        store.fillTile(1, 0, 0, samples);
        samples[0] = exact;
        store.fillTile(1, 0, 0, samples);
        samples[0] = preview;
        store.fillTile(1, 0, 0, samples);
        assertEquals(exact, store.tile(1, 0, 0).sample(0, 0));
    }

    @Test
    void voidCellsCountAsCoverageAndDoNotDiluteVisibleColours() {
        long[] samples = new long[LodTile.SIZE * 2];
        Arrays.fill(samples, LodSample.of(Short.MIN_VALUE, 0, 0, LodSample.FLAG_VOID));
        long empty = LodCombine.of(samples, 0, LodTile.SIZE);
        assertTrue(LodSample.complete(empty));
        assertFalse(LodSample.visible(empty));
        samples[0] = LodSample.of(90, GREEN, 0, 0);
        long combined = LodCombine.of(samples, 0, LodTile.SIZE);
        assertTrue(LodSample.complete(combined));
        assertTrue(LodSample.visible(combined));
        assertEquals(GREEN, LodSample.rgb(combined));
    }

    @Test
    void sampleResidencyIsBoundedAndKeepsNearbyCoarseCoverage() {
        LodStore store = new LodStore();
        long[] samples = new long[LodTile.SAMPLES];
        Arrays.fill(samples, LodSample.of(64, GREEN, 0, 0));
        store.fillTile(LodStore.LEVELS - 1, 0, 0, samples);
        for (int x = 1; x < 20; x++) {
            store.fillTile(0, x, 0, samples);
        }
        store.trim(0.0, 0.0, 4096.0, LodStore.LEVELS);
        assertEquals(LodStore.LEVELS, store.tileCount());
        assertTrue(store.tile(LodStore.LEVELS - 1, 0, 0) != null);
        store.trim(-100000.0, -100000.0, 64.0, LodStore.LEVELS);
        assertEquals(0, store.tileCount());
        assertEquals(0, store.pendingCount());
    }

    private static long[] filled(long sample) {
        long[] columns = new long[LodStore.CHUNK_SIZE * LodStore.CHUNK_SIZE];
        Arrays.fill(columns, sample);
        return columns;
    }

    @Test
    void sourcePrecedenceAllowsSavedRefreshWithoutOverwritingClientEdits() {
        LodStore store = new LodStore();
        long[] data = new long[LodTile.SAMPLES];
        Arrays.fill(data, LodSample.of(50, GREEN, 0, LodSample.FLAG_APPROXIMATE));
        store.fillTile(0, 0, 0, data);
        int previewVersion = store.tile(0, 0, 0).version();
        Arrays.fill(data, LodSample.of(55, BLUE, 0, LodSample.FLAG_APPROXIMATE));
        store.fillTile(0, 0, 0, data);
        assertEquals(50, LodSample.height(store.tile(0, 0, 0).sample(0, 0)));
        assertEquals(previewVersion, store.tile(0, 0, 0).version());
        Arrays.fill(data, LodSample.of(60, GREEN, 0, LodSample.FLAG_SEEDED));
        store.fillTile(0, 0, 0, data);
        int seededVersion = store.tile(0, 0, 0).version();
        Arrays.fill(data, LodSample.of(65, BLUE, 0, LodSample.FLAG_SEEDED));
        store.fillTile(0, 0, 0, data);
        assertEquals(60, LodSample.height(store.tile(0, 0, 0).sample(0, 0)));
        assertEquals(seededVersion, store.tile(0, 0, 0).version());
        Arrays.fill(data, LodSample.of(70, BLUE, 0, LodSample.FLAG_SAVED));
        store.fillTile(0, 0, 0, data);
        Arrays.fill(data, LodSample.of(80, BLUE, 0, LodSample.FLAG_SAVED));
        store.fillTile(0, 0, 0, data);
        assertEquals(80, LodSample.height(store.tile(0, 0, 0).sample(0, 0)));
        store.putChunk(0, 0, filled(LodSample.of(100, GREEN, 0, 0)));
        store.fillTile(0, 0, 0, data);
        assertEquals(100, LodSample.height(store.tile(0, 0, 0).sample(0, 0)));
        Arrays.fill(data, LodSample.of(110, GREEN, 0, LodSample.FLAG_SEEDED));
        store.fillTile(0, 0, 0, data);
        assertEquals(80, LodSample.height(store.tile(0, 0, 0).sample(32, 32)));
    }

    @Test
    void layerStorageCountsAgainstTheByteResidencyLimit() {
        LodStore store = new LodStore();
        LodData data = LodLayersTest.layered(false);
        store.fillTile(0, 0, 0, data);
        store.fillTile(0, 1, 0, data);
        long tileBytes = store.tile(0, 0, 0).bytes();
        assertTrue(tileBytes > LodTile.SAMPLES * Long.BYTES);
        store.trim(0.0, 0.0, 1024.0, 10, tileBytes);
        assertEquals(1, store.tileCount());
        assertEquals(tileBytes, store.sampleBytes());
    }
}
