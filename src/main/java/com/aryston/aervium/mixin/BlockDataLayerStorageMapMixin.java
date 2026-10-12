package com.aryston.aervium.mixin;

import com.aryston.aervium.integration.light.LightSnapshotSource;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.minecraft.world.level.lighting.BlockLightSectionStorage$BlockDataLayerStorageMap")
abstract class BlockDataLayerStorageMapMixin {
    @Redirect(
        method = "copy",
        at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectOpenHashMap;clone()Lit/unimi/dsi/fastutil/longs/Long2ObjectOpenHashMap;")
    )
    private Long2ObjectOpenHashMap<?> aervium$publishSnapshot(Long2ObjectOpenHashMap<?> sections) {
        return ((LightSnapshotSource) (Object) this).aervium$snapshot();
    }
}
