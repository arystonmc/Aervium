package com.aryston.kinetic.mixin;

import com.aryston.kinetic.client.chunk.DecodedChunk;
import com.aryston.kinetic.client.chunk.DecodedChunkHolder;
import java.util.concurrent.CompletableFuture;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ClientboundLevelChunkPacketData.class)
abstract class ClientboundLevelChunkPacketDataDecodeMixin implements DecodedChunkHolder {
    @Unique
    private volatile @Nullable CompletableFuture<DecodedChunk> kinetic$decoded;

    @Override
    public @Nullable CompletableFuture<DecodedChunk> kinetic$decoded() {
        return kinetic$decoded;
    }

    @Override
    public void kinetic$setDecoded(CompletableFuture<DecodedChunk> decoded) {
        kinetic$decoded = decoded;
    }
}
