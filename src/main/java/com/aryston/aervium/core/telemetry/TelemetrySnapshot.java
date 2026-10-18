package com.aryston.aervium.core.telemetry;

import com.aryston.aervium.core.budget.TimedSystem;
import java.util.List;
import java.util.Optional;

public record TelemetrySnapshot(
    FrameTimeSummary frames,
    MotionErrorSummary motion,
    Optional<TickSummary> serverTicks,
    GcPauseTotals gcPauses,
    HeapUsage heap,
    VramUsage vram,
    List<SystemTiming> timings
) {
    public record SystemTiming(TimedSystem system, double smoothedMs, double peakMs, boolean overBudget) {
    }
}
