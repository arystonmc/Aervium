package com.aryston.aervium.mixin;

import com.aryston.aervium.world.collision.CollidableEntities;
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
    private int aervium$collidable;

    @Override
    public int aervium$collidable() {
        return aervium$collidable;
    }

    @Inject(method = "add", at = @At("TAIL"))
    private void aervium$added(T entity, CallbackInfo callback) {
        if (CollidableEntities.mayBeCollidedWith(entity)) {
            aervium$collidable++;
        }
    }

    @Inject(method = "remove", at = @At("RETURN"))
    private void aervium$removed(T entity, CallbackInfoReturnable<Boolean> callback) {
        if (callback.getReturnValueZ() && CollidableEntities.mayBeCollidedWith(entity)) {
            aervium$collidable--;
        }
    }
}
