package com.aryston.kinetic.mixin;

import com.aryston.kinetic.client.chunk.SectionDrawRecord;
import com.aryston.kinetic.client.chunk.SectionDrawRecordHolder;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(SectionRenderDispatcher.RenderSection.class)
abstract class RenderSectionMixin implements SectionDrawRecordHolder {
    @Unique
    private static final int KINETIC$NO_EPOCH = 0;

    @Unique
    private @Nullable SectionDrawRecord kinetic$drawRecord;

    @Unique
    private int @Nullable [] kinetic$packedDraws;

    @Unique
    private int kinetic$gpuVersion;

    @Unique
    private int kinetic$gpuEpoch;

    @Unique
    private int kinetic$gpuFlags;

    @Override
    public @Nullable SectionDrawRecord kinetic$drawRecord() {
        return kinetic$drawRecord;
    }

    @Override
    public int @Nullable [] kinetic$packedDraws() {
        return kinetic$packedDraws;
    }

    @Override
    public void kinetic$setDrawRecord(SectionDrawRecord record) {
        kinetic$drawRecord = record;
        kinetic$packedDraws = record.packedDraws();
        record.attach(this);
    }

    @Override
    public int kinetic$gpuVersion() {
        return kinetic$gpuVersion;
    }

    @Override
    public int kinetic$gpuEpoch() {
        return kinetic$gpuEpoch;
    }

    @Override
    public int kinetic$gpuFlags() {
        return kinetic$gpuFlags;
    }

    @Override
    public void kinetic$setGpuState(int meshVersion, int epoch, int flags) {
        kinetic$gpuVersion = meshVersion;
        kinetic$gpuEpoch = epoch;
        kinetic$gpuFlags = flags;
    }

    @Override
    public void kinetic$forgetGpuState() {
        kinetic$gpuEpoch = KINETIC$NO_EPOCH;
    }
}
