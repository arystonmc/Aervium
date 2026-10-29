package com.aryston.kinetic.world.spawn;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import org.jspecify.annotations.Nullable;

public interface SpawnPregenerationHolder {
    @Nullable SpawnPregeneration kinetic$startPregeneration(ServerLevel level, ChunkPos center);
}
