package com.aryston.aervium.mixin;

import com.aryston.aervium.client.chunk.CompactTerrainFormat;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderPipeline.Builder.class)
abstract class RenderPipelineBuilderCompactMixin {
    @Shadow
    private Optional<Identifier> location;

    @Inject(method = "build", at = @At("HEAD"))
    private void aervium$compactTerrainVertices(CallbackInfoReturnable<RenderPipeline> callback) {
        if (CompactTerrainFormat.appliesTo(location)) {
            CompactTerrainFormat.apply((RenderPipeline.Builder) (Object) this);
        }
    }
}
