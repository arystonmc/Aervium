package com.aryston.aervium.mixin;

import com.aryston.aervium.client.FrameTrace;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.renderpearl.backend.vulkan.DestructionQueue;
import com.mojang.renderpearl.backend.vulkan.VulkanCommandEncoder;
import com.mojang.renderpearl.backend.vulkan.VulkanQueue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(VulkanCommandEncoder.class)
abstract class VulkanCommandEncoderTimingMixin {
    @WrapOperation(
        method = "submit",
        at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/backend/vulkan/VulkanQueue$Submission;close()V")
    )
    private void aervium$timeQueueSubmit(VulkanQueue.Submission submission, Operation<Void> original) {
        long start = System.nanoTime();
        original.call(submission);
        FrameTrace.queueSubmitted(System.nanoTime() - start);
    }

    @WrapOperation(
        method = "submit",
        at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/backend/vulkan/VulkanCommandEncoder;awaitSubmitCompletion(JJ)Z")
    )
    private boolean aervium$timeGpuWait(VulkanCommandEncoder encoder, long submitIndex, long timeout, Operation<Boolean> original) {
        long start = System.nanoTime();
        boolean completed = original.call(encoder, submitIndex, timeout);
        FrameTrace.gpuWaited(System.nanoTime() - start);
        return completed;
    }

    @WrapOperation(
        method = "submit",
        at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/backend/vulkan/DestructionQueue;rotate()Z")
    )
    private boolean aervium$timeDestruction(DestructionQueue<?> queue, Operation<Boolean> original) {
        long start = System.nanoTime();
        boolean destroyed = original.call(queue);
        FrameTrace.resourcesDestroyed(System.nanoTime() - start);
        return destroyed;
    }
}
