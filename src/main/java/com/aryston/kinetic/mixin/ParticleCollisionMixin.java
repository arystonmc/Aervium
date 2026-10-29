package com.aryston.kinetic.mixin;

import com.aryston.kinetic.client.particle.ParticleCollisions;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.List;
import net.minecraft.client.particle.Particle;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Particle.class)
abstract class ParticleCollisionMixin {
    @WrapOperation(
        method = "move(DDD)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;collideBoundingBox(Lnet/minecraft/world/phys/shapes/CollisionContext;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Lnet/minecraft/world/level/Level;Ljava/util/List;)Lnet/minecraft/world/phys/Vec3;"
        )
    )
    private Vec3 kinetic$skipCollisionsInAir(
        CollisionContext context, Vec3 movement, AABB box, Level level, List<VoxelShape> entityColliders, Operation<Vec3> original
    ) {
        if (entityColliders.isEmpty() && ParticleCollisions.throughAirOnly(level, box, movement)) {
            return movement;
        }
        return original.call(context, movement, box, level, entityColliders);
    }
}
