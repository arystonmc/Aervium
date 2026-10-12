package com.aryston.aervium.mixin;

import com.aryston.aervium.world.hopper.HopperAudit;
import com.aryston.aervium.world.hopper.HopperEntities;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HopperBlockEntity.class)
abstract class HopperBlockEntityItemSearchMixin {
    @Inject(method = "getItemsAtAndAbove", at = @At("HEAD"), cancellable = true)
    private static void aervium$skipEmptyItemSearch(Level level, Hopper hopper, CallbackInfoReturnable<List<ItemEntity>> callback) {
        if (!HopperAudit.ENABLED && HopperEntities.noItemsNear(level, aervium$suckBox(hopper))) {
            callback.setReturnValue(new ArrayList<>());
        }
    }

    @Inject(method = "getItemsAtAndAbove", at = @At("RETURN"))
    private static void aervium$auditItemSearch(Level level, Hopper hopper, CallbackInfoReturnable<List<ItemEntity>> callback) {
        if (HopperAudit.ENABLED) {
            if (HopperEntities.noItemsNear(level, aervium$suckBox(hopper))) {
                HopperAudit.itemSearch(callback.getReturnValue().isEmpty());
            } else {
                HopperAudit.itemSearched();
            }
        }
    }

    @Unique
    private static AABB aervium$suckBox(Hopper hopper) {
        return hopper.getSuckAabb().move(hopper.getLevelX() - 0.5, hopper.getLevelY() - 0.5, hopper.getLevelZ() - 0.5);
    }
}
