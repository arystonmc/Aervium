package com.aryston.kinetic.mixin;

import com.aryston.kinetic.client.chunk.SectionOwnership;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.concurrent.locks.ReentrantLock;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.client.renderer.chunk.SectionRenderDispatcher$RenderSection$CompileTask")
abstract class CompileTaskOwnershipMixin {
    @WrapOperation(
        method = "doTask",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher$RenderSection;releaseSectionMesh(Lnet/minecraft/client/renderer/chunk/SectionMesh;)V")
    )
    private void kinetic$retireOnRenderThread(SectionRenderDispatcher.RenderSection section, SectionMesh mesh, Operation<Void> original) {
        if (SectionOwnership.ownsAllocations()) {
            original.call(section, mesh);
            return;
        }
        SectionOwnership.retire(section, mesh);
    }

    @WrapOperation(method = "doTask", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/locks/ReentrantLock;lock()V"))
    private void kinetic$lockOnlyOnRenderThread(ReentrantLock lock, Operation<Void> original) {
        if (SectionOwnership.ownsAllocations()) {
            original.call(lock);
        }
    }

    @WrapOperation(method = "doTask", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/locks/ReentrantLock;unlock()V"))
    private void kinetic$unlockOnlyOnRenderThread(ReentrantLock lock, Operation<Void> original) {
        if (SectionOwnership.ownsAllocations()) {
            original.call(lock);
        }
    }

    @WrapOperation(method = "doTask", at = @At(value = "INVOKE", target = "Ljava/lang/Thread;onSpinWait()V"))
    private void kinetic$parkWhileStagingFull(Operation<Void> original) {
        SectionOwnership.waitForStagingSpace();
    }
}
