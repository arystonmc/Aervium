package com.aryston.aervium.mixin;

import com.aryston.aervium.world.spawn.SpawnPregeneration;
import com.aryston.aervium.world.spawn.SpawnPregenerationHolder;
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
    private @Nullable SpawnPregeneration aervium$pregeneration;

    @Override
    public @Nullable SpawnPregeneration aervium$startPregeneration(ServerLevel level, ChunkPos center) {
        aervium$pregeneration = SpawnPregeneration.start(server, nameAndId, level, center);
        return aervium$pregeneration;
    }

    @Inject(method = "keepAlive", at = @At("HEAD"))
    private void aervium$keepPregeneratedChunks(CallbackInfo callback) {
        if (aervium$pregeneration != null) {
            aervium$pregeneration.keepAlive();
        }
    }

    @Inject(method = "spawnPlayer", at = @At("RETURN"))
    private void aervium$releasePregeneratedChunks(CallbackInfoReturnable<?> callback) {
        aervium$pregeneration = null;
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void aervium$cancelPregeneration(CallbackInfo callback) {
        aervium$pregeneration = null;
    }
}
