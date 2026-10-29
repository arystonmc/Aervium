package com.aryston.kinetic.mixin;

import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Level.class)
public interface LevelRandomAccessor {
    @Accessor("randValue")
    void kinetic$setRandValue(int value);
}
