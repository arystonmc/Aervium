package com.aryston.kinetic.mixin;

import com.aryston.kinetic.client.chunk.MeshWorkers;
import net.minecraft.TracingExecutor;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(LevelRenderer.class)
abstract class LevelRendererMeshWorkersMixin {
    @ModifyArg(
        method = "invalidateCompiledGeometry",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher;<init>(Lnet/minecraft/TracingExecutor;Lnet/minecraft/client/renderer/RenderBuffers;Lnet/minecraft/client/renderer/chunk/SectionCompiler;Ljava/util/function/Consumer;)V"
        ),
        index = 0
    )
    private TracingExecutor kinetic$useMeshWorkers(TracingExecutor executor) {
        return MeshWorkers.executor();
    }
}
