package com.aryston.aervium.mixin;

import com.aryston.aervium.client.chunk.DirtySectionIndex;
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
    private SectionUpdateTracker.@Nullable SectionDirtyState aervium$skipCleanSections(SectionUpdateTracker tracker, long sectionNode) {
        return ((DirtySectionIndex) tracker).aervium$mayBeDirty(sectionNode) ? tracker.getDirtyState(sectionNode) : null;
    }
}
