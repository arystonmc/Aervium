package com.aryston.kinetic.core.lod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class LodLayersTest {
    private static final int GREEN = 0x408020;
    private static final int STONE = 0x808080;
    private static final int BLUE = 0x2040C0;
    private static final int QUAD_BYTES = LodMesher.VERTEX_BYTES * LodMesher.VERTICES_PER_QUAD;

    @Test
    void separatedCanopyAndGroundKeepTheirGapAndUnderside() {
        LodStore store = new LodStore();
        store.fillTile(0, 0, 0, layered(false));
        LodMesh mesh = LodMesher.mesh(0L, store.tile(0, 0, 0));
        assertEquals(12, mesh.quads());
        assertEquals(mesh.quads(), mesh.opaqueQuads());
        assertEquals(-64.0F, mesh.minY());
        assertEquals(91.0F, mesh.maxY());
        assertTrue(horizontal(mesh.vertices(), 88.0F));
        assertTrue(horizontal(mesh.vertices(), 65.0F));
        for (int offset = 0; offset < mesh.vertices().limit(); offset += QUAD_BYTES) {
            float low = Float.POSITIVE_INFINITY;
            float high = Float.NEGATIVE_INFINITY;
            for (int corner = 0; corner < LodMesher.VERTICES_PER_QUAD; corner++) {
                float y = mesh.vertices().getFloat(offset + corner * LodMesher.VERTEX_BYTES + Float.BYTES);
                low = Math.min(low, y);
                high = Math.max(high, y);
            }
            assertFalse(low < 88.0F && high > 65.0F && low != high);
        }
    }

    @Test
    void opaqueGroundPrecedesWaterAndWaterDoesNotHideTheBottom() {
        LodStore store = new LodStore();
        store.fillTile(0, 0, 0, layered(true));
        LodMesh mesh = LodMesher.mesh(0L, store.tile(0, 0, 0));
        assertEquals(6, mesh.opaqueQuads());
        assertEquals(11, mesh.quads());
        assertTrue(horizontal(mesh.vertices(), 60.0F));
        for (int offset = 0; offset < mesh.vertices().limit(); offset += LodMesher.VERTEX_BYTES) {
            boolean water = (mesh.vertices().get(offset + LodMesher.VERTEX_BYTES - 1) & 1) != 0;
            assertEquals(offset >= mesh.opaqueQuads() * QUAD_BYTES, water);
        }
    }

    @Test
    void layersRejectOverlapsAndUnboundedColumns() {
        long top = LodSample.of(80, GREEN, 0, 0);
        assertThrows(IllegalArgumentException.class, () -> new LodLayers(new int[] {0, 2}, new long[] {top, top}, new short[] {70, 60}));
        LodLayers.Builder builder = new LodLayers.Builder(1);
        for (int index = 0; index < LodLayers.MAX_LAYERS; index++) {
            builder.add(LodSample.of(80 - index * 2, GREEN, 0, 0), 80 - index * 2);
        }
        assertThrows(IllegalArgumentException.class, () -> builder.add(LodSample.of(0, GREEN, 0, 0), 0));
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void downsamplingKeepsNearLayersAndReleasesFarLayers() {
        LodStore store = new LodStore();
        store.fillTile(0, -1, -1, layered(false));
        store.downsamplePending(Integer.MAX_VALUE);
        assertTrue(store.tile(1, -1, -1).layers() != null);
        assertTrue(store.tile(2, -1, -1).layers() != null);
        assertTrue(store.tile(3, -1, -1).layers() == null);
    }

    @Test
    void lightsDoNotChangeColourHeightOrSourcePriority() {
        long original = LodSample.of(-20, GREEN, 8, LodSample.FLAG_WATER | LodSample.FLAG_SAVED);
        int previousBlock = -1;
        for (int light = 0; light <= LodSample.MAX_LIGHT; light++) {
            long sample = LodSample.withLight(original, light, light);
            assertEquals(light, LodSample.skyLight(sample));
            assertEquals(light, LodSample.blockLight(sample));
            assertEquals(GREEN, LodSample.rgb(sample));
            assertEquals(-20, LodSample.height(sample));
            assertEquals(LodSample.priority(original), LodSample.priority(sample));
            int vertex = Byte.toUnsignedInt(LodSample.vertexLight(sample));
            assertEquals(light, vertex >> 4);
            assertEquals(1, vertex & 1);
            int block = (vertex >> 1) & 7;
            assertTrue(block >= previousBlock);
            previousBlock = block;
        }
        assertEquals(7, previousBlock);
        assertEquals(0x204010, LodColor.tint(0x408020, 0x808080));
    }

    static LodData layered(boolean water) {
        int top = water ? 63 : 90;
        int ground = water ? 59 : 64;
        int bottom = water ? 60 : 88;
        long surface = LodSample.of(top, water ? BLUE : GREEN, water ? top - bottom + 1 : 0, water ? LodSample.FLAG_WATER : 0);
        long[] samples = new long[LodTile.SAMPLES];
        Arrays.fill(samples, surface);
        LodLayers.Builder builder = new LodLayers.Builder(LodTile.SAMPLES);
        for (int column = 0; column < LodTile.SAMPLES; column++) {
            builder.add(surface, bottom);
            builder.add(LodSample.of(ground, GREEN, 0, 0), -64, STONE);
            builder.finishColumn();
        }
        return new LodData(samples, builder.build());
    }

    @Test
    void pathologicalLayerDetailFallsBackWithinTheMeshByteLimit() {
        long[] samples = new long[LodTile.SAMPLES];
        LodLayers.Builder layers = new LodLayers.Builder(LodTile.SAMPLES);
        for (int column = 0; column < LodTile.SAMPLES; column++) {
            int color = column + 1;
            samples[column] = LodSample.of(100, color, 0, 0);
            for (int layer = 0; layer < LodLayers.MAX_LAYERS; layer++) {
                int top = 100 - layer * 4;
                layers.add(LodSample.of(top, color, 0, 0), top - 1);
            }
            layers.finishColumn();
        }
        LodStore store = new LodStore();
        store.fillTile(0, 0, 0, new LodData(samples, layers.build()));
        LodMesh mesh = LodMesher.mesh(0L, store.tile(0, 0, 0));
        int limit = 4 * 1024 * 1024;
        assertTrue(mesh.vertices().remaining() <= limit);
        assertTrue(mesh.complete());
    }

    private static boolean horizontal(ByteBuffer vertices, float y) {
        for (int offset = 0; offset < vertices.limit(); offset += QUAD_BYTES) {
            boolean matches = true;
            for (int corner = 0; corner < LodMesher.VERTICES_PER_QUAD; corner++) {
                matches &= vertices.getFloat(offset + corner * LodMesher.VERTEX_BYTES + Float.BYTES) == y;
            }
            if (matches) {
                return true;
            }
        }
        return false;
    }
}
