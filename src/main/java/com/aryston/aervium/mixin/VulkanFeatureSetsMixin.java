package com.aryston.aervium.mixin;

import com.aryston.aervium.client.gpu.IndirectCountFeature;
import com.mojang.renderpearl.backend.vulkan.VulkanFeatureSets;
import com.mojang.renderpearl.backend.vulkan.init.FeatureSet;
import java.util.Set;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VulkanFeatureSets.class)
abstract class VulkanFeatureSetsMixin {
    @Inject(method = "optionalFeatureSets", at = @At("RETURN"))
    private static void aervium$requestIndirectCount(CallbackInfoReturnable<Set<FeatureSet>> callback) {
        callback.getReturnValue().add(IndirectCountFeature.FEATURE_SET);
    }
}
