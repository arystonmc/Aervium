package com.aryston.kinetic.client.culling;

import net.minecraft.world.phys.AABB;

public final class ParticleOcclusion {
    private static final double QUAD_REACH = 1.5;
    private static final double MARGIN = 0.5;

    private static int tested;
    private static int hidden;

    private ParticleOcclusion() {
    }

    public static boolean hidden(double previousX, double previousY, double previousZ, double x, double y, double z, float quadSize) {
        if (!TerrainOcclusion.enabled() || !TerrainOcclusion.cameraHidesSections()) {
            return false;
        }
        double reach = Math.abs(quadSize) * QUAD_REACH + MARGIN;
        double minX = Math.min(previousX, x) - reach;
        double minY = Math.min(previousY, y) - reach;
        double minZ = Math.min(previousZ, z) - reach;
        double maxX = Math.max(previousX, x) + reach;
        double maxY = Math.max(previousY, y) + reach;
        double maxZ = Math.max(previousZ, z) + reach;
        tested++;
        if (TerrainOcclusion.isVisibleToCamera(minX, minY, minZ, maxX, maxY, maxZ)) {
            return false;
        }
        hidden++;
        if (CullingAudit.ENABLED) {
            TerrainOcclusion.auditHiddenBox(new AABB(minX + MARGIN, minY + MARGIN, minZ + MARGIN, maxX - MARGIN, maxY - MARGIN, maxZ - MARGIN));
        }
        return true;
    }

    public static int takeTested() {
        int value = tested;
        tested = 0;
        return value;
    }

    public static int takeHidden() {
        int value = hidden;
        hidden = 0;
        return value;
    }
}
