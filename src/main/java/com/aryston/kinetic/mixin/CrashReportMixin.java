package com.aryston.kinetic.mixin;

import com.aryston.kinetic.integration.crash.CrashReportSection;
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
    private static final String KINETIC$DETAIL = "Kinetic";

    @Shadow
    @Final
    private Throwable exception;

    @Shadow
    @Final
    private SystemReport systemReport;

    @Unique
    private boolean kinetic$detailAdded;

    @Inject(method = "getDetails(Ljava/lang/StringBuilder;)V", at = @At("HEAD"))
    private void kinetic$addDetail(StringBuilder builder, CallbackInfo callback) {
        if (!kinetic$detailAdded) {
            kinetic$detailAdded = true;
            systemReport.setDetail(KINETIC$DETAIL, () -> CrashReportSection.describe(exception));
        }
    }
}
