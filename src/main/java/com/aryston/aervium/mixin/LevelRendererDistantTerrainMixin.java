package com.aryston.aervium.mixin;

import com.aryston.aervium.client.lod.DistantTerrainRenderer;
import com.mojang.renderpearl.api.commands.RenderPass;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
abstract class LevelRendererDistantTerrainMixin {
    @Inject(method = "executeSolid", at = @At("RETURN"))
    private void aervium$drawDistantTerrain(
        ChunkSectionsToRender chunkSectionsToRender, FeatureRenderDispatcher.PreparedFrame featureFrame, RenderPass renderPass, CallbackInfo callback
    ) {
        DistantTerrainRenderer.draw(renderPass);
    }
}
