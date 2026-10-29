package com.aryston.kinetic.mixin;

import com.aryston.kinetic.client.chunk.DirectDraws;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import java.util.List;
import net.minecraft.client.renderer.DynamicGpuData;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelRenderer.class)
abstract class LevelRendererDirectDrawMixin {
    @Shadow
    @Final
    LevelRenderState levelRenderState;

    @Inject(method = "prepareChunkRendersIndirect", at = @At("HEAD"))
    private void kinetic$beginIndirectPass(Matrix4fc modelView, boolean respectTranslucentOrder, CallbackInfoReturnable<ChunkSectionsToRender> callback) {
        DirectDraws.beginPass(true, kinetic$cameraPass(modelView));
    }

    @Inject(method = "prepareChunkRenders", at = @At("HEAD"))
    private void kinetic$beginSeparatePass(Matrix4fc modelView, boolean respectTranslucentOrder, CallbackInfoReturnable<ChunkSectionsToRender> callback) {
        DirectDraws.beginPass(false, null);
    }

    @Unique
    private @Nullable CameraRenderState kinetic$cameraPass(Matrix4fc modelView) {
        CameraRenderState camera = levelRenderState.cameraRenderState;
        boolean usable = camera.initialized && !camera.isPanoramicMode && !camera.isFrustumCaptured;
        return usable && modelView.equals(camera.viewRotationMatrix) ? camera : null;
    }

    @Redirect(
        method = "prepareChunkRendersIndirect",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/DynamicGpuData;writeChunkSectionCommands(Ljava/util/List;)[Lcom/mojang/renderpearl/api/buffers/GpuBufferSlice;"
        )
    )
    private GpuBufferSlice[] kinetic$writePackedCommands(DynamicGpuData data, List<List<DynamicGpuData.IndexedDraw>> draws) {
        return DirectDraws.writeCommands(data, draws);
    }

    @Redirect(
        method = "prepareChunkRendersIndirect",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/DynamicGpuData;writeChunkSectionsInstanced(Ljava/util/List;)Lcom/mojang/renderpearl/api/buffers/GpuBufferSlice;"
        )
    )
    private GpuBufferSlice kinetic$writePackedSections(DynamicGpuData data, List<DynamicGpuData.ChunkSectionInfo> sections) {
        return DirectDraws.writeSectionInfos(data, sections);
    }
}
