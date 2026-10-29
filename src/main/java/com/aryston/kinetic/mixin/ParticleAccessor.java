package com.aryston.kinetic.mixin;

import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Particle.class)
public interface ParticleAccessor {
    @Accessor("xo")
    double kinetic$xo();

    @Accessor("yo")
    double kinetic$yo();

    @Accessor("zo")
    double kinetic$zo();
}
