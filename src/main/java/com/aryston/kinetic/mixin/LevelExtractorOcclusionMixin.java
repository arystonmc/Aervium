package com.aryston.kinetic.mixin;

import com.aryston.kinetic.client.culling.EntityOcclusion;
import com.aryston.kinetic.client.culling.TerrainOcclusion;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelExtractor.class)
abstract class LevelExtractorOcclusionMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    @Final
    private LevelRenderer levelRenderer;

    @Inject(method = "extractVisibleEntities", at = @At("HEAD"))
    private void kinetic$prepareCameraOcclusion(Camera camera, Frustum frustum, DeltaTracker deltaTracker, LevelRenderState output, CallbackInfo callback) {
        TerrainOcclusion.prepareCamera(
            levelRenderer.visibleSections(),
            output.cameraRenderState,
            Util.getMillis(),
            Util.toMillis(minecraft.options.chunkSectionFadeInTime().get())
        );
    }

    @Inject(method = "isEntityVisible", at = @At("RETURN"), cancellable = true)
    private void kinetic$hideOccludedEntity(
        Entity entity, Frustum frustum, double camX, double camY, double camZ, float partialTicks, long chunkFadeDuration,
        CallbackInfoReturnable<Boolean> callback
    ) {
        if (callback.getReturnValueZ() && EntityOcclusion.hidden(entity, partialTicks)) {
            callback.setReturnValue(false);
        }
    }

    @WrapOperation(
        method = "extractVisibleBlockEntities",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderDispatcher;tryExtractRenderState(Lnet/minecraft/world/level/block/entity/BlockEntity;FLnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;ZLnet/minecraft/client/renderer/culling/Frustum;)Lnet/minecraft/client/renderer/blockentity/state/BlockEntityRenderState;"
        )
    )
    private @Nullable BlockEntityRenderState kinetic$skipOccludedBlockEntity(
        BlockEntityRenderDispatcher dispatcher, BlockEntity blockEntity, float partialTicks,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress, boolean globallyRendered, @Nullable Frustum frustum,
        Operation<BlockEntityRenderState> original
    ) {
        if (!globallyRendered) {
            BlockEntityRenderer<BlockEntity, BlockEntityRenderState> renderer = dispatcher.getRenderer(blockEntity);
            if (renderer != null && EntityOcclusion.hidden(blockEntity, renderer.getRenderBoundingBox(blockEntity))) {
                return null;
            }
        }
        return original.call(dispatcher, blockEntity, partialTicks, breakProgress, globallyRendered, frustum);
    }
}
