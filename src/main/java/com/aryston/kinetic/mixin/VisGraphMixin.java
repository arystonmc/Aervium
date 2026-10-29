package com.aryston.kinetic.mixin;

import com.aryston.kinetic.client.culling.OccluderHolder;
import com.aryston.kinetic.core.culling.OccluderBoxes;
import com.aryston.kinetic.core.culling.OpaqueMask;
import java.util.BitSet;
import net.minecraft.client.renderer.chunk.VisGraph;
import net.minecraft.client.renderer.chunk.VisibilitySet;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VisGraph.class)
abstract class VisGraphMixin {
    @Shadow
    @Final
    private BitSet bitSet;

    @Unique
    private OccluderBoxes kinetic$occluders = OccluderBoxes.NONE;

    @Inject(method = "resolve", at = @At("HEAD"))
    private void kinetic$findOccluders(CallbackInfoReturnable<VisibilitySet> callback) {
        kinetic$occluders = OpaqueMask.fromBits(bitSet.toLongArray()).boxes();
    }

    @Inject(method = "resolve", at = @At("RETURN"))
    private void kinetic$attachOccluders(CallbackInfoReturnable<VisibilitySet> callback) {
        ((OccluderHolder) callback.getReturnValue()).kinetic$setOccluders(kinetic$occluders);
    }
}
