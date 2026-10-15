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
    private @Nullable SectionDrawRecord aervium$drawRecord;

    @Override
    public @Nullable SectionDrawRecord aervium$drawRecord() {
        return aervium$drawRecord;
    }

    @Override
    public void aervium$setDrawRecord(SectionDrawRecord record) {
        aervium$drawRecord = record;
    }
}
