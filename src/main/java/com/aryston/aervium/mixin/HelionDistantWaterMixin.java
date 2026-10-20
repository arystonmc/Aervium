package com.aryston.aervium.mixin;

import com.aryston.aervium.client.lod.DistantTerrainRenderer;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Optional;
import java.util.OptionalDouble;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.aryston.helion.render.geometry.TransparentGeometryStage")
abstract class HelionDistantWaterMixin {
    @Inject(method = "render(Lcom/mojang/blaze3d/pipeline/RenderTarget;ZLjava/util/Optional;)V", at = @At("HEAD"), require = 0)
    private void aervium$distantWater(RenderTarget target, boolean independent, Optional<?> fog, CallbackInfo callback) {
        try (var pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "Aervium distant water", target.getColorTextureView(),
            Optional.empty(), target.getDepthTextureView(), OptionalDouble.empty())) {
            DistantTerrainRenderer.drawWater(pass);
        }
    }
}
