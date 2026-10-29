package com.aryston.kinetic.core.mesh;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.junit.jupiter.api.Test;

class CompactVertexEncoderTest {
    private static final int COLOR = 0xFF336699;
    private static final float ATLAS = 1024.0F;

    @Test
    void decodesBackToTheBlockVertexExactlyOnTheModelGrid() {
        ByteBuffer buffer = quad(
            new float[] {0.0F, 16.0F, -1.0F / 16.0F, 17.5F},
            new float[] {0.0F, 3.0F / 16.0F, 18.0F, -1.0F},
            new float[] {16.0F, 0.0F, 7.25F, 0.0625F},
            new float[] {32.0F / ATLAS, 48.0F / ATLAS, 48.0F / ATLAS, 32.0F / ATLAS},
            new float[] {16.0F / ATLAS, 16.0F / ATLAS, 32.0F / ATLAS, 32.0F / ATLAS}
        );
        ByteBuffer original = copy(buffer);

        assertEquals(0, new CompactVertexEncoder().encode(buffer, 4));

        for (int vertex = 0; vertex < 4; vertex++) {
            int source = vertex * CompactVertexEncoder.SOURCE_STRIDE;
            int compact = vertex * CompactVertexEncoder.COMPACT_STRIDE;
            assertEquals(original.getFloat(source), position(buffer, compact));
            assertEquals(original.getFloat(source + 4), position(buffer, compact + 2));
            assertEquals(original.getFloat(source + 8), position(buffer, compact + 4));
            assertEquals(original.getInt(source + 12), buffer.getInt(compact + 8));
            assertEquals(original.getFloat(source + 16), uv(buffer, compact + 12));
            assertEquals(original.getFloat(source + 20), uv(buffer, compact + 14));
        }
    }

    @Test
    void packsBlockLightLowAndSkyLightHigh() {
        ByteBuffer buffer = quad(new float[4], new float[4], new float[4], new float[4], new float[4]);
        buffer.putShort(24, (short) 240).putShort(26, (short) 80);
        buffer.putShort(CompactVertexEncoder.SOURCE_STRIDE + 24, (short) 0).putShort(CompactVertexEncoder.SOURCE_STRIDE + 26, (short) 240);

        new CompactVertexEncoder().encode(buffer, 4);

        assertEquals(240 | 80 << 8, Short.toUnsignedInt(buffer.getShort(6)));
        assertEquals(240 << 8, Short.toUnsignedInt(buffer.getShort(CompactVertexEncoder.COMPACT_STRIDE + 6)));
    }

    @Test
    void clampsPositionsOutsideTheRangeAndCountsThem() {
        ByteBuffer buffer = quad(
            new float[] {-9.0F, 0.0F, 24.0F, 1.0F},
            new float[4],
            new float[4],
            new float[4],
            new float[4]
        );

        assertEquals(2, new CompactVertexEncoder().encode(buffer, 4));

        assertEquals(0, Short.toUnsignedInt(buffer.getShort(0)));
        assertEquals(0xFFFF, Short.toUnsignedInt(buffer.getShort(2 * CompactVertexEncoder.COMPACT_STRIDE)));
    }

    @Test
    void roundsTextureEdgesThatAreNotOnTheGridTowardTheInsideOfTheSprite() {
        float low = 1.0F / 3.0F;
        float high = 2.0F / 3.0F;
        ByteBuffer buffer = quad(new float[4], new float[4], new float[4], new float[] {low, high, high, low}, new float[] {low, low, high, high});

        new CompactVertexEncoder().encode(buffer, 4);

        assertEquals((int) Math.ceil(low * 65536.0), Short.toUnsignedInt(buffer.getShort(12)));
        assertEquals((int) Math.floor(high * 65536.0), Short.toUnsignedInt(buffer.getShort(CompactVertexEncoder.COMPACT_STRIDE + 12)));
        assertEquals((int) Math.ceil(low * 65536.0), Short.toUnsignedInt(buffer.getShort(14)));
        assertEquals((int) Math.floor(high * 65536.0), Short.toUnsignedInt(buffer.getShort(2 * CompactVertexEncoder.COMPACT_STRIDE + 14)));
    }

    @Test
    void keepsTheFarAtlasEdgeInsideTheShortRange() {
        ByteBuffer buffer = quad(new float[4], new float[4], new float[4], new float[] {0.5F, 1.0F, 1.0F, 0.5F}, new float[4]);

        new CompactVertexEncoder().encode(buffer, 4);

        assertEquals(0xFFFF, Short.toUnsignedInt(buffer.getShort(CompactVertexEncoder.COMPACT_STRIDE + 12)));
    }

    private static float position(ByteBuffer buffer, int at) {
        return Short.toUnsignedInt(buffer.getShort(at)) / CompactVertexEncoder.POSITION_STEPS_PER_BLOCK - CompactVertexEncoder.POSITION_ORIGIN;
    }

    private static float uv(ByteBuffer buffer, int at) {
        return Short.toUnsignedInt(buffer.getShort(at)) / CompactVertexEncoder.UV_STEPS;
    }

    private static ByteBuffer quad(float[] x, float[] y, float[] z, float[] u, float[] v) {
        ByteBuffer buffer = ByteBuffer.allocate(4 * CompactVertexEncoder.SOURCE_STRIDE).order(ByteOrder.nativeOrder());
        for (int vertex = 0; vertex < 4; vertex++) {
            int at = vertex * CompactVertexEncoder.SOURCE_STRIDE;
            buffer.putFloat(at, x[vertex]).putFloat(at + 4, y[vertex]).putFloat(at + 8, z[vertex]).putInt(at + 12, COLOR)
                .putFloat(at + 16, u[vertex]).putFloat(at + 20, v[vertex]).putShort(at + 24, (short) 15).putShort(at + 26, (short) 240);
        }
        return buffer;
    }

    private static ByteBuffer copy(ByteBuffer buffer) {
        ByteBuffer copy = ByteBuffer.allocate(buffer.capacity()).order(ByteOrder.nativeOrder());
        copy.put(0, buffer, 0, buffer.capacity());
        return copy;
    }
}
