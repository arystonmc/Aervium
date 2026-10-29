package com.aryston.kinetic.mixin;

import com.aryston.kinetic.client.pregeneration.JoinPregeneration;
import net.minecraft.client.multiplayer.LevelLoadTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelLoadTracker.class)
abstract class LevelLoadTrackerPregenerationMixin {
    @Inject(method = "setServerChunkStatusView", at = @At("HEAD"))
    private void kinetic$adoptSingleplayerJoin(CallbackInfo callback) {
        JoinPregeneration.adopt((LevelLoadTracker) (Object) this);
    }

    @Inject(method = "startClientLoad", at = @At("TAIL"))
    private void kinetic$startTerrainWait(CallbackInfo callback) {
        JoinPregeneration.clientLoadStarted((LevelLoadTracker) (Object) this);
    }

    @Inject(method = "isLevelReady", at = @At("RETURN"), cancellable = true)
    private void kinetic$waitForPregeneratedTerrain(CallbackInfoReturnable<Boolean> callback) {
        if (callback.getReturnValueZ() && JoinPregeneration.holds((LevelLoadTracker) (Object) this)) {
            callback.setReturnValue(false);
        }
    }

    @Inject(method = "serverProgress", at = @At("RETURN"), cancellable = true)
    private void kinetic$includeTerrainProgress(CallbackInfoReturnable<Float> callback) {
        callback.setReturnValue(JoinPregeneration.progress((LevelLoadTracker) (Object) this, callback.getReturnValueF()));
    }
}
