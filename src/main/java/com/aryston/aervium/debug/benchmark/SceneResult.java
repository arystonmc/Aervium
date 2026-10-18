package com.aryston.aervium.debug.benchmark;

import com.aryston.aervium.core.telemetry.FrameTimeSummary;
import com.aryston.aervium.core.telemetry.GcPauseTotals;
import com.aryston.aervium.core.telemetry.MotionErrorSummary;
import com.aryston.aervium.core.telemetry.TickSummary;

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
