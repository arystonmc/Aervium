package com.aryston.aervium.client.gpu;

import com.mojang.renderpearl.backend.vulkan.VulkanFeatureSets;
import com.mojang.renderpearl.backend.vulkan.init.FeatureSet;
import com.mojang.renderpearl.backend.vulkan.init.VulkanFeature;
import java.util.Set;

public final class IndirectCountFeature {
    public static final FeatureSet FEATURE_SET = new FeatureSet(
        "Aervium indirect draw count", Set.of(), Set.of(new VulkanFeature(VulkanFeatureSets.VK12_FEATURES_STRUCT, "drawIndirectCount"))
    );

    private IndirectCountFeature() {
    }
}
