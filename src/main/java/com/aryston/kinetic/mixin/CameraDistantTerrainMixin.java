package com.aryston.kinetic.mixin;

import com.aryston.kinetic.client.lod.DistantTerrain;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Camera.class)
abstract class CameraDistantTerrainMixin {
    @ModifyExpressionValue(method = "update(Lnet/minecraft/client/DeltaTracker;)V", at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(FF)F"))
    private float kinetic$reachDistantTerrain(float vanillaFar) {
        return DistantTerrain.drawing() ? Math.max(vanillaFar, DistantTerrain.farPlaneBlocks()) : vanillaFar;
    }
}
