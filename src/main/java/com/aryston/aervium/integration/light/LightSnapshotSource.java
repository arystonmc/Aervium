package com.aryston.aervium.integration.light;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.world.level.chunk.DataLayer;

public interface LightSnapshotSource {
    Long2ObjectOpenHashMap<DataLayer> aervium$snapshot();
}
