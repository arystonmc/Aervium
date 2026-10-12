package com.aryston.aervium.mixin;

import net.minecraft.client.renderer.chunk.CompiledSectionMesh;
import net.minecraft.client.renderer.chunk.VisibilitySet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CompiledSectionMesh.class)
public interface CompiledSectionMeshVisibilityAccessor {
    @Accessor("visibilitySet")
    VisibilitySet aervium$visibilitySet();
}
