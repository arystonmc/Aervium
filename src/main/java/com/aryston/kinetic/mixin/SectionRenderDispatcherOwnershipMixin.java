package com.aryston.kinetic.mixin;

import com.aryston.kinetic.client.chunk.SectionOwnership;
import java.util.concurrent.locks.ReentrantLock;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SectionRenderDispatcher.class)
abstract class SectionRenderDispatcherOwnershipMixin {
    @Shadow
    @Final
    private ReentrantLock copyLock;

    @Inject(method = "lock", at = @At("HEAD"), cancellable = true)
    private void kinetic$lockWithoutWaiting(CallbackInfo callback) {
        if (SectionOwnership.ownsAllocations()) {
            SectionOwnership.lockForUpload(copyLock);
            callback.cancel();
        }
    }

    @Inject(method = "unlock", at = @At("HEAD"), cancellable = true)
    private void kinetic$unlockIfHeld(CallbackInfo callback) {
        if (SectionOwnership.ownsAllocations() && !copyLock.isHeldByCurrentThread()) {
            callback.cancel();
        }
    }

    @Inject(method = "uploadTerrainBuffersToGpu", at = @At("HEAD"), cancellable = true)
    private void kinetic$uploadWhenHeld(CallbackInfo callback) {
        if (!copyLock.isHeldByCurrentThread()) {
            callback.cancel();
            return;
        }
        SectionOwnership.releaseRetired();
    }

    @Inject(method = "dispose", at = @At("HEAD"))
    private void kinetic$releaseBeforeDispose(CallbackInfo callback) {
        copyLock.lock();
        try {
            SectionOwnership.releaseRetired();
        } finally {
            copyLock.unlock();
        }
    }
}
