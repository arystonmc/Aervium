package com.aryston.kinetic.core.culling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OcclusionPassTest {
    private static final OccluderBoxes SOLID = solid();

    @Test
    void solidWallHidesTheSectionsBehindIt() {
        DepthRaster raster = new DepthRaster(128, 64);
        raster.begin(new ClipMatrix().perspective((float) Math.toRadians(70.0), 2.0F, 0.5F, 512.0F), true);
        OcclusionPass pass = new OcclusionPass();
        int[] behind = new int[9];
        int slot = 0;
        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                if (Math.abs(x) <= 1 && Math.abs(y) <= 1) {
                    behind[slot++] = pass.add(x * 16 - 8, y * 16 - 8, -96, OccluderBoxes.NONE, false);
                }
            }
        }
        int[] wall = new int[400];
        int walls = 0;
        for (int x = -10; x < 10; x++) {
            for (int y = -10; y < 10; y++) {
                wall[walls++] = pass.add(x * 16, y * 16, -48, SOLID, true);
            }
        }
        pass.run(raster, 10_000);
        for (int index : behind) {
            assertFalse(pass.isVisible(index));
        }
        int visibleWall = 0;
        for (int index = 0; index < walls; index++) {
            if (pass.isVisible(wall[index])) {
                visibleWall++;
            }
        }
        assertEquals(walls, visibleWall);
        assertEquals(behind.length, pass.hidden());
    }

    @Test
    void sectionsThatCannotOccludeHideNothing() {
        DepthRaster raster = new DepthRaster(128, 64);
        raster.begin(new ClipMatrix().perspective((float) Math.toRadians(70.0), 2.0F, 0.5F, 512.0F), true);
        OcclusionPass pass = new OcclusionPass();
        int target = pass.add(-8, -8, -96, OccluderBoxes.NONE, false);
        for (int x = -10; x < 10; x++) {
            for (int y = -10; y < 10; y++) {
                pass.add(x * 16, y * 16, -48, SOLID, false);
            }
        }
        pass.run(raster, 10_000);
        assertTrue(pass.isVisible(target));
        assertEquals(0, pass.occluderBoxes());
    }

    @Test
    void depthFromWKeepsScreenPositionsAndOrdersByDistance() {
        ClipMatrix matrix = new ClipMatrix().perspective((float) Math.toRadians(70.0), 2.0F, 0.05F, 100.0F).depthFromW(0.5F, 512.0F);
        float near = matrix.z(0, 0, -1) / matrix.w(0, 0, -1);
        float far = matrix.z(0, 0, -200) / matrix.w(0, 0, -200);
        assertTrue(near >= 0.0F && near < far && far < 1.0F);
        assertEquals(0.0F, matrix.z(0, 0, -0.5F) / matrix.w(0, 0, -0.5F), 1.0E-6F);
    }

    private static OccluderBoxes solid() {
        OpaqueMask mask = new OpaqueMask();
        for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    mask.set(x, y, z);
                }
            }
        }
        return mask.boxes();
    }
}
