package com.aryston.kinetic.mixin;

import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(SectionRenderDispatcher.RenderSection.class)
public interface RenderSectionAccessor {
    @Invoker("releaseSectionMesh")
    void kinetic$releaseSectionMesh(SectionMesh mesh);
}
