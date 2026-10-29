package com.aryston.kinetic.mixin;

import com.aryston.kinetic.client.FrameRateGovernor;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.platform.FramerateLimitTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FramerateLimitTracker.class)
abstract class FramerateLimitTrackerMixin {
    @ModifyReturnValue(method = "getFramerateLimit", at = @At("RETURN"))
    private int kinetic$governFrameRate(int vanillaLimit) {
        return FrameRateGovernor.limit(vanillaLimit);
    }
}
