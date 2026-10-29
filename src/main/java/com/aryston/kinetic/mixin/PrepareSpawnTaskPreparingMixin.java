package com.aryston.kinetic.mixin;

import com.aryston.kinetic.world.spawn.SpawnPregeneration;
import com.aryston.kinetic.world.spawn.SpawnPregenerationHolder;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.LevelLoadListener;
import net.minecraft.server.network.config.PrepareSpawnTask;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.server.network.config.PrepareSpawnTask$Preparing")
abstract class PrepareSpawnTaskPreparingMixin {
    private static final String TICK = "tick";
    private static final String LISTENER = "Lnet/minecraft/server/level/progress/LevelLoadListener;";
    private static final String STAGE = "Lnet/minecraft/server/level/progress/LevelLoadListener$Stage;";

    @Shadow
    @Final
    private ServerLevel spawnLevel;

    @Shadow
    @Final
    private CompletableFuture<Vec3> spawnPosition;

    @Shadow
    @Final
    PrepareSpawnTask this$0;

    @Unique
    private @Nullable SpawnPregeneration kinetic$pregeneration;

    @WrapOperation(method = TICK, at = @At(value = "INVOKE", target = LISTENER + "start(" + STAGE + "I)V"))
    private void kinetic$startPregeneration(LevelLoadListener listener, LevelLoadListener.Stage stage, int totalChunks, Operation<Void> original) {
        ChunkPos center = ChunkPos.containing(BlockPos.containing(spawnPosition.join()));
        kinetic$pregeneration = ((SpawnPregenerationHolder) this$0).kinetic$startPregeneration(spawnLevel, center);
        original.call(listener, stage, kinetic$pregeneration == null ? totalChunks : kinetic$pregeneration.totalChunks());
    }

    @WrapOperation(method = TICK, at = @At(value = "INVOKE", target = LISTENER + "update(" + STAGE + "II)V"))
    private void kinetic$reportPregeneration(LevelLoadListener listener, LevelLoadListener.Stage stage, int currentChunks, int totalChunks,
        Operation<Void> original) {
        if (kinetic$pregeneration == null) {
            original.call(listener, stage, currentChunks, totalChunks);
        } else {
            original.call(listener, stage, kinetic$pregeneration.readyChunks(), kinetic$pregeneration.totalChunks());
        }
    }

    @WrapOperation(method = TICK, at = @At(value = "INVOKE", target = "Ljava/util/concurrent/CompletableFuture;isDone()Z", ordinal = 1))
    private boolean kinetic$waitForPregeneration(CompletableFuture<?> playerChunks, Operation<Boolean> original) {
        boolean loaded = original.call(playerChunks);
        return kinetic$pregeneration == null ? loaded : loaded && kinetic$pregeneration.isDone();
    }
}
