package com.aryston.kinetic.client.gpu;

import com.aryston.kinetic.mixin.FrontendGpuDeviceAccessor;
import com.aryston.kinetic.mixin.VulkanCommandEncoderAccessor;
import com.aryston.kinetic.mixin.VulkanDeviceAccessor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.backend.vulkan.VulkanDevice;
import com.mojang.renderpearl.backend.vulkan.VulkanGpuBuffer;
import org.jspecify.annotations.Nullable;
import org.lwjgl.vulkan.VkCommandBuffer;

final class VulkanAccess {
    private VulkanAccess() {
    }

    static @Nullable VulkanDevice device() {
        GpuDevice device = RenderSystem.tryGetDevice();
        if (device instanceof FrontendGpuDeviceAccessor accessor && accessor.kinetic$backend() instanceof VulkanDevice vulkan) {
            return vulkan;
        }
        return null;
    }

    static boolean supportsIndirectCount(VulkanDevice device) {
        return ((VulkanDeviceAccessor) device).kinetic$enabledFeatures().contains(IndirectCountFeature.FEATURE_SET);
    }

    static VkCommandBuffer commandBuffer(VulkanDevice device) {
        return ((VulkanCommandEncoderAccessor) device.createCommandEncoder()).kinetic$commandBuffer();
    }

    static long handle(GpuBuffer buffer) {
        return ((VulkanGpuBuffer) buffer).vkBuffer();
    }
}
