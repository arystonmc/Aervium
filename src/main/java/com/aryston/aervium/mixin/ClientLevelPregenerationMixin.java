package com.aryston.aervium.mixin;

import com.aryston.aervium.client.pregeneration.JoinPregeneration;
import com.aryston.aervium.client.pregeneration.PendingLightUpdates;
import java.util.Deque;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
abstract class ClientLevelPregenerationMixin implements PendingLightUpdates {
    @Shadow
    @Final
    private Deque<Runnable> lightUpdateQueue;

    @Override
    public int aervium$pendingLightUpdates() {
        return lightUpdateQueue.size();
    }

    @Inject(method = "pollLightUpdates", at = @At("HEAD"), cancellable = true)
    private void aervium$spreadLightUpdates(CallbackInfo callback) {
        if (!JoinPregeneration.spreadsWork()) {
            return;
        }
        callback.cancel();
        long deadline = System.nanoTime() + JoinPregeneration.LIGHT_BUDGET_NANOS;
        while (System.nanoTime() < deadline && !lightUpdateQueue.isEmpty()) {
            lightUpdateQueue.poll().run();
        }
    }
}
