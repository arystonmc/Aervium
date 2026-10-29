package com.aryston.kinetic.mixin;

import com.aryston.kinetic.client.chunk.DirtySectionIndex;
import net.minecraft.client.SectionUpdateTracker;
import net.minecraft.client.renderer.extract.LevelExtractor;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LevelExtractor.class)
abstract class LevelExtractorDirtySectionsMixin {
    @Redirect(
        method = "extract",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/SectionUpdateTracker;getDirtyState(J)Lnet/minecraft/client/SectionUpdateTracker$SectionDirtyState;"
        )
    )
    private SectionUpdateTracker.@Nullable SectionDirtyState kinetic$skipCleanSections(SectionUpdateTracker tracker, long sectionNode) {
        return ((DirtySectionIndex) tracker).kinetic$mayBeDirty(sectionNode) ? tracker.getDirtyState(sectionNode) : null;
    }
}
