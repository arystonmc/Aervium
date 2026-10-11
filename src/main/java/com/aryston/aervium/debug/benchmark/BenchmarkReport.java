package com.aryston.aervium.debug.benchmark;

import com.aryston.aervium.core.telemetry.FrameTimeSummary;
import com.aryston.aervium.core.telemetry.TickSummary;
import java.util.List;

public record BenchmarkReport(
    int formatVersion,
    String startedAt,
    BenchmarkEnvironment environment,
    BenchmarkAnchors anchors,
    List<SceneResult> scenes,
    FrameTimeSummary overallFrames,
    TickSummary overallTicks
) {
    public static final int FORMAT_VERSION = 1;
}
