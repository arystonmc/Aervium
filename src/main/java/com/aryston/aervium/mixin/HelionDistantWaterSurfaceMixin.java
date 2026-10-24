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
@Mixin(targets = "com.aryston.helion.render.water.WaterSurfaceStage")
abstract class HelionDistantWaterSurfaceMixin {
    @Inject(
        method = "render",
        at = @At(value = "INVOKE", target = "Lcom/aryston/helion/render/geometry/TerrainSource;renderWaterSurface", shift = At.Shift.AFTER),
        require = 0
    )
    private void aervium$drawDistantWaterSurface(CallbackInfo callback, @Local RenderPass pass) {
        DistantTerrainRenderer.drawWaterSurface(pass);
    }
}
