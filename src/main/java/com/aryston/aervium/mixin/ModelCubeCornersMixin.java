package com.aryston.aervium.mixin;

import com.aryston.aervium.client.model.CubeCorners;
import com.aryston.aervium.core.mesh.SharedCorners;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelPart.Cube.class)
abstract class ModelCubeCornersMixin {
    @Shadow
    @Final
    public ModelPart.Polygon[] polygons;

    @Unique
    private @Nullable SharedCorners aervium$corners;

    @Inject(
        method = "compile(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void aervium$compileSharedCorners(
        PoseStack.Pose pose, VertexConsumer builder, int lightCoords, int overlayCoords, int color, CallbackInfo callback
    ) {
        SharedCorners corners = aervium$corners;
        if (corners == null) {
            corners = CubeCorners.of(polygons);
            aervium$corners = corners;
        }
        CubeCorners.compile(polygons, corners, pose, builder, lightCoords, overlayCoords, color);
        callback.cancel();
    }
}
