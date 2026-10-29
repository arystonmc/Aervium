package com.aryston.kinetic.mixin;

import java.util.List;
import java.util.Map;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.capabilities.EntityCapability;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityCapability.class)
public interface EntityCapabilityAccessor {
    @Accessor(value = "providers", remap = false)
    Map<EntityType<?>, List<?>> kinetic$providers();
}
