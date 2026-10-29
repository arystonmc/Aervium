package com.aryston.kinetic.mixin;

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
    private static final String KINETIC$CONTAINER_NAME = "PalettedContainer";

    @Unique
    private static final ThreadingDetector KINETIC$SHARED_DETECTOR = new ThreadingDetector(KINETIC$CONTAINER_NAME);

    @Unique
    private final AtomicBoolean kinetic$inUse = new AtomicBoolean();

    @Redirect(method = "<init>*", at = @At(value = "NEW", target = "(Ljava/lang/String;)Lnet/minecraft/util/ThreadingDetector;"))
    private ThreadingDetector kinetic$shareDetector(String name) {
        return KINETIC$SHARED_DETECTOR;
    }

    @Inject(method = "acquire", at = @At("HEAD"), cancellable = true)
    private void kinetic$acquire(CallbackInfo callback) {
        if (!kinetic$inUse.compareAndSet(false, true)) {
            throw ThreadingDetector.makeThreadingException(KINETIC$CONTAINER_NAME, null);
        }
        callback.cancel();
    }

    @Inject(method = "release", at = @At("HEAD"), cancellable = true)
    private void kinetic$release(CallbackInfo callback) {
        kinetic$inUse.set(false);
        callback.cancel();
    }
}
