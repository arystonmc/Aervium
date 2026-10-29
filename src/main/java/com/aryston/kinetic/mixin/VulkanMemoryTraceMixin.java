package com.aryston.kinetic.mixin;

import com.aryston.kinetic.client.FrameTrace;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.nio.LongBuffer;
import org.jspecify.annotations.Nullable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.util.vma.VmaAllocationCreateInfo;
import org.lwjgl.util.vma.VmaAllocationInfo;
import org.lwjgl.vulkan.VkBufferCreateInfo;
import org.lwjgl.vulkan.VkImageCreateInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(
    targets = {
        "com.mojang.renderpearl.backend.vulkan.VulkanGpuBuffer$Direct",
        "com.mojang.renderpearl.backend.vulkan.VulkanGpuTexture"
    }
)
abstract class VulkanMemoryTraceMixin {
    @WrapOperation(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Lorg/lwjgl/util/vma/Vma;vmaCreateBuffer(JLorg/lwjgl/vulkan/VkBufferCreateInfo;Lorg/lwjgl/util/vma/VmaAllocationCreateInfo;Ljava/nio/LongBuffer;Lorg/lwjgl/PointerBuffer;Lorg/lwjgl/util/vma/VmaAllocationInfo;)I"
        ),
        require = 0
    )
    private static int kinetic$traceBufferCreated(
        long allocator, VkBufferCreateInfo info, VmaAllocationCreateInfo allocation, LongBuffer buffer, PointerBuffer handle,
        @Nullable VmaAllocationInfo result, Operation<Integer> original
    ) {
        long start = System.nanoTime();
        int status = original.call(allocator, info, allocation, buffer, handle, result);
        FrameTrace.gpuMemoryCreated(info.size(), System.nanoTime() - start);
        return status;
    }

    @WrapOperation(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Lorg/lwjgl/util/vma/Vma;vmaCreateImage(JLorg/lwjgl/vulkan/VkImageCreateInfo;Lorg/lwjgl/util/vma/VmaAllocationCreateInfo;Ljava/nio/LongBuffer;Lorg/lwjgl/PointerBuffer;Lorg/lwjgl/util/vma/VmaAllocationInfo;)I"
        ),
        require = 0
    )
    private static int kinetic$traceImageCreated(
        long allocator, VkImageCreateInfo info, VmaAllocationCreateInfo allocation, LongBuffer image, PointerBuffer handle,
        @Nullable VmaAllocationInfo result, Operation<Integer> original
    ) {
        long start = System.nanoTime();
        int status = original.call(allocator, info, allocation, image, handle, result);
        FrameTrace.gpuMemoryCreated(0L, System.nanoTime() - start);
        return status;
    }

    @WrapOperation(
        method = "destroy",
        at = @At(value = "INVOKE", target = "Lorg/lwjgl/util/vma/Vma;vmaDestroyBuffer(JJJ)V"),
        require = 0
    )
    private void kinetic$traceBufferDestroyed(long allocator, long buffer, long allocation, Operation<Void> original) {
        long start = System.nanoTime();
        original.call(allocator, buffer, allocation);
        FrameTrace.gpuMemoryDestroyed(System.nanoTime() - start);
    }

    @WrapOperation(method = "destroy", at = @At(value = "INVOKE", target = "Lorg/lwjgl/util/vma/Vma;vmaDestroyImage(JJJ)V"), require = 0)
    private void kinetic$traceImageDestroyed(long allocator, long image, long allocation, Operation<Void> original) {
        long start = System.nanoTime();
        original.call(allocator, image, allocation);
        FrameTrace.gpuMemoryDestroyed(System.nanoTime() - start);
    }
}
