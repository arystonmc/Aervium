package com.aryston.kinetic.mixin;

import com.aryston.kinetic.world.hopper.HopperAudit;
import com.aryston.kinetic.world.hopper.HopperEntities;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.transfer.item.ContainerOrHandler;
import net.neoforged.neoforge.transfer.item.VanillaInventoryCodeHooks;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = VanillaInventoryCodeHooks.class, remap = false)
abstract class HopperEntityContainerSearchMixin {
    @Inject(method = "getEntityContainerOrHandler", at = @At("HEAD"), cancellable = true)
    private static void kinetic$skipEmptyContainerSearch(Level level, double x, double y, double z, @Nullable Direction side,
            CallbackInfoReturnable<ContainerOrHandler> callback) {
        if (!HopperAudit.ENABLED && HopperEntities.noContainersNear(level, kinetic$box(x, y, z))) {
            callback.setReturnValue(ContainerOrHandler.EMPTY);
        }
    }

    @Inject(method = "getEntityContainerOrHandler", at = @At("RETURN"))
    private static void kinetic$auditContainerSearch(Level level, double x, double y, double z, @Nullable Direction side,
            CallbackInfoReturnable<ContainerOrHandler> callback) {
        if (HopperAudit.ENABLED) {
            if (HopperEntities.noContainersNear(level, kinetic$box(x, y, z))) {
                ContainerOrHandler result = callback.getReturnValue();
                HopperAudit.containerSearch(result.container() == null && result.itemHandler() == null);
            } else {
                HopperAudit.containerSearched();
            }
        }
    }

    @Unique
    private static AABB kinetic$box(double x, double y, double z) {
        return new AABB(x - 0.5, y - 0.5, z - 0.5, x + 0.5, y + 0.5, z + 0.5);
    }
}
