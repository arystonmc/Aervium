package com.aryston.kinetic.mixin;

import com.aryston.kinetic.client.gpu.MainPassTimings;
import com.mojang.renderpearl.api.commands.RenderPass;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
abstract class LevelRendererMainPassTimingMixin {
    @Unique
    private static final String RENDER_OIT = "Lnet/minecraft/client/renderer/chunk/ChunkSectionsToRender;renderOit(Lcom/mojang/renderpearl/api/textures/GpuSampler;Lnet/minecraft/client/renderer/oit/OitStage;Lnet/minecraft/client/renderer/oit/OitRenderPassProvider$Parameters;Lcom/mojang/renderpearl/api/textures/GpuTextureView;Lcom/mojang/renderpearl/api/textures/GpuTextureView;)V";

    @Inject(method = "executeSolid", at = @At("HEAD"), require = 0)
    private void kinetic$opaqueTerrainStarted(
        ChunkSectionsToRender sections, FeatureRenderDispatcher.PreparedFrame featureFrame, RenderPass renderPass, CallbackInfo callback
    ) {
        MainPassTimings.next(MainPassTimings.OPAQUE_TERRAIN, renderPass);
    }

    @Inject(
        method = "executeSolid",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/chunk/ChunkSectionsToRender;renderGroup(Lnet/minecraft/client/renderer/chunk/ChunkSectionLayerGroup;Lcom/mojang/renderpearl/api/commands/RenderPass;Lcom/mojang/renderpearl/api/textures/GpuSampler;Lcom/mojang/renderpearl/api/textures/GpuTextureView;Z)V",
            shift = At.Shift.AFTER
        ),
        require = 0
    )
    private void kinetic$solidFeaturesStarted(
        ChunkSectionsToRender sections, FeatureRenderDispatcher.PreparedFrame featureFrame, RenderPass renderPass, CallbackInfo callback
    ) {
        MainPassTimings.next(MainPassTimings.SOLID_FEATURES, renderPass);
    }

    @Inject(method = "executeSolid", at = @At("RETURN"), require = 0)
    private void kinetic$solidFinished(
        ChunkSectionsToRender sections, FeatureRenderDispatcher.PreparedFrame featureFrame, RenderPass renderPass, CallbackInfo callback
    ) {
        MainPassTimings.finish(renderPass);
    }

    @Inject(method = "executeClassicTransparency", at = @At("HEAD"), require = 0)
    private void kinetic$translucentFeaturesStarted(
        ChunkSectionsToRender sections, FeatureRenderDispatcher.PreparedFrame featureFrame, RenderPass renderPass, CallbackInfo callback
    ) {
        MainPassTimings.next(MainPassTimings.TRANSLUCENT_FEATURES, renderPass);
    }

    @Inject(
        method = "executeClassicTransparency",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/chunk/ChunkSectionsToRender;renderGroup(Lnet/minecraft/client/renderer/chunk/ChunkSectionLayerGroup;Lcom/mojang/renderpearl/api/commands/RenderPass;Lcom/mojang/renderpearl/api/textures/GpuSampler;Lcom/mojang/renderpearl/api/textures/GpuTextureView;Z)V"
        ),
        require = 0
    )
    private void kinetic$translucentTerrainStarted(
        ChunkSectionsToRender sections, FeatureRenderDispatcher.PreparedFrame featureFrame, RenderPass renderPass, CallbackInfo callback
    ) {
        MainPassTimings.next(MainPassTimings.TRANSLUCENT_TERRAIN, renderPass);
    }

    @Inject(
        method = "executeClassicTransparency",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher$PreparedFrame;executeTranslucentAfterTerrain(Lcom/mojang/renderpearl/api/commands/RenderPass;)V"
        ),
        require = 0
    )
    private void kinetic$particlesStarted(
        ChunkSectionsToRender sections, FeatureRenderDispatcher.PreparedFrame featureFrame, RenderPass renderPass, CallbackInfo callback
    ) {
        MainPassTimings.next(MainPassTimings.PARTICLES, renderPass);
    }

    @Inject(
        method = "executeClassicTransparency",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher$PreparedFrame;executeTranslucentAfterTerrain(Lcom/mojang/renderpearl/api/commands/RenderPass;)V",
            shift = At.Shift.AFTER
        ),
        require = 0
    )
    private void kinetic$skyEffectsStarted(
        ChunkSectionsToRender sections, FeatureRenderDispatcher.PreparedFrame featureFrame, RenderPass renderPass, CallbackInfo callback
    ) {
        MainPassTimings.next(MainPassTimings.CLOUDS_WEATHER_BORDER, renderPass);
    }

    @Inject(method = "executeClassicTransparency", at = @At("RETURN"), require = 0)
    private void kinetic$transparencyFinished(
        ChunkSectionsToRender sections, FeatureRenderDispatcher.PreparedFrame featureFrame, RenderPass renderPass, CallbackInfo callback
    ) {
        MainPassTimings.finish(renderPass);
    }

    @Inject(method = "executeOit", at = @At("HEAD"), require = 0)
    private void kinetic$transparencyStarted(ChunkSectionsToRender sections, FeatureRenderDispatcher.PreparedFrame featureFrame, CallbackInfo callback) {
        MainPassTimings.transparencyStarted();
    }

    @Inject(method = "executeOit", at = @At("RETURN"), require = 0)
    private void kinetic$transparencyFinished(ChunkSectionsToRender sections, FeatureRenderDispatcher.PreparedFrame featureFrame, CallbackInfo callback) {
        MainPassTimings.transparencyFinished();
    }

    @Inject(method = "executeOit", at = @At(value = "INVOKE", target = RENDER_OIT), require = 0)
    private void kinetic$transparencyTerrainStarted(ChunkSectionsToRender sections, FeatureRenderDispatcher.PreparedFrame featureFrame, CallbackInfo callback) {
        MainPassTimings.transparencyTerrainStarted();
    }

    @Inject(method = "executeOit", at = @At(value = "INVOKE", target = RENDER_OIT, shift = At.Shift.AFTER), require = 0)
    private void kinetic$transparencyTerrainFinished(ChunkSectionsToRender sections, FeatureRenderDispatcher.PreparedFrame featureFrame, CallbackInfo callback) {
        MainPassTimings.transparencyTerrainFinished();
    }
}
