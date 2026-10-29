package com.aryston.kinetic.mixin;

import com.aryston.kinetic.client.FrameTrace;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.renderpearl.backend.vulkan.VulkanTransientMemory;
import java.nio.LongBuffer;
import org.jspecify.annotations.Nullable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.util.vma.VmaAllocationCreateInfo;
import org.lwjgl.util.vma.VmaAllocationInfo;
import org.lwjgl.vulkan.VkBufferCreateInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(VulkanTransientMemory.class)
abstract class VulkanTransientMemoryTraceMixin {
    @WrapOperation(
        method = "allocateVulkanBlock",
        at = @At(
            value = "INVOKE",
            target = "Lorg/lwjgl/util/vma/Vma;vmaCreateBuffer(JLorg/lwjgl/vulkan/VkBufferCreateInfo;Lorg/lwjgl/util/vma/VmaAllocationCreateInfo;Ljava/nio/LongBuffer;Lorg/lwjgl/PointerBuffer;Lorg/lwjgl/util/vma/VmaAllocationInfo;)I"
        )
    )
    private int kinetic$traceBlockCreated(
        long allocator, VkBufferCreateInfo info, VmaAllocationCreateInfo allocation, LongBuffer buffer, PointerBuffer handle,
        @Nullable VmaAllocationInfo result, Operation<Integer> original
    ) {
        long start = System.nanoTime();
        int status = original.call(allocator, info, allocation, buffer, handle, result);
        FrameTrace.gpuMemoryCreated(info.size(), System.nanoTime() - start);
        return status;
    }

    @WrapOperation(method = "freeVulkanBlock", at = @At(value = "INVOKE", target = "Lorg/lwjgl/util/vma/Vma;vmaDestroyBuffer(JJJ)V"))
    private void kinetic$traceBlockDestroyed(long allocator, long buffer, long allocation, Operation<Void> original) {
        long start = System.nanoTime();
        original.call(allocator, buffer, allocation);
        FrameTrace.gpuMemoryDestroyed(System.nanoTime() - start);
    }
}
