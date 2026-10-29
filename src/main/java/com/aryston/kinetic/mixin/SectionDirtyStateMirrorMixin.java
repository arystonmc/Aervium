package com.aryston.kinetic.mixin;

import com.aryston.kinetic.client.chunk.DirtySectionMirror;
import net.minecraft.client.SectionUpdateTracker;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SectionUpdateTracker.SectionDirtyState.class)
abstract class SectionDirtyStateMirrorMixin implements DirtySectionMirror {
    @Shadow
    private boolean isDirty;

    @Unique
    private boolean @Nullable [] kinetic$dirty;

    @Unique
    private int kinetic$index;

    @Override
    public void kinetic$attach(boolean[] dirty, int index) {
        kinetic$dirty = dirty;
        kinetic$index = index;
        dirty[index] = isDirty;
    }

    @Inject(method = {"setDirty", "setNotDirty", "setSectionNode"}, at = @At("RETURN"))
    private void kinetic$mirrorDirty(CallbackInfo callback) {
        boolean[] dirty = kinetic$dirty;
        if (dirty != null) {
            dirty[kinetic$index] = isDirty;
        }
    }
}
