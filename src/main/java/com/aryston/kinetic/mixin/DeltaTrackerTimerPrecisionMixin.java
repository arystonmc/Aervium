package com.aryston.kinetic.mixin;

import com.aryston.kinetic.core.budget.LapTimer;
import it.unimi.dsi.fastutil.floats.FloatUnaryOperator;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.client.DeltaTracker$Timer")
abstract class DeltaTrackerTimerPrecisionMixin {
    @Unique
    private static final float WHOLE_TICK = 1.0F;

    @Shadow
    private float deltaTicks;
    @Shadow
    private float deltaTickResidual;
    @Shadow
    private float realtimeDeltaTicks;
    @Shadow
    @Final
    private float msPerTick;
    @Shadow
    @Final
    private FloatUnaryOperator targetMsptProvider;

    @Unique
    private final LapTimer kinetic$gameLaps = new LapTimer();
    @Unique
    private final LapTimer kinetic$realLaps = new LapTimer();
    @Unique
    private boolean kinetic$gameAdvanced;

    @Inject(method = "advanceGameTime(J)I", at = @At("HEAD"), cancellable = true)
    private void kinetic$advanceGameTimeInNanos(long currentMs, CallbackInfoReturnable<Integer> callback) {
        deltaTicks = (float) (kinetic$gameLaps.lapMillis(Util.getNanos()) / targetMsptProvider.apply(msPerTick));
        deltaTickResidual += deltaTicks;
        int ticks = (int) deltaTickResidual;
        deltaTickResidual -= ticks;
        kinetic$gameAdvanced = true;
        callback.setReturnValue(ticks);
    }

    @Inject(method = "advanceRealTime(J)V", at = @At("HEAD"), cancellable = true)
    private void kinetic$advanceRealTimeInNanos(long currentMs, CallbackInfo callback) {
        long now = Util.getNanos();
        realtimeDeltaTicks = (float) (kinetic$realLaps.lapMillis(now) / msPerTick);
        if (kinetic$gameAdvanced) {
            kinetic$gameAdvanced = false;
            kinetic$catchUpToRendering(now);
        }
        callback.cancel();
    }

    @Unique
    private void kinetic$catchUpToRendering(long now) {
        float lateTicks = (float) (kinetic$gameLaps.elapsedMillis(now) / targetMsptProvider.apply(msPerTick));
        if (deltaTickResidual + lateTicks >= WHOLE_TICK) {
            return;
        }
        kinetic$gameLaps.lapMillis(now);
        deltaTicks += lateTicks;
        deltaTickResidual += lateTicks;
    }
}
