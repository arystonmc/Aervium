package com.aryston.aervium.mixin;

import com.aryston.aervium.client.chunk.LeafCulling;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ModelBlockRenderer.class)
abstract class ModelBlockRendererLeafCullingMixin {
    @Shadow
    @Final
    private boolean cull;

    @Inject(
        method = "shouldRenderFace(Lnet/minecraft/client/renderer/block/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;Lnet/minecraft/core/BlockPos;)Z",
        at = @At("HEAD"),
        cancellable = true
    )
    private void aervium$cullBuriedLeaves(
        BlockAndTintGetter level, BlockPos pos, BlockState state, Direction direction, BlockPos neighborPos,
        CallbackInfoReturnable<Boolean> callback
    ) {
        if (cull && LeafCulling.enabled() && LeafCulling.hidden(level, state, direction, neighborPos)) {
            callback.setReturnValue(false);
        }
    }
}
