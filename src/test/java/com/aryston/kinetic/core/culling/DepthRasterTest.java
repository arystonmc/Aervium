package com.aryston.kinetic.core.culling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.Test;

class DepthRasterTest {
    private static final float FOV = (float) Math.toRadians(70.0);

    private static DepthRaster camera() {
        DepthRaster raster = new DepthRaster(128, 64);
        raster.begin(new ClipMatrix().perspective(FOV, 2.0F, 0.5F, 512.0F), true);
        return raster;
    }

    @Test
    void emptyRasterHidesNothing() {
        DepthRaster raster = camera();
        assertTrue(raster.isVisible(-8, -8, -40, 8, 8, -24));
        assertEquals(0, raster.occluders());
    }

    @Test
    void wallHidesWhatIsBehindIt() {
        DepthRaster raster = camera();
        assertTrue(raster.addOccluder(-64, -64, -18, 64, 64, -16));
        assertFalse(raster.isVisible(-8, -8, -40, 8, 8, -24));
        assertFalse(raster.isVisible(30, -5, -100, 40, 5, -90));
    }

    @Test
    void wallNeverHidesWhatIsInFrontOfIt() {
        DepthRaster raster = camera();
        raster.addOccluder(-64, -64, -18, 64, 64, -16);
        assertTrue(raster.isVisible(-2, -2, -10, 2, 2, -6));
        assertTrue(raster.isVisible(-2, -2, -17, 2, 2, -15));
    }

    @Test
    void sectionNeverHidesItself() {
        DepthRaster raster = camera();
        raster.addOccluder(-8, -8, -40, 8, 8, -24);
        assertTrue(raster.isVisible(-8, -8, -40, 8, 8, -24));
    }

    @Test
    void partlyCoveredBoxStaysVisible() {
        DepthRaster raster = camera();
        raster.addOccluder(-64, -64, -18, 0, 64, -16);
        assertTrue(raster.isVisible(-8, -8, -40, 8, 8, -24));
        assertFalse(raster.isVisible(-30, -8, -40, -20, 8, -24));
    }

    @Test
    void gapBetweenTwoWallsKeepsTheBoxBehindVisible() {
        DepthRaster raster = camera();
        raster.addOccluder(-64, -64, -18, -0.25F, 64, -16);
        raster.addOccluder(0.25F, -64, -18, 64, 64, -16);
        assertTrue(raster.isVisible(-1, -1, -60, 1, 1, -58));
    }

    @Test
    void boxesAroundTheCameraNeverHideWhatIsInsideThem() {
        DepthRaster raster = camera();
        raster.addOccluder(-4, -4, -4, 4, 4, 4);
        assertTrue(raster.isVisible(-1, -1, -3, 1, 1, -2));
        raster.addOccluder(-64, -64, -18, 64, 64, -16);
        assertTrue(raster.isVisible(-1, -1, -40, 1, 1, 1));
    }

    @Test
    void wallsReachingBehindTheCameraAreClippedAndStillOcclude() {
        DepthRaster raster = camera();
        assertTrue(raster.addOccluder(2, -64, -60, 64, 64, 10));
        assertFalse(raster.isVisible(30, -2, -60, 34, 2, -50));
        assertTrue(raster.isVisible(-6, -2, -60, -2, 2, -50));
    }

    @Test
    void boxesBehindTheCameraAreLeftToTheFrustum() {
        DepthRaster raster = camera();
        raster.addOccluder(-64, -64, -18, 64, 64, -16);
        assertTrue(raster.isVisible(-8, -8, 20, 8, 8, 30));
    }

    @Test
    void orthographicLightViewHidesBuriedBoxes() {
        DepthRaster raster = new DepthRaster(64, 64);
        raster.begin(new ClipMatrix().orthographic(-64, 64, -64, 64, -256, 256), false);
        raster.addOccluder(-64, -64, 0, 64, 64, 16);
        assertFalse(raster.isVisible(-8, -8, -40, 8, 8, -24));
        assertTrue(raster.isVisible(-8, -8, 20, 8, 8, 30));
        assertTrue(raster.isVisible(-80, -8, -40, -56, 8, -24));
    }

    @Test
    void randomScenesNeverHideAVisibleBox() {
        Random random = new Random(42);
        int hidden = 0;
        for (int scene = 0; scene < 200; scene++) {
            DepthRaster raster = new DepthRaster(64, 32);
            ClipMatrix matrix = new ClipMatrix().perspective(FOV, 2.0F, 0.5F, 256.0F);
            raster.begin(matrix, true);
            float[][] occluders = new float[12][];
            for (int index = 0; index < occluders.length; index++) {
                occluders[index] = randomBox(random, 6.0F);
                float[] box = occluders[index];
                raster.addOccluder(box[0], box[1], box[2], box[3], box[4], box[5]);
            }
            for (int probe = 0; probe < 40; probe++) {
                float[] box = randomBox(random, 3.0F);
                if (!raster.isVisible(box[0], box[1], box[2], box[3], box[4], box[5])) {
                    hidden++;
                    assertTrue(fullyHidden(matrix, box, occluders), "scene " + scene + " probe " + probe);
                }
            }
        }
        assertTrue(hidden > 50, "hidden " + hidden);
    }

    private static float[] randomBox(Random random, float size) {
        float x = random.nextFloat() * 60 - 30;
        float y = random.nextFloat() * 30 - 15;
        float z = -4 - random.nextFloat() * 60;
        float sx = 0.5F + random.nextFloat() * size;
        float sy = 0.5F + random.nextFloat() * size;
        float sz = 0.5F + random.nextFloat() * size;
        return new float[] {x, y, z - sz, x + sx, y + sy, z};
    }

    private static boolean fullyHidden(ClipMatrix matrix, float[] box, float[][] occluders) {
        for (int sample = 0; sample < 4000; sample++) {
            float t = sample / 4000.0F;
            float x = box[0] + (box[3] - box[0]) * frac(t * 13.37F);
            float y = box[1] + (box[4] - box[1]) * frac(t * 7.77F);
            float z = box[2] + (box[5] - box[2]) * frac(t * 3.11F);
            if (!blocked(x, y, z, occluders) && onScreen(matrix, x, y, z)) {
                return false;
            }
        }
        return true;
    }

    private static boolean onScreen(ClipMatrix matrix, float x, float y, float z) {
        float w = matrix.w(x, y, z);
        return w > 0 && Math.abs(matrix.x(x, y, z) / w) <= 1 && Math.abs(matrix.y(x, y, z) / w) <= 1;
    }

    private static float frac(float value) {
        return value - (float) Math.floor(value);
    }

    private static boolean blocked(float x, float y, float z, float[][] occluders) {
        for (float[] box : occluders) {
            if (segmentHitsBox(x, y, z, box)) {
                return true;
            }
        }
        return false;
    }

    private static boolean segmentHitsBox(float x, float y, float z, float[] box) {
        float enter = 0.0F;
        float exit = 0.999F;
        float[] origin = {0, 0, 0};
        float[] target = {x, y, z};
        for (int axis = 0; axis < 3; axis++) {
            float direction = target[axis] - origin[axis];
            float low = box[axis];
            float high = box[axis + 3];
            if (Math.abs(direction) < 1.0E-6F) {
                if (origin[axis] < low || origin[axis] > high) {
                    return false;
                }
                continue;
            }
            float t0 = (low - origin[axis]) / direction;
            float t1 = (high - origin[axis]) / direction;
            enter = Math.max(enter, Math.min(t0, t1));
            exit = Math.min(exit, Math.max(t0, t1));
            if (enter > exit) {
                return false;
            }
        }
        return true;
    }
}
