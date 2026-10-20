package com.aryston.aervium.mixin;

import com.aryston.aervium.client.lod.DistantTerrainRenderer;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.renderpearl.api.commands.RenderPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.aryston.helion.render.geometry.OpaqueGeometryStage")
abstract class HelionDistantTerrainMixin {
    @Inject(
        method = "render(Lcom/mojang/blaze3d/pipeline/RenderTarget;Ljava/util/Optional;)V",
        at = @At(
            value = "INVOKE",
            target = "Lcom/aryston/helion/render/geometry/TerrainSource;renderOpaque(Lcom/mojang/renderpearl/api/commands/RenderPass;)V",
            shift = At.Shift.AFTER
        ),
        require = 0
    )
    private void aervium$drawDistantTerrain(CallbackInfo callback, @Local RenderPass pass) {
        DistantTerrainRenderer.drawOpaque(pass);
    }

    @Inject(
        method = "renderGeometry(Lcom/mojang/blaze3d/pipeline/RenderTarget;Lcom/aryston/helion/render/geometry/GeometryBuffer$Targets;)V",
        at = @At(
            value = "INVOKE",
            target = "Lcom/aryston/helion/render/geometry/TerrainSource;renderOpaqueGeometry(Lcom/mojang/renderpearl/api/commands/RenderPass;)V",
            shift = At.Shift.AFTER
        ),
        require = 0
    )
    private void aervium$drawDistantTerrainGeometry(CallbackInfo callback, @Local RenderPass pass) {
        DistantTerrainRenderer.drawGeometry(pass);
    }
}
