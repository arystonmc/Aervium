package com.aryston.kinetic.core.mesh;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public final class FacingSort {
    private static final int VERTICES_PER_QUAD = 4;
    private static final float PLANE_TOLERANCE = 1.0E-5F;
    private static final float AXIS_TOLERANCE = 1.0E-4F;
    private static final ThreadLocal<FacingSort> PER_THREAD = ThreadLocal.withInitial(FacingSort::new);

    private byte[] scratch = new byte[0];
    private byte[] buckets = new byte[0];
    private final int[] counts = new int[FaceRanges.BUCKETS + 1];
    private final float[] planes = new float[FaceRanges.BUCKETS];
    private final float[] corner = new float[VERTICES_PER_QUAD * 3];

    public static FaceRanges sortQuads(ByteBuffer vertices, int vertexCount, int stride) {
        ByteBuffer nativeVertices = vertices.order() == ByteOrder.nativeOrder() ? vertices : vertices.duplicate().order(ByteOrder.nativeOrder());
        return PER_THREAD.get().sort(nativeVertices, vertexCount, stride);
    }

    FaceRanges sort(ByteBuffer vertices, int vertexCount, int stride) {
        int quads = vertexCount / VERTICES_PER_QUAD;
        int quadBytes = stride * VERTICES_PER_QUAD;
        int base = vertices.position();
        if (buckets.length < quads) {
            buckets = new byte[quads];
        }
        Arrays.fill(counts, 0);
        Arrays.fill(planes, Float.NaN);
        for (int quad = 0; quad < quads; quad++) {
            int bucket = classify(vertices, base + quad * quadBytes, stride);
            buckets[quad] = (byte) bucket;
            counts[bucket + 1]++;
        }
        for (int bucket = 0; bucket < FaceRanges.BUCKETS; bucket++) {
            counts[bucket + 1] += counts[bucket];
        }
        int[] firstQuad = Arrays.copyOf(counts, counts.length);
        if (quads > 0 && counts[1] != quads) {
            int bytes = quads * quadBytes;
            if (scratch.length < bytes) {
                scratch = new byte[bytes];
            }
            int[] next = Arrays.copyOf(counts, FaceRanges.BUCKETS);
            for (int quad = 0; quad < quads; quad++) {
                int target = next[buckets[quad]]++;
                vertices.get(base + quad * quadBytes, scratch, target * quadBytes, quadBytes);
            }
            vertices.put(base, scratch, 0, bytes);
        }
        return new FaceRanges(firstQuad, closedPlanes());
    }

    private float[] closedPlanes() {
        float[] result = new float[FaceRanges.BUCKETS];
        for (int bucket = 1; bucket < FaceRanges.BUCKETS; bucket++) {
            float plane = planes[bucket];
            boolean positive = bucket % 2 == 1;
            if (Float.isNaN(plane)) {
                result[bucket] = positive ? Float.POSITIVE_INFINITY : Float.NEGATIVE_INFINITY;
            } else {
                result[bucket] = plane;
            }
        }
        return result;
    }

    private int classify(ByteBuffer vertices, int offset, int stride) {
        for (int vertex = 0; vertex < VERTICES_PER_QUAD; vertex++) {
            int at = offset + vertex * stride;
            corner[vertex * 3] = vertices.getFloat(at);
            corner[vertex * 3 + 1] = vertices.getFloat(at + Float.BYTES);
            corner[vertex * 3 + 2] = vertices.getFloat(at + 2 * Float.BYTES);
        }
        float ax = corner[6] - corner[0];
        float ay = corner[7] - corner[1];
        float az = corner[8] - corner[2];
        float bx = corner[9] - corner[3];
        float by = corner[10] - corner[4];
        float bz = corner[11] - corner[5];
        float nx = ay * bz - az * by;
        float ny = az * bx - ax * bz;
        float nz = ax * by - ay * bx;
        float length = Math.abs(nx) + Math.abs(ny) + Math.abs(nz);
        if (!(length > 0.0F)) {
            return FaceRanges.UNASSIGNED;
        }
        int axis;
        float sign;
        if (Math.abs(ny) <= AXIS_TOLERANCE * length && Math.abs(nz) <= AXIS_TOLERANCE * length) {
            axis = 0;
            sign = nx;
        } else if (Math.abs(nx) <= AXIS_TOLERANCE * length && Math.abs(nz) <= AXIS_TOLERANCE * length) {
            axis = 1;
            sign = ny;
        } else if (Math.abs(nx) <= AXIS_TOLERANCE * length && Math.abs(ny) <= AXIS_TOLERANCE * length) {
            axis = 2;
            sign = nz;
        } else {
            return FaceRanges.UNASSIGNED;
        }
        float low = corner[axis];
        float high = low;
        for (int vertex = 1; vertex < VERTICES_PER_QUAD; vertex++) {
            low = Math.min(low, corner[vertex * 3 + axis]);
            high = Math.max(high, corner[vertex * 3 + axis]);
        }
        if (high - low > PLANE_TOLERANCE) {
            return FaceRanges.UNASSIGNED;
        }
        float plane = sign > 0.0F ? low : high;
        int bucket = FaceRanges.EAST + axis * 2 + (sign > 0.0F ? 0 : 1);
        float known = planes[bucket];
        if (Float.isNaN(known)) {
            planes[bucket] = plane;
        } else {
            planes[bucket] = sign > 0.0F ? Math.min(known, plane) : Math.max(known, plane);
        }
        return bucket;
    }
}
