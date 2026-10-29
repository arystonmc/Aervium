package com.aryston.kinetic.mixin;

import com.aryston.kinetic.client.chunk.FaceCulling;
import com.aryston.kinetic.client.chunk.FaceRangesHolder;
import com.aryston.kinetic.core.mesh.FaceRanges;
import net.minecraft.client.renderer.chunk.CompiledSectionMesh;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.client.renderer.chunk.TranslucencyPointOfView;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CompiledSectionMesh.class)
abstract class CompiledSectionMeshFacingMixin implements FaceRangesHolder {
    @Unique
    private FaceRanges @Nullable [] kinetic$faceRanges;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void kinetic$sortByFacing(TranslucencyPointOfView pointOfView, SectionCompiler.Results results, long compileTaskStartTimeNs, CallbackInfo callback) {
        kinetic$faceRanges = FaceCulling.sortLayers(results.renderedLayers);
    }

    @Override
    public @Nullable FaceRanges kinetic$faceRanges(int layer) {
        FaceRanges[] ranges = kinetic$faceRanges;
        return ranges == null ? null : ranges[layer];
    }
}
