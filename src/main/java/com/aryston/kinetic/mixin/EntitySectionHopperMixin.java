package com.aryston.kinetic.mixin;

import com.aryston.kinetic.world.hopper.HopperEntities;
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
    private int kinetic$items;
    @Unique
    private int kinetic$containers;

    @Override
    public int kinetic$items() {
        return kinetic$items;
    }

    @Override
    public int kinetic$containers() {
        return kinetic$containers;
    }

    @Inject(method = "add", at = @At("TAIL"))
    private void kinetic$hopperAdded(T entity, CallbackInfo callback) {
        if (HopperEntities.isItem(entity)) {
            kinetic$items++;
        }
        if (HopperEntities.mayHoldItems(entity)) {
            kinetic$containers++;
        }
    }

    @Inject(method = "remove", at = @At("RETURN"))
    private void kinetic$hopperRemoved(T entity, CallbackInfoReturnable<Boolean> callback) {
        if (!callback.getReturnValueZ()) {
            return;
        }
        if (HopperEntities.isItem(entity)) {
            kinetic$items--;
        }
        if (HopperEntities.mayHoldItems(entity)) {
            kinetic$containers--;
        }
    }
}
