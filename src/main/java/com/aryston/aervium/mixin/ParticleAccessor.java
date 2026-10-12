package com.aryston.aervium.mixin;

import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Particle.class)
public interface ParticleAccessor {
    @Accessor("xo")
    double aervium$xo();

    @Accessor("yo")
    double aervium$yo();

    @Accessor("zo")
    double aervium$zo();
}
