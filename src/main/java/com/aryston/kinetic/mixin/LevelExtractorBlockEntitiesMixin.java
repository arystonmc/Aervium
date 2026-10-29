package com.aryston.kinetic.mixin;

import com.aryston.kinetic.client.chunk.BlockEntitySectionHolder;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.chunk.CompiledSectionMesh;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.extract.LevelExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LevelExtractor.class)
abstract class LevelExtractorBlockEntitiesMixin {
    @WrapOperation(method = "extractVisibleBlockEntities", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher$RenderSection;getSectionMesh()Lnet/minecraft/client/renderer/chunk/SectionMesh;"))
    private SectionMesh kinetic$skipSectionsWithoutBlockEntities(SectionRenderDispatcher.RenderSection section, Operation<SectionMesh> original) {
        return ((BlockEntitySectionHolder) section).kinetic$mayHaveBlockEntities() ? original.call(section) : CompiledSectionMesh.EMPTY;
    }
}
