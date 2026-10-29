package com.aryston.kinetic.mixin;

import com.aryston.kinetic.client.lod.DistantTerrain;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SectionOcclusionGraph.class)
abstract class SectionOcclusionGraphDistantTerrainMixin {
    @ModifyExpressionValue(method = "getRelativeFrom", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ViewArea;getViewDistance()I"))
    private int kinetic$reachTheGroundFromAbove(int viewDistance) {
        return DistantTerrain.drawing() ? Integer.MAX_VALUE : viewDistance;
    }
}
