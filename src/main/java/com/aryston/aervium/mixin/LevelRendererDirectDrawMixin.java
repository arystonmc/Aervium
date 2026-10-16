package com.aryston.aervium.mixin;

import com.aryston.aervium.client.chunk.DirectDraws;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import java.util.List;
import net.minecraft.client.renderer.DynamicGpuData;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelRenderer.class)
abstract class LevelRendererDirectDrawMixin {
    @Inject(method = "prepareChunkRendersIndirect", at = @At("HEAD"))
    private void aervium$beginIndirectPass(Matrix4fc modelView, boolean respectTranslucentOrder, CallbackInfoReturnable<ChunkSectionsToRender> callback) {
        DirectDraws.beginPass(true);
    }

    @Inject(method = "prepareChunkRenders", at = @At("HEAD"))
    private void aervium$beginSeparatePass(Matrix4fc modelView, boolean respectTranslucentOrder, CallbackInfoReturnable<ChunkSectionsToRender> callback) {
        DirectDraws.beginPass(false);
    }

    @Redirect(
        method = "prepareChunkRendersIndirect",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/DynamicGpuData;writeChunkSectionCommands(Ljava/util/List;)[Lcom/mojang/renderpearl/api/buffers/GpuBufferSlice;"
        )
    )
    private GpuBufferSlice[] aervium$writePackedCommands(DynamicGpuData data, List<List<DynamicGpuData.IndexedDraw>> draws) {
        return DirectDraws.writeCommands(data, draws);
    }

    @Redirect(
        method = "prepareChunkRendersIndirect",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/DynamicGpuData;writeChunkSectionsInstanced(Ljava/util/List;)Lcom/mojang/renderpearl/api/buffers/GpuBufferSlice;"
        )
    )
    private GpuBufferSlice aervium$writePackedSections(DynamicGpuData data, List<DynamicGpuData.ChunkSectionInfo> sections) {
        return DirectDraws.writeSectionInfos(data, sections);
    }
}
