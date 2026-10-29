package com.aryston.kinetic.mixin;

import com.aryston.kinetic.client.chunk.MeshVersionHolder;
import com.aryston.kinetic.client.chunk.SectionDrawRecord;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SectionRenderDispatcher.RenderSection.class)
abstract class RenderSectionMeshVersionMixin implements MeshVersionHolder {
    @Unique
    private volatile int kinetic$meshVersion;

    @Override
    public int kinetic$meshVersion() {
        return kinetic$meshVersion;
    }

    @Inject(method = "setSectionMesh", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/atomic/AtomicReference;getAndSet(Ljava/lang/Object;)Ljava/lang/Object;", shift = At.Shift.AFTER))
    private void kinetic$meshReplaced(CallbackInfoReturnable<?> callback) {
        kinetic$meshVersion = SectionDrawRecord.nextMeshVersion();
    }

    @Inject(method = "reset", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/atomic/AtomicReference;getAndSet(Ljava/lang/Object;)Ljava/lang/Object;", shift = At.Shift.AFTER))
    private void kinetic$meshCleared(CallbackInfo callback) {
        kinetic$meshVersion = SectionDrawRecord.nextMeshVersion();
    }
}
