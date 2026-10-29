package com.aryston.kinetic.debug.benchmark;

import com.aryston.kinetic.core.telemetry.FrameTimeSummary;
import com.aryston.kinetic.core.telemetry.TickSummary;
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
    public static final int FORMAT_VERSION = 2;
}
