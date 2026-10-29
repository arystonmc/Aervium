package com.aryston.kinetic.mixin;

import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.lighting.ChunkSkyLightSources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChunkAccess.class)
public interface ChunkAccessLightSourcesAccessor {
    @Accessor("skyLightSources")
    void kinetic$setSkyLightSources(ChunkSkyLightSources skyLightSources);
}
