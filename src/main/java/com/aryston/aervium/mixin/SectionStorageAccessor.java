package com.aryston.aervium.mixin;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import java.util.Optional;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.storage.SectionStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SectionStorage.class)
public interface SectionStorageAccessor<R> {
    @Accessor("storage")
    Long2ObjectMap<Optional<R>> aervium$storage();

    @Accessor("levelHeightAccessor")
    LevelHeightAccessor aervium$levelHeightAccessor();
}
