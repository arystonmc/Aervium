package com.aryston.aervium.mixin;

import com.aryston.aervium.world.poi.PoiColumns;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import java.util.Optional;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.storage.SectionStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SectionStorage.class)
abstract class SectionStoragePoiIndexMixin<R> {
    @Shadow
    @Final
    @Mutable
    private Long2ObjectMap<Optional<R>> storage;

    @Shadow
    @Final
    protected LevelHeightAccessor levelHeightAccessor;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void aervium$indexPoiColumns(CallbackInfo callback) {
        if ((Object) this instanceof PoiManager && storage.isEmpty() && PoiColumns.supports(levelHeightAccessor)) {
            storage = PoiColumns.createMap(levelHeightAccessor);
        }
    }
}
