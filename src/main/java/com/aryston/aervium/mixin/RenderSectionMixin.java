package com.aryston.aervium.mixin;

import com.aryston.aervium.client.chunk.SectionDrawRecord;
import com.aryston.aervium.client.chunk.SectionDrawRecordHolder;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SectionRenderDispatcher.RenderSection.class)
abstract class RenderSectionMixin implements SectionDrawRecordHolder {
    @Unique
    private @Nullable SectionDrawRecord aervium$drawRecord;

    @Unique
    private volatile int aervium$meshVersion;

    @Override
    public @Nullable SectionDrawRecord aervium$drawRecord() {
        return aervium$drawRecord;
    }

    @Override
    public void aervium$setDrawRecord(SectionDrawRecord record) {
        aervium$drawRecord = record;
    }

    @Override
    public int aervium$meshVersion() {
        return aervium$meshVersion;
    }

    @Inject(method = "setSectionMesh", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/atomic/AtomicReference;getAndSet(Ljava/lang/Object;)Ljava/lang/Object;", shift = At.Shift.AFTER))
    private void aervium$meshReplaced(CallbackInfoReturnable<?> callback) {
        aervium$meshVersion = SectionDrawRecord.nextMeshVersion();
    }

    @Inject(method = "reset", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/atomic/AtomicReference;getAndSet(Ljava/lang/Object;)Ljava/lang/Object;", shift = At.Shift.AFTER))
    private void aervium$meshCleared(CallbackInfo callback) {
        aervium$meshVersion = SectionDrawRecord.nextMeshVersion();
    }
}
