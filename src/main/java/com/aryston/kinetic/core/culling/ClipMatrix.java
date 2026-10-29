package com.aryston.kinetic.core.culling;

import java.util.Arrays;

public final class ClipMatrix {
    private final float[] m = new float[16];
    private final float[] product = new float[16];

    public ClipMatrix() {
        identity();
    }

    public ClipMatrix identity() {
        Arrays.fill(m, 0.0F);
        m[0] = 1.0F;
        m[5] = 1.0F;
        m[10] = 1.0F;
        m[15] = 1.0F;
        return this;
    }

    public ClipMatrix set(float[] columnMajor) {
        System.arraycopy(columnMajor, 0, m, 0, 16);
        return this;
    }

    public ClipMatrix copy(ClipMatrix other) {
        System.arraycopy(other.m, 0, m, 0, 16);
        return this;
    }

    public ClipMatrix perspective(float fovYRadians, float aspect, float near, float far) {
        Arrays.fill(m, 0.0F);
        float focal = (float) (1.0 / Math.tan(fovYRadians * 0.5));
        m[0] = focal / aspect;
        m[5] = focal;
        m[10] = far / (near - far);
        m[11] = -1.0F;
        m[14] = near * far / (near - far);
        return this;
    }

    public ClipMatrix orthographic(float left, float right, float bottom, float top, float near, float far) {
        Arrays.fill(m, 0.0F);
        m[0] = 2.0F / (right - left);
        m[5] = 2.0F / (top - bottom);
        m[10] = 1.0F / (near - far);
        m[12] = -(right + left) / (right - left);
        m[13] = -(top + bottom) / (top - bottom);
        m[14] = near / (near - far);
        m[15] = 1.0F;
        return this;
    }

    public ClipMatrix multiply(ClipMatrix right) {
        float[] a = m;
        float[] b = right.m;
        float[] result = product;
        for (int column = 0; column < 4; column++) {
            for (int row = 0; row < 4; row++) {
                result[column * 4 + row] = a[row] * b[column * 4]
                    + a[4 + row] * b[column * 4 + 1]
                    + a[8 + row] * b[column * 4 + 2]
                    + a[12 + row] * b[column * 4 + 3];
            }
        }
        System.arraycopy(result, 0, m, 0, 16);
        return this;
    }

    public ClipMatrix depthFromW(float near, float far) {
        float scale = far / (far - near);
        m[2] = scale * m[3];
        m[6] = scale * m[7];
        m[10] = scale * m[11];
        m[14] = scale * (m[15] - near);
        return this;
    }

    public float get(int column, int row) {
        return m[column * 4 + row];
    }

    public float x(float x, float y, float z) {
        return m[0] * x + m[4] * y + m[8] * z + m[12];
    }

    public float y(float x, float y, float z) {
        return m[1] * x + m[5] * y + m[9] * z + m[13];
    }

    public float z(float x, float y, float z) {
        return m[2] * x + m[6] * y + m[10] * z + m[14];
    }

    public float w(float x, float y, float z) {
        return m[3] * x + m[7] * y + m[11] * z + m[15];
    }
}
