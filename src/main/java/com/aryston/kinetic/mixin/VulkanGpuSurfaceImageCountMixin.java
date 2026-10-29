package com.aryston.kinetic.mixin;

import com.aryston.kinetic.client.gpu.SwapchainImages;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.renderpearl.backend.vulkan.VulkanGpuSurface;
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;
import org.lwjgl.vulkan.VkSwapchainCreateInfoKHR;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(VulkanGpuSurface.class)
abstract class VulkanGpuSurfaceImageCountMixin {
    @WrapOperation(
        method = "configure",
        at = @At(value = "INVOKE", target = "Lorg/lwjgl/vulkan/VkSwapchainCreateInfoKHR;minImageCount(I)Lorg/lwjgl/vulkan/VkSwapchainCreateInfoKHR;")
    )
    private VkSwapchainCreateInfoKHR kinetic$requestSpareImages(
        VkSwapchainCreateInfoKHR info, int count, Operation<VkSwapchainCreateInfoKHR> original, @Local VkSurfaceCapabilitiesKHR capabilities
    ) {
        return original.call(info, SwapchainImages.count(count, capabilities.maxImageCount()));
    }
}
