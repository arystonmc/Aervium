package com.aryston.aervium.mixin;

import com.aryston.aervium.client.chunk.BlockEntitySectionHolder;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SectionRenderDispatcher.RenderSection.class)
abstract class RenderSectionBlockEntityMixin implements BlockEntitySectionHolder {
    @Unique
    private volatile boolean aervium$mayHaveBlockEntities;

    @Override
    public boolean aervium$mayHaveBlockEntities() {
        return aervium$mayHaveBlockEntities;
    }

    @Inject(method = "setSectionMesh", at = @At("HEAD"))
    private void aervium$beforeMeshReplaced(SectionMesh mesh, CallbackInfoReturnable<SectionMesh> callback) {
        if (!mesh.getRenderableBlockEntities().isEmpty()) {
            aervium$mayHaveBlockEntities = true;
        }
    }

    @Inject(method = "setSectionMesh", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/atomic/AtomicReference;getAndSet(Ljava/lang/Object;)Ljava/lang/Object;", shift = At.Shift.AFTER))
    private void aervium$afterMeshReplaced(SectionMesh mesh, CallbackInfoReturnable<SectionMesh> callback) {
        aervium$mayHaveBlockEntities = !mesh.getRenderableBlockEntities().isEmpty();
    }

    @Inject(method = "reset", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/atomic/AtomicReference;getAndSet(Ljava/lang/Object;)Ljava/lang/Object;", shift = At.Shift.AFTER))
    private void aervium$afterMeshCleared(CallbackInfo callback) {
        aervium$mayHaveBlockEntities = false;
    }
}
