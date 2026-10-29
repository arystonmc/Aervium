package com.aryston.kinetic.mixin;

import com.aryston.kinetic.client.FrameTrace;
import com.aryston.kinetic.client.gpu.GpuStageTimer;
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
    private long kinetic$callStart;

    @Inject(method = "acquireNextTexture", at = @At("HEAD"))
    private void kinetic$acquireStarted(CallbackInfo callback) {
        kinetic$callStart = System.nanoTime();
    }

    @Inject(method = "acquireNextTexture", at = @At("RETURN"))
    private void kinetic$acquireFinished(CallbackInfo callback) {
        FrameTrace.imageAcquired(System.nanoTime() - kinetic$callStart);
    }

    @Inject(method = "blitFromTexture", at = @At("HEAD"))
    private void kinetic$swapchainCopyStarted(CommandEncoderBackend encoder, GpuTextureView texture, CallbackInfo callback) {
        GpuStageTimer.swapchainCopyStarting();
    }

    @Inject(method = "present", at = @At("HEAD"))
    private void kinetic$presentStarted(CallbackInfo callback) {
        kinetic$callStart = System.nanoTime();
    }

    @Inject(method = "present", at = @At("RETURN"))
    private void kinetic$presentFinished(CallbackInfo callback) {
        FrameTrace.imagePresented(System.nanoTime() - kinetic$callStart);
    }
}
