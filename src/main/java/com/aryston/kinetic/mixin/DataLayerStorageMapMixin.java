package com.aryston.kinetic.mixin;

import com.aryston.kinetic.core.collection.ShardedLongMap;
import com.aryston.kinetic.core.collection.ShardedLongMapBuilder;
import com.aryston.kinetic.integration.light.LightSnapshotSource;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.lighting.DataLayerStorageMap;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DataLayerStorageMap.class)
abstract class DataLayerStorageMapMixin implements LightSnapshotSource {
    @Shadow
    @Final
    protected Long2ObjectOpenHashMap<DataLayer> map;

    @Unique
    private @Nullable ShardedLongMapBuilder<DataLayer> kinetic$snapshots;

    @Override
    public Long2ObjectOpenHashMap<DataLayer> kinetic$snapshot() {
        if (map instanceof ShardedLongMap<DataLayer> snapshot) {
            return snapshot;
        }
        ShardedLongMapBuilder<DataLayer> snapshots = kinetic$snapshots;
        if (snapshots == null) {
            snapshots = ShardedLongMapBuilder.copyOf(map);
            kinetic$snapshots = snapshots;
        }
        return snapshots.publish();
    }

    @Inject(method = "setLayer", at = @At("TAIL"))
    private void kinetic$layerSet(long sectionNode, DataLayer layer, CallbackInfo callback) {
        ShardedLongMapBuilder<DataLayer> snapshots = kinetic$snapshots;
        if (snapshots != null) {
            snapshots.put(sectionNode, layer);
        }
    }

    @Inject(method = "copyDataLayer", at = @At("RETURN"))
    private void kinetic$layerCopied(long sectionNode, CallbackInfoReturnable<DataLayer> callback) {
        ShardedLongMapBuilder<DataLayer> snapshots = kinetic$snapshots;
        if (snapshots != null) {
            snapshots.put(sectionNode, callback.getReturnValue());
        }
    }

    @Inject(method = "removeLayer", at = @At("RETURN"))
    private void kinetic$layerRemoved(long sectionNode, CallbackInfoReturnable<DataLayer> callback) {
        ShardedLongMapBuilder<DataLayer> snapshots = kinetic$snapshots;
        if (snapshots != null) {
            snapshots.remove(sectionNode);
        }
    }
}
