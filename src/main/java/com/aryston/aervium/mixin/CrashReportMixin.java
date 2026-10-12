package com.aryston.aervium.mixin;

import com.aryston.aervium.integration.crash.CrashReportSection;
import net.minecraft.CrashReport;
import net.minecraft.SystemReport;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CrashReport.class)
abstract class CrashReportMixin {
    @Unique
    private static final String AERVIUM$DETAIL = "Aervium";

    @Shadow
    @Final
    private Throwable exception;

    @Shadow
    @Final
    private SystemReport systemReport;

    @Unique
    private boolean aervium$detailAdded;

    @Inject(method = "getDetails(Ljava/lang/StringBuilder;)V", at = @At("HEAD"))
    private void aervium$addDetail(StringBuilder builder, CallbackInfo callback) {
        if (!aervium$detailAdded) {
            aervium$detailAdded = true;
            systemReport.setDetail(AERVIUM$DETAIL, () -> CrashReportSection.describe(exception));
        }
    }
}
