package com.aryston.kinetic.mixin;

import com.aryston.kinetic.client.chunk.ChunkPacketDecoding;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ClientChunkCache.class)
abstract class ClientChunkCacheChunkDecodeMixin {
    @WrapOperation(
        method = "replaceWithPacketData",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/chunk/LevelChunk;replaceWithPacketData(IILnet/minecraft/network/protocol/game/ClientboundLevelChunkPacketData;)V",
            ordinal = 0
        )
    )
    private void kinetic$fillFreshChunk(LevelChunk chunk, int chunkX, int chunkZ, ClientboundLevelChunkPacketData data, Operation<Void> original) {
        ChunkPacketDecoding.replaceFresh(chunk, chunkX, chunkZ, data, original);
    }
}
