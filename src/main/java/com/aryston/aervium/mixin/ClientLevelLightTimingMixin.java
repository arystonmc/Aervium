package com.aryston.aervium.mixin;

import com.aryston.aervium.client.FrameTrace;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ClientLevel.class)
abstract class ClientLevelLightTimingMixin {
    @WrapOperation(method = "pollLightUpdates", at = @At(value = "INVOKE", target = "Ljava/lang/Runnable;run()V"))
    private void aervium$countLightUpdate(Runnable update, Operation<Void> original) {
        original.call(update);
        FrameTrace.lightUpdated();
    }
}
