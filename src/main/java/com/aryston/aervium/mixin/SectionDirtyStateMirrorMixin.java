package com.aryston.aervium.mixin;

import com.aryston.aervium.client.chunk.DirtySectionMirror;
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
    private boolean @Nullable [] aervium$dirty;

    @Unique
    private int aervium$index;

    @Override
    public void aervium$attach(boolean[] dirty, int index) {
        aervium$dirty = dirty;
        aervium$index = index;
        dirty[index] = isDirty;
    }

    @Inject(method = {"setDirty", "setNotDirty", "setSectionNode"}, at = @At("RETURN"))
    private void aervium$mirrorDirty(CallbackInfo callback) {
        boolean[] dirty = aervium$dirty;
        if (dirty != null) {
            dirty[aervium$index] = isDirty;
        }
    }
}
