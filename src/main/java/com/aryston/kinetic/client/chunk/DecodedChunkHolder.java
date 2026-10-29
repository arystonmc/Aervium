package com.aryston.kinetic.client.chunk;

import java.util.concurrent.CompletableFuture;
import org.jspecify.annotations.Nullable;

public interface DecodedChunkHolder {
    @Nullable CompletableFuture<DecodedChunk> kinetic$decoded();

    void kinetic$setDecoded(CompletableFuture<DecodedChunk> decoded);
}
