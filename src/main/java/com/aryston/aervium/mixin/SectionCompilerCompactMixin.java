package com.aryston.aervium.mixin;

import com.aryston.aervium.client.chunk.CompactMeshUpload;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SectionCompiler.class)
abstract class SectionCompilerCompactMixin {
    @WrapOperation(
        method = "getOrBeginLayer",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/ChunkSectionLayer;vertexFormat()Lcom/mojang/renderpearl/api/vertex/VertexFormat;")
    )
    private VertexFormat aervium$buildInBlockFormat(ChunkSectionLayer layer, Operation<VertexFormat> original) {
        return CompactMeshUpload.buildFormat(original.call(layer));
    }
}
