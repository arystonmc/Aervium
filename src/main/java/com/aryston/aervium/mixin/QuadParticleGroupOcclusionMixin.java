package com.aryston.aervium.mixin;

import com.aryston.aervium.client.culling.ParticleOcclusion;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.particle.QuadParticleGroup;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(QuadParticleGroup.class)
abstract class QuadParticleGroupOcclusionMixin {
    @WrapOperation(
        method = "extractRenderState",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/culling/Frustum;pointInFrustum(DDD)Z")
    )
    private boolean aervium$skipOccludedParticle(
        Frustum frustum, double x, double y, double z, Operation<Boolean> original,
        @Local(argsOnly = true) float partialTicks, @Local SingleQuadParticle particle
    ) {
        if (!original.call(frustum, x, y, z)) {
            return false;
        }
        ParticleAccessor previous = (ParticleAccessor) particle;
        return !ParticleOcclusion.hidden(
            previous.aervium$xo(), previous.aervium$yo(), previous.aervium$zo(), x, y, z, particle.getQuadSize(partialTicks)
        );
    }
}
