package com.aryston.aervium.mixin;

import com.aryston.aervium.client.chunk.SectionDrawRecord;
import com.aryston.aervium.client.chunk.SectionDrawRecordHolder;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(SectionRenderDispatcher.RenderSection.class)
abstract class RenderSectionMixin implements SectionDrawRecordHolder {
    @Unique
    private static final int AERVIUM$NO_EPOCH = 0;

    @Unique
    private @Nullable SectionDrawRecord aervium$drawRecord;

    @Unique
    private int @Nullable [] aervium$packedDraws;

    @Unique
    private int aervium$gpuVersion;

    @Unique
    private int aervium$gpuEpoch;

    @Unique
    private int aervium$gpuFlags;

    @Override
    public @Nullable SectionDrawRecord aervium$drawRecord() {
        return aervium$drawRecord;
    }

    @Override
    public int @Nullable [] aervium$packedDraws() {
        return aervium$packedDraws;
    }

    @Override
    public void aervium$setDrawRecord(SectionDrawRecord record) {
        aervium$drawRecord = record;
        aervium$packedDraws = record.packedDraws();
        record.attach(this);
    }

    @Override
    public int aervium$gpuVersion() {
        return aervium$gpuVersion;
    }

    @Override
    public int aervium$gpuEpoch() {
        return aervium$gpuEpoch;
    }

    @Override
    public int aervium$gpuFlags() {
        return aervium$gpuFlags;
    }

    @Override
    public void aervium$setGpuState(int meshVersion, int epoch, int flags) {
        aervium$gpuVersion = meshVersion;
        aervium$gpuEpoch = epoch;
        aervium$gpuFlags = flags;
    }

    @Override
    public void aervium$forgetGpuState() {
        aervium$gpuEpoch = AERVIUM$NO_EPOCH;
    }
}
