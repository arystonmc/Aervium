package com.aryston.aervium.mixin;

import com.aryston.aervium.client.chunk.DirtySectionIndex;
import com.aryston.aervium.client.chunk.DirtySectionMirror;
import net.minecraft.client.RotatingSectionStorage;
import net.minecraft.client.SectionUpdateTracker;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LevelHeightAccessor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SectionUpdateTracker.class)
abstract class SectionUpdateTrackerMirrorMixin implements DirtySectionIndex {
    @Shadow
    @Final
    private RotatingSectionStorage<SectionUpdateTracker.SectionDirtyState> storage;

    @Unique
    private boolean[] aervium$dirty = new boolean[0];

    @Unique
    private int aervium$gridXZ;

    @Unique
    private int aervium$gridY;

    @Unique
    private int aervium$minY;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void aervium$attachMirror(LevelHeightAccessor levelHeightAccessor, int renderDistance, CallbackInfo callback) {
        boolean[] dirty = new boolean[storage.size()];
        int[] index = new int[1];
        storage.forEach(state -> ((DirtySectionMirror) state).aervium$attach(dirty, index[0]++));
        aervium$gridXZ = storage.radius() * 2 + 1;
        aervium$gridY = storage.height();
        aervium$minY = storage.minY();
        aervium$dirty = dirty;
    }

    @Override
    public boolean aervium$mayBeDirty(long sectionNode) {
        int y = SectionPos.y(sectionNode) - aervium$minY;
        if (y < 0 || y >= aervium$gridY) {
            return false;
        }
        int x = Math.floorMod(SectionPos.x(sectionNode), aervium$gridXZ);
        int z = Math.floorMod(SectionPos.z(sectionNode), aervium$gridXZ);
        return aervium$dirty[(z * aervium$gridY + y) * aervium$gridXZ + x];
    }
}
