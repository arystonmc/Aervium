package com.aryston.kinetic.mixin;

import com.aryston.kinetic.client.FrameTrace;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ClientLevel.class)
abstract class ClientLevelLightTimingMixin {
    @WrapOperation(method = "pollLightUpdates", at = @At(value = "INVOKE", target = "Ljava/lang/Runnable;run()V"))
    private void kinetic$countLightUpdate(Runnable update, Operation<Void> original) {
        original.call(update);
        FrameTrace.lightUpdated();
    }
}
