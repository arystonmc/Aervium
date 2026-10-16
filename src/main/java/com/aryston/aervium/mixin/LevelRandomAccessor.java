package com.aryston.aervium.mixin;

import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Level.class)
public interface LevelRandomAccessor {
    @Accessor("randValue")
    void aervium$setRandValue(int value);
}
