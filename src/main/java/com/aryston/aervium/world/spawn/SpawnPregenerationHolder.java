package com.aryston.aervium.world.spawn;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import org.jspecify.annotations.Nullable;

public interface SpawnPregenerationHolder {
    @Nullable SpawnPregeneration aervium$startPregeneration(ServerLevel level, ChunkPos center);
}
