package com.aryston.kinetic.mixin;

import com.aryston.kinetic.world.collision.CollidableEntities;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntitySection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntitySection.class)
abstract class EntitySectionCollidableMixin<T extends EntityAccess> implements CollidableEntities.Counter {
    @Unique
    private int kinetic$collidable;

    @Override
    public int kinetic$collidable() {
        return kinetic$collidable;
    }

    @Inject(method = "add", at = @At("TAIL"))
    private void kinetic$added(T entity, CallbackInfo callback) {
        if (CollidableEntities.mayBeCollidedWith(entity)) {
            kinetic$collidable++;
        }
    }

    @Inject(method = "remove", at = @At("RETURN"))
    private void kinetic$removed(T entity, CallbackInfoReturnable<Boolean> callback) {
        if (callback.getReturnValueZ() && CollidableEntities.mayBeCollidedWith(entity)) {
            kinetic$collidable--;
        }
    }
}
