package com.aryston.aervium.mixin;

import com.aryston.aervium.client.chunk.FaceCulling;
import com.aryston.aervium.client.chunk.FaceRangesHolder;
import com.aryston.aervium.core.mesh.FaceRanges;
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
    private FaceRanges @Nullable [] aervium$faceRanges;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void aervium$sortByFacing(TranslucencyPointOfView pointOfView, SectionCompiler.Results results, long compileTaskStartTimeNs, CallbackInfo callback) {
        aervium$faceRanges = FaceCulling.sortLayers(results.renderedLayers);
    }

    @Override
    public @Nullable FaceRanges aervium$faceRanges(int layer) {
        FaceRanges[] ranges = aervium$faceRanges;
        return ranges == null ? null : ranges[layer];
    }
}
