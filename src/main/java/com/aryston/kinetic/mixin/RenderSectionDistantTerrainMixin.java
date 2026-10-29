package com.aryston.kinetic.mixin;

import com.aryston.kinetic.client.lod.DistantTerrainCoverage;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SectionRenderDispatcher.RenderSection.class)
abstract class RenderSectionDistantTerrainMixin {
    @Inject(method = "setSectionMesh", at = @At("RETURN"))
    private void kinetic$terrainUploaded(SectionMesh mesh, CallbackInfoReturnable<SectionMesh> callback) {
        DistantTerrainCoverage.changed(((SectionRenderDispatcher.RenderSection) (Object) this).getSectionNode(), true);
    }

    @Inject(method = "reset", at = @At("HEAD"))
    private void kinetic$terrainRemoved(CallbackInfo callback) {
        DistantTerrainCoverage.changed(((SectionRenderDispatcher.RenderSection) (Object) this).getSectionNode(), false);
    }
}
