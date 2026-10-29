package com.aryston.kinetic.mixin;

import com.aryston.kinetic.world.worldgen.ParallelFeatures;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import java.util.concurrent.CompletableFuture;
import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatusTasks;
import net.minecraft.world.level.chunk.status.ChunkStep;
import net.minecraft.world.level.chunk.status.WorldGenContext;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ChunkStatusTasks.class)
abstract class ChunkStatusTasksFeaturesMixin {
    @WrapMethod(method = "generateFeatures")
    private static CompletableFuture<ChunkAccess> kinetic$placeInParallel(WorldGenContext context, ChunkStep step,
            StaticCache2D<GenerationChunkHolder> chunks, ChunkAccess chunk, Operation<CompletableFuture<ChunkAccess>> original) {
        return ParallelFeatures.place(chunk.getPos(), () -> original.call(context, step, chunks, chunk));
    }
}
