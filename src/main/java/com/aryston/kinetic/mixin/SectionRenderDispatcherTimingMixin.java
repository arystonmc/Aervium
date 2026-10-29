package com.aryston.kinetic.mixin;

import com.aryston.kinetic.client.chunk.ChunkUploadTimings;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.concurrent.locks.ReentrantLock;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SectionRenderDispatcher.class)
abstract class SectionRenderDispatcherTimingMixin {
    @WrapOperation(method = "lock", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/locks/ReentrantLock;lock()V"))
    private void kinetic$timeLockWait(ReentrantLock lock, Operation<Void> original) {
        long start = System.nanoTime();
        original.call(lock);
        ChunkUploadTimings.lockWaited(start);
    }

    @Inject(method = "uploadTerrainBuffersToGpu", at = @At("HEAD"))
    private void kinetic$uploadStarted(CallbackInfo callback) {
        ChunkUploadTimings.uploadStarted();
    }

    @Inject(method = "uploadTerrainBuffersToGpu", at = @At("RETURN"))
    private void kinetic$uploadFinished(CallbackInfo callback) {
        ChunkUploadTimings.uploadFinished();
    }
}
