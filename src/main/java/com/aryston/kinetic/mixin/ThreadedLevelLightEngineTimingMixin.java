package com.aryston.kinetic.mixin;

import com.aryston.kinetic.world.light.ServerLightTimings;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ThreadedLevelLightEngine.class)
abstract class ThreadedLevelLightEngineTimingMixin {
    @Unique
    private long kinetic$batchStart;

    @Unique
    private long kinetic$batchCpuStart;

    @Inject(method = "runUpdate", at = @At("HEAD"))
    private void kinetic$batchStarted(CallbackInfo callback) {
        kinetic$batchStart = System.nanoTime();
        kinetic$batchCpuStart = ServerLightTimings.threadCpuNanos();
    }

    @Inject(method = "runUpdate", at = @At("RETURN"))
    private void kinetic$batchFinished(CallbackInfo callback) {
        ServerLightTimings.batchFinished(kinetic$batchStart, kinetic$batchCpuStart);
    }
}
