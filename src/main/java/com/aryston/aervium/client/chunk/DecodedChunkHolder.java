package com.aryston.aervium.client.chunk;

import java.util.concurrent.CompletableFuture;
import org.jspecify.annotations.Nullable;

public interface DecodedChunkHolder {
    @Nullable CompletableFuture<DecodedChunk> aervium$decoded();

    void aervium$setDecoded(CompletableFuture<DecodedChunk> decoded);
}
