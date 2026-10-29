package com.aryston.kinetic.debug.benchmark;

import com.aryston.kinetic.core.telemetry.FrameTimeSummary;
import com.aryston.kinetic.core.telemetry.GcPauseTotals;
import com.aryston.kinetic.core.telemetry.MotionErrorSummary;
import com.aryston.kinetic.core.telemetry.TickSummary;

public record SceneResult(
    String scene,
    String description,
    double seconds,
    FrameTimeSummary frames,
    MotionErrorSummary motion,
    long droppedFrames,
    TickSummary ticks,
    GcPauseTotals gcPauses,
    double peakHeapMb,
    double peakVramMb
) {
}
