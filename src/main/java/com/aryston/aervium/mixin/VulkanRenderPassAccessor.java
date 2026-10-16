package com.aryston.aervium.mixin;

import com.mojang.renderpearl.backend.vulkan.VulkanRenderPass;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(VulkanRenderPass.class)
public interface VulkanRenderPassAccessor {
    @Accessor("commandBuffer")
    VkCommandBuffer aervium$commandBuffer();

    @Invoker("pushDescriptors")
    void aervium$pushDescriptors();
}
