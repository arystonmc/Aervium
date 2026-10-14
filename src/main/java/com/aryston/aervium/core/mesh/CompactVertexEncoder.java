package com.aryston.aervium.core.mesh;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class CompactVertexEncoder {
    public static final int SOURCE_STRIDE = 28;
    public static final int COMPACT_STRIDE = 16;
    public static final float POSITION_ORIGIN = 8.0F;
    public static final float POSITION_STEPS_PER_BLOCK = 2048.0F;
    public static final float UV_STEPS = 65536.0F;

    private static final int VERTICES_PER_QUAD = 4;
    private static final int MAX_UNSIGNED_SHORT = 0xFFFF;
    private static final int MAX_LIGHT = 0xFF;
    private static final int SKY_LIGHT_SHIFT = 8;
    private static final float UV_SNAP = 1.0F / 64.0F;
    private static final int SOURCE_COLOR = 12;
    private static final int SOURCE_U = 16;
    private static final int SOURCE_V = 20;
    private static final int SOURCE_BLOCK_LIGHT = 24;
    private static final int SOURCE_SKY_LIGHT = 26;
    private static final int COMPACT_Y = 2;
    private static final int COMPACT_Z = 4;
    private static final int COMPACT_LIGHT = 6;
    private static final int COMPACT_COLOR = 8;
    private static final int COMPACT_U = 12;
    private static final int COMPACT_V = 14;

    private static final ThreadLocal<CompactVertexEncoder> PER_THREAD = ThreadLocal.withInitial(CompactVertexEncoder::new);

    private final float[] x = new float[VERTICES_PER_QUAD];
    private final float[] y = new float[VERTICES_PER_QUAD];
    private final float[] z = new float[VERTICES_PER_QUAD];
    private final int[] color = new int[VERTICES_PER_QUAD];
    private final float[] u = new float[VERTICES_PER_QUAD];
    private final float[] v = new float[VERTICES_PER_QUAD];
    private final int[] light = new int[VERTICES_PER_QUAD];
    private int clamped;

    public static int compactBytes(int vertexCount) {
        return vertexCount * COMPACT_STRIDE;
    }

    public static int encodeInPlace(ByteBuffer vertices, int vertexCount) {
        return PER_THREAD.get().encode(vertices, vertexCount);
    }

    int encode(ByteBuffer vertices, int vertexCount) {
        ByteBuffer buffer = vertices.order() == ByteOrder.nativeOrder() ? vertices : vertices.duplicate().order(ByteOrder.nativeOrder());
        int base = buffer.position();
        clamped = 0;
        for (int first = 0; first < vertexCount; first += VERTICES_PER_QUAD) {
            int count = Math.min(VERTICES_PER_QUAD, vertexCount - first);
            for (int vertex = 0; vertex < count; vertex++) {
                read(buffer, base + (first + vertex) * SOURCE_STRIDE, vertex);
            }
            for (int vertex = 0; vertex < count; vertex++) {
                write(buffer, base + (first + vertex) * COMPACT_STRIDE, vertex, count);
            }
        }
        return clamped;
    }

    private void read(ByteBuffer buffer, int at, int vertex) {
        x[vertex] = buffer.getFloat(at);
        y[vertex] = buffer.getFloat(at + Float.BYTES);
        z[vertex] = buffer.getFloat(at + 2 * Float.BYTES);
        color[vertex] = buffer.getInt(at + SOURCE_COLOR);
        u[vertex] = buffer.getFloat(at + SOURCE_U);
        v[vertex] = buffer.getFloat(at + SOURCE_V);
        int blockLight = Math.clamp(buffer.getShort(at + SOURCE_BLOCK_LIGHT), 0, MAX_LIGHT);
        int skyLight = Math.clamp(buffer.getShort(at + SOURCE_SKY_LIGHT), 0, MAX_LIGHT);
        light[vertex] = blockLight | skyLight << SKY_LIGHT_SHIFT;
    }

    private void write(ByteBuffer buffer, int at, int vertex, int count) {
        buffer.putShort(at, position(x[vertex]));
        buffer.putShort(at + COMPACT_Y, position(y[vertex]));
        buffer.putShort(at + COMPACT_Z, position(z[vertex]));
        buffer.putShort(at + COMPACT_LIGHT, (short) light[vertex]);
        buffer.putInt(at + COMPACT_COLOR, color[vertex]);
        buffer.putShort(at + COMPACT_U, textureCoordinate(u, vertex, count));
        buffer.putShort(at + COMPACT_V, textureCoordinate(v, vertex, count));
    }

    private short position(float coordinate) {
        float steps = (coordinate + POSITION_ORIGIN) * POSITION_STEPS_PER_BLOCK;
        int rounded = Math.round(steps);
        if (!(steps >= 0.0F) || rounded > MAX_UNSIGNED_SHORT) {
            clamped++;
            return (short) (steps > 0.0F ? MAX_UNSIGNED_SHORT : 0);
        }
        return (short) rounded;
    }

    private static short textureCoordinate(float[] quad, int vertex, int count) {
        float value = quad[vertex];
        float steps = value * UV_STEPS;
        float rounded = Math.round(steps);
        int encoded;
        if (Math.abs(steps - rounded) <= UV_SNAP) {
            encoded = (int) rounded;
        } else if (value <= lowest(quad, count)) {
            encoded = (int) Math.ceil(steps);
        } else if (value >= highest(quad, count)) {
            encoded = (int) Math.floor(steps);
        } else {
            encoded = (int) rounded;
        }
        return (short) Math.clamp(encoded, 0, MAX_UNSIGNED_SHORT);
    }

    private static float lowest(float[] quad, int count) {
        float lowest = quad[0];
        for (int vertex = 1; vertex < count; vertex++) {
            lowest = Math.min(lowest, quad[vertex]);
        }
        return lowest;
    }

    private static float highest(float[] quad, int count) {
        float highest = quad[0];
        for (int vertex = 1; vertex < count; vertex++) {
            highest = Math.max(highest, quad[vertex]);
        }
        return highest;
    }
}
