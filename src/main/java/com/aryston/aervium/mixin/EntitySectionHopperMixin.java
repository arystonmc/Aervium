package com.aryston.aervium.mixin;

import com.aryston.aervium.world.hopper.HopperEntities;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntitySection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntitySection.class)
abstract class EntitySectionHopperMixin<T extends EntityAccess> implements HopperEntities.Counter {
    @Unique
    private int aervium$items;
    @Unique
    private int aervium$containers;

    @Override
    public int aervium$items() {
        return aervium$items;
    }

    @Override
    public int aervium$containers() {
        return aervium$containers;
    }

    @Inject(method = "add", at = @At("TAIL"))
    private void aervium$hopperAdded(T entity, CallbackInfo callback) {
        if (HopperEntities.isItem(entity)) {
            aervium$items++;
        }
        if (HopperEntities.mayHoldItems(entity)) {
            aervium$containers++;
        }
    }

    @Inject(method = "remove", at = @At("RETURN"))
    private void aervium$hopperRemoved(T entity, CallbackInfoReturnable<Boolean> callback) {
        if (!callback.getReturnValueZ()) {
            return;
        }
        if (HopperEntities.isItem(entity)) {
            aervium$items--;
        }
        if (HopperEntities.mayHoldItems(entity)) {
            aervium$containers--;
        }
    }
}
