package com.aryston.kinetic.core.mesh;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SharedCornersTest {
    @Test
    void verticesAtTheSamePositionShareACorner() {
        float[] positions = {
            0.0F, 0.0F, 0.0F,
            1.0F, 0.0F, 0.0F,
            0.0F, 0.0F, 0.0F,
            1.0F, 0.0F, 0.0F
        };
        SharedCorners corners = SharedCorners.of(positions);
        assertEquals(2, corners.cornerCount());
        assertEquals(0, corners.cornerOf(2));
        assertEquals(1, corners.cornerOf(3));
        assertEquals(1.0F, corners.cornerX(1));
    }

    @Test
    void positiveAndNegativeZeroStayApart() {
        SharedCorners corners = SharedCorners.of(new float[] {0.0F, 0.0F, 0.0F, -0.0F, 0.0F, 0.0F});
        assertEquals(2, corners.cornerCount());
    }
}
