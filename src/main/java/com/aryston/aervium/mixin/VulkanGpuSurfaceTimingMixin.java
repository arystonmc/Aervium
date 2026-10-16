package com.aryston.aervium.mixin;

import com.aryston.aervium.client.FrameTrace;
import com.aryston.aervium.client.gpu.GpuStageTimer;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.mojang.renderpearl.backend.api.CommandEncoderBackend;
import com.mojang.renderpearl.backend.vulkan.VulkanGpuSurface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VulkanGpuSurface.class)
abstract class VulkanGpuSurfaceTimingMixin {
    @Unique
    private long aervium$callStart;

    @Inject(method = "acquireNextTexture", at = @At("HEAD"))
    private void aervium$acquireStarted(CallbackInfo callback) {
        aervium$callStart = System.nanoTime();
    }

    @Inject(method = "acquireNextTexture", at = @At("RETURN"))
    private void aervium$acquireFinished(CallbackInfo callback) {
        FrameTrace.imageAcquired(System.nanoTime() - aervium$callStart);
    }

    @Inject(method = "blitFromTexture", at = @At("HEAD"))
    private void aervium$swapchainCopyStarted(CommandEncoderBackend encoder, GpuTextureView texture, CallbackInfo callback) {
        GpuStageTimer.swapchainCopyStarting();
    }

    @Inject(method = "present", at = @At("HEAD"))
    private void aervium$presentStarted(CallbackInfo callback) {
        aervium$callStart = System.nanoTime();
    }

    @Inject(method = "present", at = @At("RETURN"))
    private void aervium$presentFinished(CallbackInfo callback) {
        FrameTrace.imagePresented(System.nanoTime() - aervium$callStart);
    }
}
