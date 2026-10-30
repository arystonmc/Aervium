package com.aryston.kinetic.client;

import com.aryston.kinetic.client.chunk.MeshWorkers;
import com.aryston.kinetic.client.culling.HelionShadowCasters;
import com.aryston.kinetic.client.culling.TerrainOcclusion;
import com.aryston.kinetic.client.lod.DistantTerrain;
import com.aryston.kinetic.client.lod.DistantTerrainRenderer;
import com.aryston.kinetic.core.culling.CullingStatistics;
import com.aryston.kinetic.core.job.JobLoadMeter.JobLoad;
import com.aryston.kinetic.core.telemetry.ByteSizes;
import com.aryston.kinetic.core.telemetry.FrameCostSummary;
import com.aryston.kinetic.core.telemetry.FrameTimeSummary;
import com.aryston.kinetic.core.telemetry.GcPauseTotals;
import com.aryston.kinetic.core.telemetry.MotionErrorSummary;
import com.aryston.kinetic.core.telemetry.TelemetrySnapshot;
import com.aryston.kinetic.core.telemetry.TelemetrySnapshot.SystemTiming;
import com.aryston.kinetic.core.telemetry.TickSummary;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;

final class OverlayLines {
    static final int TEXT = 0xFFFFFFFF;
    static final int MUTED = 0xFFB8C4D0;
    static final int WARNING = 0xFFFFC857;
    static final int ALERT = 0xFFFF5C5C;

    private static final double MEGABYTES_PER_GIGABYTE = 1024.0;
    private static final double NANOS_PER_MILLI = 1.0E6;
    private static final double MOTION_ERROR_WARNING_MS = 0.5;

    private OverlayLines() {
    }

    static List<Line> of(TelemetrySnapshot snapshot, List<String> passiveRendererMods) {
        List<Line> lines = new ArrayList<>();
        FrameTimeSummary frames = snapshot.frames();
        lines.add(new Line(format("Kinetic  FPS %.0f  1%% low %.0f  0.1%% low %.0f", frames.averageFps(), frames.onePercentLowFps(), frames.pointOnePercentLowFps()), TEXT));
        lines.add(new Line(format("Frame %.2f ms  stddev %.2f ms  max %.1f ms", frames.averageMs(), frames.standardDeviationMs(), frames.maxMs()), MUTED));
        lines.add(costLine(snapshot.costs()));
        lines.add(smoothnessLine(frames, snapshot.motion()));
        lines.add(snapshot.serverTicks().map(OverlayLines::tickLine).orElse(new Line("MSPT: no local server", MUTED)));
        lines.add(gcLine(snapshot.gcPauses()));
        lines.add(new Line(memoryText(snapshot), MUTED));
        lines.add(cullingLine(CullingStatistics.SHARED));
        if (DistantTerrain.enabled()) {
            lines.add(distantTerrainLine(DistantTerrain.statistics()));
        }
        if (MeshWorkers.active()) {
            lines.add(meshLine(MeshWorkers.overlayLoad(System.nanoTime())));
        }
        snapshot.timings().forEach(timing -> lines.add(timingLine(timing)));
        if (!passiveRendererMods.isEmpty()) {
            lines.add(new Line("Optimizations off: " + String.join(", ", passiveRendererMods) + " installed", WARNING));
        }
        return lines;
    }

    private static Line costLine(FrameCostSummary costs) {
        if (costs.limit() == FrameCostSummary.Limit.UNKNOWN) {
            return new Line(format("CPU %.2f ms  GPU n/a", costs.averageCpuMs()), MUTED);
        }
        return new Line(format("CPU %.2f ms  GPU %.2f ms  %s", costs.averageCpuMs(), costs.averageGpuMs(), costs.limit().label()), TEXT);
    }

    private static Line smoothnessLine(FrameTimeSummary frames, MotionErrorSummary motion) {
        int color = motion.p99Ms() > MOTION_ERROR_WARNING_MS || frames.hitches() > 0 ? WARNING : MUTED;
        return new Line(format("Hitches %d  motion error %.2f ms avg  %.2f ms p99  %.2f ms max", frames.hitches(), motion.averageMs(), motion.p99Ms(), motion.maxMs()), color);
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
            return new Line("Culling off in settings", WARNING);
        }
        String camera = TerrainOcclusion.cameraPaused()
            ? "Culling camera resting: under 15% hidden here"
            : format("Culling camera %d of %d hidden (%d%%)", culling.cameraHidden(), culling.cameraSections(),
                CullingStatistics.percent(culling.cameraHidden(), culling.cameraSections()));
        camera += format("  worker %.2f ms", culling.cameraJobNanos() / NANOS_PER_MILLI);
        camera += format("  entities %d of %d", culling.entitiesHidden(), culling.entitiesTested());
        camera += format("  particles %d of %d", culling.particlesHidden(), culling.particlesTested());
        if (!HelionShadowCasters.active()) {
            return new Line(camera, MUTED);
        }
        if (TerrainOcclusion.shadowsPaused()) {
            return new Line(camera + "  shadows resting", MUTED);
        }
        return new Line(camera + format("  shadows %d of %d (%d%%)", culling.shadowHidden(), culling.shadowCasters(),
            CullingStatistics.percent(culling.shadowHidden(), culling.shadowCasters())), MUTED);
    }

    private static Line distantTerrainLine(DistantTerrain.Statistics terrain) {
        return new Line(format("Distant terrain %d chunks recorded  %d tiles generated  %d tiles  %.0f MB  %d meshes  %d drawn", terrain.chunks(), terrain.generated(), terrain.tiles(), ByteSizes.megabytes(terrain.bytes()), DistantTerrainRenderer.meshCount(), DistantTerrainRenderer.drawnCount()), MUTED);
    }

    private static Line meshLine(JobLoad load) {
        SectionRenderDispatcher dispatcher = Minecraft.getInstance().levelRenderer.sectionRenderDispatcher();
        int queued = dispatcher == null ? 0 : dispatcher.getCompileQueueSize();
        return new Line(format("Chunk builds %.0f/s  mesh workers %d  busy %.0f%%  queued %d",
            load.jobsPerSecond(), MeshWorkers.workers(), load.busyShare() * 100.0, queued), MUTED);
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
