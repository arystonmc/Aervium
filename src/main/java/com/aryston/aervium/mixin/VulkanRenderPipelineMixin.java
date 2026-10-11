package com.aryston.aervium.mixin;

import com.aryston.aervium.client.pipeline.ShaderCaches;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.renderpearl.backend.vulkan.VulkanRenderPipeline;
import java.nio.LongBuffer;
import org.lwjgl.vulkan.VkAllocationCallbacks;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkGraphicsPipelineCreateInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(VulkanRenderPipeline.class)
abstract class VulkanRenderPipelineMixin {
    @WrapOperation(
        method = "compile",
        at = @At(
            value = "INVOKE",
            target = "Lorg/lwjgl/vulkan/VK12;vkCreateGraphicsPipelines(Lorg/lwjgl/vulkan/VkDevice;JLorg/lwjgl/vulkan/VkGraphicsPipelineCreateInfo$Buffer;Lorg/lwjgl/vulkan/VkAllocationCallbacks;Ljava/nio/LongBuffer;)I"
        ),
        require = 1
    )
    private static int aervium$usePipelineCache(
        VkDevice device,
        long pipelineCache,
        VkGraphicsPipelineCreateInfo.Buffer createInfos,
        VkAllocationCallbacks allocator,
        LongBuffer pipelines,
        Operation<Integer> original
    ) {
        long cache = ShaderCaches.pipelines().map(caches -> caches.handle(device)).orElse(pipelineCache);
        long start = System.nanoTime();
        int result = original.call(device, cache, createInfos, allocator, pipelines);
        ShaderCaches.pipelines().ifPresent(caches -> caches.recordCreation(System.nanoTime() - start));
        return result;
    }
}
