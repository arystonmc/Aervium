package com.aryston.kinetic.mixin;

import com.aryston.kinetic.client.chunk.DirtySectionIndex;
import com.aryston.kinetic.client.chunk.DirtySectionMirror;
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
    private boolean[] kinetic$dirty = new boolean[0];

    @Unique
    private int kinetic$gridXZ;

    @Unique
    private int kinetic$gridY;

    @Unique
    private int kinetic$minY;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void kinetic$attachMirror(LevelHeightAccessor levelHeightAccessor, int renderDistance, CallbackInfo callback) {
        boolean[] dirty = new boolean[storage.size()];
        int[] index = new int[1];
        storage.forEach(state -> ((DirtySectionMirror) state).kinetic$attach(dirty, index[0]++));
        kinetic$gridXZ = storage.radius() * 2 + 1;
        kinetic$gridY = storage.height();
        kinetic$minY = storage.minY();
        kinetic$dirty = dirty;
    }

    @Override
    public boolean kinetic$mayBeDirty(long sectionNode) {
        int y = SectionPos.y(sectionNode) - kinetic$minY;
        if (y < 0 || y >= kinetic$gridY) {
            return false;
        }
        int x = Math.floorMod(SectionPos.x(sectionNode), kinetic$gridXZ);
        int z = Math.floorMod(SectionPos.z(sectionNode), kinetic$gridXZ);
        return kinetic$dirty[(z * kinetic$gridY + y) * kinetic$gridXZ + x];
    }
}
