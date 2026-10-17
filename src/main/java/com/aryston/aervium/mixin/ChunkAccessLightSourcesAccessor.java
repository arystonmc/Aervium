package com.aryston.aervium.mixin;

import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.lighting.ChunkSkyLightSources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChunkAccess.class)
public interface ChunkAccessLightSourcesAccessor {
    @Accessor("skyLightSources")
    void aervium$setSkyLightSources(ChunkSkyLightSources skyLightSources);
}
