package com.aryston.aervium.client.pregeneration;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;

final class OmniFrustum extends Frustum {
    OmniFrustum() {
        super(new Matrix4f(), new Matrix4f());
    }

    @Override
    public int cubeInFrustum(BoundingBox box) {
        return FrustumIntersection.INSIDE;
    }

    @Override
    public boolean isVisible(AABB box) {
        return true;
    }
}
