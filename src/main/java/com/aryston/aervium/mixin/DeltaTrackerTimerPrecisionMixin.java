package com.aryston.aervium.mixin;

import com.aryston.aervium.core.budget.LapTimer;
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
    private final LapTimer aervium$gameLaps = new LapTimer();
    @Unique
    private final LapTimer aervium$realLaps = new LapTimer();
    @Unique
    private boolean aervium$gameAdvanced;

    @Inject(method = "advanceGameTime(J)I", at = @At("HEAD"), cancellable = true)
    private void aervium$advanceGameTimeInNanos(long currentMs, CallbackInfoReturnable<Integer> callback) {
        deltaTicks = (float) (aervium$gameLaps.lapMillis(Util.getNanos()) / targetMsptProvider.apply(msPerTick));
        deltaTickResidual += deltaTicks;
        int ticks = (int) deltaTickResidual;
        deltaTickResidual -= ticks;
        aervium$gameAdvanced = true;
        callback.setReturnValue(ticks);
    }

    @Inject(method = "advanceRealTime(J)V", at = @At("HEAD"), cancellable = true)
    private void aervium$advanceRealTimeInNanos(long currentMs, CallbackInfo callback) {
        long now = Util.getNanos();
        realtimeDeltaTicks = (float) (aervium$realLaps.lapMillis(now) / msPerTick);
        if (aervium$gameAdvanced) {
            aervium$gameAdvanced = false;
            aervium$catchUpToRendering(now);
        }
        callback.cancel();
    }

    @Unique
    private void aervium$catchUpToRendering(long now) {
        float lateTicks = (float) (aervium$gameLaps.elapsedMillis(now) / targetMsptProvider.apply(msPerTick));
        if (deltaTickResidual + lateTicks >= WHOLE_TICK) {
            return;
        }
        aervium$gameLaps.lapMillis(now);
        deltaTicks += lateTicks;
        deltaTickResidual += lateTicks;
    }
}
