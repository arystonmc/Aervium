package com.aryston.kinetic.mixin;

import com.aryston.kinetic.core.telemetry.FrameCostRecorder;
import net.minecraft.client.FramerateLimiter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FramerateLimiter.class)
abstract class FramerateLimiterTimingMixin {
    @Unique
    private static long kinetic$limitStart;

    @Inject(method = "limitDisplayFPS", at = @At("HEAD"))
    private static void kinetic$limitStarted(int framerateLimit, CallbackInfo callback) {
        kinetic$limitStart = System.nanoTime();
    }

    @Inject(method = "limitDisplayFPS", at = @At("RETURN"))
    private static void kinetic$limitFinished(int framerateLimit, CallbackInfo callback) {
        FrameCostRecorder.SHARED.waited(System.nanoTime() - kinetic$limitStart);
    }
}
