package com.aryston.aervium.mixin;

import com.aryston.aervium.world.light.ServerLightTimings;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ThreadedLevelLightEngine.class)
abstract class ThreadedLevelLightEngineTimingMixin {
    @Unique
    private long aervium$batchStart;

    @Unique
    private long aervium$batchCpuStart;

    @Inject(method = "runUpdate", at = @At("HEAD"))
    private void aervium$batchStarted(CallbackInfo callback) {
        aervium$batchStart = System.nanoTime();
        aervium$batchCpuStart = ServerLightTimings.threadCpuNanos();
    }

    @Inject(method = "runUpdate", at = @At("RETURN"))
    private void aervium$batchFinished(CallbackInfo callback) {
        ServerLightTimings.batchFinished(aervium$batchStart, aervium$batchCpuStart);
    }
}
