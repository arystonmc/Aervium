package com.aryston.aervium.mixin;

import com.aryston.aervium.client.chunk.FaceCulling;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelRenderer.class)
abstract class LevelRendererFaceCullingMixin {
    @Shadow
    @Final
    LevelRenderState levelRenderState;

    @Inject(method = {"prepareChunkRenders", "prepareChunkRendersIndirect"}, at = @At("HEAD"))
    private void aervium$beginFaceCulling(Matrix4fc modelView, boolean respectTranslucentOrder, CallbackInfoReturnable<ChunkSectionsToRender> callback) {
        FaceCulling.beginTerrainPass(modelView, levelRenderState.cameraRenderState);
    }

    @Inject(method = {"prepareChunkRenders", "prepareChunkRendersIndirect"}, at = @At("RETURN"))
    private void aervium$endFaceCulling(Matrix4fc modelView, boolean respectTranslucentOrder, CallbackInfoReturnable<ChunkSectionsToRender> callback) {
        FaceCulling.endTerrainPass();
    }
}
