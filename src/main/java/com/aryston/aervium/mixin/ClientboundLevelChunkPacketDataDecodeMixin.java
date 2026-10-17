package com.aryston.aervium.mixin;

import com.aryston.aervium.client.chunk.DecodedChunk;
import com.aryston.aervium.client.chunk.DecodedChunkHolder;
import java.util.concurrent.CompletableFuture;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ClientboundLevelChunkPacketData.class)
abstract class ClientboundLevelChunkPacketDataDecodeMixin implements DecodedChunkHolder {
    @Unique
    private volatile @Nullable CompletableFuture<DecodedChunk> aervium$decoded;

    @Override
    public @Nullable CompletableFuture<DecodedChunk> aervium$decoded() {
        return aervium$decoded;
    }

    @Override
    public void aervium$setDecoded(CompletableFuture<DecodedChunk> decoded) {
        aervium$decoded = decoded;
    }
}
