package com.aryston.aervium.client;

import com.aryston.aervium.client.culling.HelionShadowCasters;
import com.aryston.aervium.client.culling.TerrainOcclusion;
import com.aryston.aervium.core.culling.CullingStatistics;
import com.aryston.aervium.core.telemetry.ByteSizes;
import com.aryston.aervium.core.telemetry.FrameTimeSummary;
import com.aryston.aervium.core.telemetry.GcPauseTotals;
import com.aryston.aervium.core.telemetry.TelemetrySnapshot;
import com.aryston.aervium.core.telemetry.TelemetrySnapshot.SystemTiming;
import com.aryston.aervium.core.telemetry.TickSummary;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class OverlayLines {
    static final int TEXT = 0xFFFFFFFF;
    static final int MUTED = 0xFFB8C4D0;
    static final int WARNING = 0xFFFFC857;
    static final int ALERT = 0xFFFF5C5C;

    private static final double MEGABYTES_PER_GIGABYTE = 1024.0;

    private OverlayLines() {
    }

    static List<Line> of(TelemetrySnapshot snapshot, List<String> passiveRendererMods) {
        List<Line> lines = new ArrayList<>();
        FrameTimeSummary frames = snapshot.frames();
        lines.add(new Line(format("Aervium  FPS %.0f  1%% low %.0f  0.1%% low %.0f", frames.averageFps(), frames.onePercentLowFps(), frames.pointOnePercentLowFps()), TEXT));
        lines.add(new Line(format("Frame %.2f ms  stddev %.2f ms  max %.1f ms", frames.averageMs(), frames.standardDeviationMs(), frames.maxMs()), MUTED));
        lines.add(snapshot.serverTicks().map(OverlayLines::tickLine).orElse(new Line("MSPT: no local server", MUTED)));
        lines.add(gcLine(snapshot.gcPauses()));
        lines.add(new Line(memoryText(snapshot), MUTED));
        lines.add(cullingLine(CullingStatistics.SHARED));
        snapshot.timings().forEach(timing -> lines.add(timingLine(timing)));
        if (!passiveRendererMods.isEmpty()) {
            lines.add(new Line("Optimizations off: " + String.join(", ", passiveRendererMods) + " installed", WARNING));
        }
        return lines;
    }

    private static Line tickLine(TickSummary ticks) {
        int color = ticks.ticksOverBudget() > 0 ? ALERT : MUTED;
        return new Line(format("MSPT %.1f avg  %.1f p99  %.1f max  over 50 ms: %d", ticks.averageMs(), ticks.p99Ms(), ticks.maxMs(), ticks.ticksOverBudget()), color);
    }

    private static Line gcLine(GcPauseTotals pauses) {
        return new Line(format("GC pauses %d  total %.0f ms  longest %.1f ms", pauses.pauses(), pauses.totalMs(), pauses.longestMs()), MUTED);
    }

    private static Line cullingLine(CullingStatistics culling) {
        if (!TerrainOcclusion.enabled()) {
            return new Line("Culling off (F6)", WARNING);
        }
        String camera = format("Culling camera %d of %d hidden (%d%%)", culling.cameraHidden(), culling.cameraSections(),
            CullingStatistics.percent(culling.cameraHidden(), culling.cameraSections()));
        camera += format("  entities %d of %d", culling.entitiesHidden(), culling.entitiesTested());
        if (!HelionShadowCasters.active()) {
            return new Line(camera, MUTED);
        }
        return new Line(camera + format("  shadows %d of %d (%d%%)", culling.shadowHidden(), culling.shadowCasters(),
            CullingStatistics.percent(culling.shadowHidden(), culling.shadowCasters())), MUTED);
    }

    private static String memoryText(TelemetrySnapshot snapshot) {
        String heap = format("RAM %.2f / %.2f GB", gigabytes(snapshot.heap().usedBytes()), gigabytes(snapshot.heap().maxBytes()));
        if (!snapshot.vram().available()) {
            return heap + "  VRAM n/a";
        }
        return heap + format("  VRAM %.2f / %.2f GB", gigabytes(snapshot.vram().usedBytes()), gigabytes(snapshot.vram().budgetBytes()));
    }

    private static Line timingLine(SystemTiming timing) {
        int color = timing.overBudget() ? ALERT : MUTED;
        return new Line(format("%s %.2f ms  peak %.2f ms", timing.system().label(), timing.smoothedMs(), timing.peakMs()), color);
    }

    private static double gigabytes(long bytes) {
        return ByteSizes.megabytes(bytes) / MEGABYTES_PER_GIGABYTE;
    }

    private static String format(String pattern, Object... arguments) {
        return String.format(Locale.ROOT, pattern, arguments);
    }

    record Line(String text, int color) {
    }
}
