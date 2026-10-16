package com.aryston.aervium.mixin;

import com.aryston.aervium.client.culling.OccluderHolder;
import com.aryston.aervium.core.culling.OccluderBoxes;
import com.aryston.aervium.core.culling.OpaqueMask;
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
    private OccluderBoxes aervium$occluders = OccluderBoxes.NONE;

    @Inject(method = "resolve", at = @At("HEAD"))
    private void aervium$findOccluders(CallbackInfoReturnable<VisibilitySet> callback) {
        aervium$occluders = OpaqueMask.fromBits(bitSet.toLongArray()).boxes();
    }

    @Inject(method = "resolve", at = @At("RETURN"))
    private void aervium$attachOccluders(CallbackInfoReturnable<VisibilitySet> callback) {
        ((OccluderHolder) callback.getReturnValue()).aervium$setOccluders(aervium$occluders);
    }
}
