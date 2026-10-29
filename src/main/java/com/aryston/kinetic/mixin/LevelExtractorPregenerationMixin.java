package com.aryston.kinetic.mixin;

import com.aryston.kinetic.client.pregeneration.JoinPregeneration;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.SectionUpdateTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.extract.LevelExtractor;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LevelExtractor.class)
abstract class LevelExtractorPregenerationMixin {
    @Shadow
    private @Nullable SectionUpdateTracker sectionUpdateTracker;

    @WrapOperation(
        method = "extract",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;visibleSections()Lit/unimi/dsi/fastutil/objects/ObjectArrayList;")
    )
    private ObjectArrayList<SectionRenderDispatcher.RenderSection> kinetic$compileEveryDirection(LevelRenderer renderer,
        Operation<ObjectArrayList<SectionRenderDispatcher.RenderSection>> original) {
        return JoinPregeneration.sectionsToCompile(original.call(renderer), sectionUpdateTracker);
    }
}
