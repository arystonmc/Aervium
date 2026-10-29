package com.aryston.kinetic.mixin;

import com.aryston.kinetic.world.parity.WorldgenCheck;
import java.util.concurrent.CompletableFuture;
import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatusTasks;
import net.minecraft.world.level.chunk.status.ChunkStep;
import net.minecraft.world.level.chunk.status.WorldGenContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkStatusTasks.class)
abstract class ChunkStatusTasksTerrainMixin {
    @Inject(method = "buildTerrain", at = @At("RETURN"), cancellable = true)
    private static void kinetic$terrainBuilt(WorldGenContext context, ChunkStep step, StaticCache2D<GenerationChunkHolder> chunks,
                                             ChunkAccess chunk, CallbackInfoReturnable<CompletableFuture<ChunkAccess>> callback) {
        if (WorldgenCheck.recording()) {
            callback.setReturnValue(callback.getReturnValue().thenApply(WorldgenCheck::terrainBuilt));
        }
    }
}
