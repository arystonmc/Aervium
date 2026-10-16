package com.aryston.aervium.mixin;

import com.aryston.aervium.client.chunk.SectionOwnership;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.client.renderer.chunk.SectionRenderDispatcher$RenderSection$ResortTransparencyTask")
abstract class ResortTransparencyTaskOwnershipMixin {
    @WrapOperation(method = "doTask", at = @At(value = "INVOKE", target = "Ljava/lang/Thread;onSpinWait()V"))
    private void aervium$parkWhileStagingFull(Operation<Void> original) {
        SectionOwnership.waitForStagingSpace();
    }
}
