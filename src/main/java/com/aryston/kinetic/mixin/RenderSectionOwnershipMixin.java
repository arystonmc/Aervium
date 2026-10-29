package com.aryston.kinetic.mixin;

import com.aryston.kinetic.client.chunk.SectionOwnership;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.concurrent.locks.ReentrantLock;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SectionRenderDispatcher.RenderSection.class)
abstract class RenderSectionOwnershipMixin {
    @WrapOperation(method = "reset", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/locks/ReentrantLock;lock()V"))
    private void kinetic$resetWithoutLock(ReentrantLock lock, Operation<Void> original) {
        if (!SectionOwnership.ownsAllocations()) {
            original.call(lock);
        }
    }

    @WrapOperation(method = "reset", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/locks/ReentrantLock;unlock()V"))
    private void kinetic$resetWithoutUnlock(ReentrantLock lock, Operation<Void> original) {
        if (!SectionOwnership.ownsAllocations()) {
            original.call(lock);
        }
    }
}
