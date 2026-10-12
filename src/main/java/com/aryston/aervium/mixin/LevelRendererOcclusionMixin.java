package com.aryston.aervium.mixin;

import com.aryston.aervium.client.culling.TerrainOcclusion;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.state.OptionsRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.util.Util;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelRenderer.class)
abstract class LevelRendererOcclusionMixin {
    @Shadow
    @Final
    LevelRenderState levelRenderState;

    @Shadow
    @Final
    private OptionsRenderState optionsRenderState;

    @Inject(method = {"prepareChunkRenders", "prepareChunkRendersIndirect"}, at = @At("HEAD"))
    private void aervium$beginTerrainPass(Matrix4fc modelView, boolean respectTranslucentOrder, CallbackInfoReturnable<ChunkSectionsToRender> callback) {
        TerrainOcclusion.beginTerrainPass(modelView, levelRenderState.cameraRenderState, Util.toMillis(optionsRenderState.chunkSectionFadeInTime));
    }

    @Inject(method = {"prepareChunkRenders", "prepareChunkRendersIndirect"}, at = @At("RETURN"))
    private void aervium$endTerrainPass(Matrix4fc modelView, boolean respectTranslucentOrder, CallbackInfoReturnable<ChunkSectionsToRender> callback) {
        TerrainOcclusion.endTerrainPass();
    }
}
