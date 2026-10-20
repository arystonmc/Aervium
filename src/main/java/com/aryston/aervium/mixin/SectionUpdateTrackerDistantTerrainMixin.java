package com.aryston.aervium.mixin;

import com.aryston.aervium.client.lod.DistantTerrain;
import net.minecraft.client.SectionUpdateTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SectionUpdateTracker.class)
abstract class SectionUpdateTrackerDistantTerrainMixin {
    @Inject(method = "setDirty", at = @At("HEAD"))
    private void aervium$terrainChanged(int x, int y, int z, boolean playerChanged, CallbackInfo callback) {
        DistantTerrain.chunkChanged(x, z);
    }
}
