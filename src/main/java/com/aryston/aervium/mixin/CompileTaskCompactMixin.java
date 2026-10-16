package com.aryston.aervium.mixin;

import com.aryston.aervium.client.chunk.CompactMeshUpload;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.MeshData;
import java.nio.ByteBuffer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.CompiledSectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.client.renderer.chunk.SectionRenderDispatcher$RenderSection$CompileTask")
abstract class CompileTaskCompactMixin {
    @WrapOperation(
        method = "doTask",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher$RenderSection;addSectionBuffersToUberBuffer(Lnet/minecraft/client/renderer/chunk/ChunkSectionLayer;Lnet/minecraft/client/renderer/chunk/CompiledSectionMesh;Ljava/nio/ByteBuffer;Ljava/nio/ByteBuffer;)Z"
        )
    )
    private boolean aervium$uploadCompactVertices(
        SectionRenderDispatcher.RenderSection section,
        ChunkSectionLayer layer,
        CompiledSectionMesh mesh,
        ByteBuffer vertices,
        @Nullable ByteBuffer indices,
        Operation<Boolean> original,
        @Local MeshData meshData
    ) {
        return original.call(section, layer, mesh, CompactMeshUpload.uploadView(layer, meshData, vertices), indices);
    }
}
