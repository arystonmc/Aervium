package com.aryston.aervium.mixin;

import com.aryston.aervium.client.gpu.GpuCulling;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ChunkSectionsToRender.DrawIndirect.class)
abstract class ChunkSectionsToRenderIndirectCountMixin {
    @Redirect(
        method = "render",
        at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/api/commands/RenderPass;drawIndexedIndirect(Lcom/mojang/renderpearl/api/buffers/GpuBufferSlice;I)V")
    )
    private void aervium$drawCountedCommands(RenderPass pass, GpuBufferSlice commands, int drawCount) {
        GpuCulling.drawIndexedIndirect(pass, commands, drawCount);
    }
}
