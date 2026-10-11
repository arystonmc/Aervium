package com.aryston.aervium.mixin;

import com.aryston.aervium.client.FrameRateGovernor;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.platform.FramerateLimitTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FramerateLimitTracker.class)
abstract class FramerateLimitTrackerMixin {
    @ModifyReturnValue(method = "getFramerateLimit", at = @At("RETURN"))
    private int aervium$governFrameRate(int vanillaLimit) {
        return FrameRateGovernor.limit(vanillaLimit);
    }
}
