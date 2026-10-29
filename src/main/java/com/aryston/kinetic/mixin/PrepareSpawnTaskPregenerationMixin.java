package com.aryston.kinetic.mixin;

import com.aryston.kinetic.world.spawn.SpawnPregeneration;
import com.aryston.kinetic.world.spawn.SpawnPregenerationHolder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.config.PrepareSpawnTask;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.level.ChunkPos;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PrepareSpawnTask.class)
abstract class PrepareSpawnTaskPregenerationMixin implements SpawnPregenerationHolder {
    @Shadow
    @Final
    private MinecraftServer server;

    @Shadow
    @Final
    private NameAndId nameAndId;

    @Unique
    private @Nullable SpawnPregeneration kinetic$pregeneration;

    @Override
    public @Nullable SpawnPregeneration kinetic$startPregeneration(ServerLevel level, ChunkPos center) {
        kinetic$pregeneration = SpawnPregeneration.start(server, nameAndId, level, center);
        return kinetic$pregeneration;
    }

    @Inject(method = "keepAlive", at = @At("HEAD"))
    private void kinetic$keepPregeneratedChunks(CallbackInfo callback) {
        if (kinetic$pregeneration != null) {
            kinetic$pregeneration.keepAlive();
        }
    }

    @Inject(method = "spawnPlayer", at = @At("RETURN"))
    private void kinetic$releasePregeneratedChunks(CallbackInfoReturnable<?> callback) {
        kinetic$pregeneration = null;
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void kinetic$cancelPregeneration(CallbackInfo callback) {
        kinetic$pregeneration = null;
    }
}
