package com.aryston.aervium.mixin;

import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.util.ThreadingDetector;
import net.minecraft.world.level.chunk.PalettedContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PalettedContainer.class)
abstract class PalettedContainerMixin {
    @Unique
    private static final String AERVIUM$CONTAINER_NAME = "PalettedContainer";

    @Unique
    private static final ThreadingDetector AERVIUM$SHARED_DETECTOR = new ThreadingDetector(AERVIUM$CONTAINER_NAME);

    @Unique
    private final AtomicBoolean aervium$inUse = new AtomicBoolean();

    @Redirect(method = "<init>*", at = @At(value = "NEW", target = "(Ljava/lang/String;)Lnet/minecraft/util/ThreadingDetector;"))
    private ThreadingDetector aervium$shareDetector(String name) {
        return AERVIUM$SHARED_DETECTOR;
    }

    @Inject(method = "acquire", at = @At("HEAD"), cancellable = true)
    private void aervium$acquire(CallbackInfo callback) {
        if (!aervium$inUse.compareAndSet(false, true)) {
            throw ThreadingDetector.makeThreadingException(AERVIUM$CONTAINER_NAME, null);
        }
        callback.cancel();
    }

    @Inject(method = "release", at = @At("HEAD"), cancellable = true)
    private void aervium$release(CallbackInfo callback) {
        aervium$inUse.set(false);
        callback.cancel();
    }
}
