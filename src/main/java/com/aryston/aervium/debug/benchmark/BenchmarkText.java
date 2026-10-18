package com.aryston.aervium.debug.benchmark;

import com.aryston.aervium.core.telemetry.FrameTimeSummary;
import com.aryston.aervium.core.telemetry.TickSummary;
import java.util.Locale;
import java.util.Optional;

public final class BenchmarkText {
    private static final String NEW_LINE = "\n";
    private static final double PERCENT = 100.0;
    private static final String RULE = "-".repeat(118);
    private static final String SCENE_HEADER = String.format(Locale.ROOT,
        "%-12s %8s %8s %8s %9s %9s %9s %9s %9s %8s %8s %9s",
        "Scene", "Avg FPS", "1% low", "0.1% low", "Frame ms", "StdDev", "Max ms", "MSPT avg", "MSPT p99", ">50ms", "GC ms", "Heap MB"
    );

    private BenchmarkText() {
    }

    public static String format(BenchmarkReport report, Optional<BenchmarkComparison> comparison) {
        StringBuilder text = new StringBuilder();
        BenchmarkEnvironment environment = report.environment();
        line(text, "Aervium benchmark " + report.startedAt());
        line(text, RULE);
        line(text, "Aervium %s, Minecraft %s, Java %s, %s", environment.aerviumVersion(), environment.minecraftVersion(), environment.javaVersion(), environment.operatingSystem());
        line(text, "GPU %s (%s), %d CPU threads, heap limit %.0f MB, %s collector", environment.gpu(), environment.graphicsBackend(), environment.cpuThreads(), environment.maxHeapMb(), environment.garbageCollector());
        line(text, "Window %dx%d, render distance %d, simulation distance %d, frame limit %d, vsync %s",
            environment.windowWidth(), environment.windowHeight(), environment.renderDistance(), environment.simulationDistance(),
            environment.framerateLimit(), environment.vsync() ? "on" : "off");
        line(text, "Seed %d, %d scenes with built sites, mods: %s", environment.seed(), report.anchors().sites().size(), String.join(", ", environment.mods()));
        line(text, "");
        line(text, SCENE_HEADER);
        line(text, RULE);
        report.scenes().forEach(scene -> sceneRow(text, scene.scene(), scene.frames(), scene.ticks(), scene.gcPauses().totalMs(), scene.peakHeapMb()));
        line(text, RULE);
        sceneRow(text, "overall", report.overallFrames(), report.overallTicks(), totalGcMs(report), peakHeapMb(report));
        line(text, "");
        line(text, "MSPT distribution (ticks per bucket, upper bounds in ms: 5, 10, 20, 30, 40, 50, over 50)");
        report.scenes().forEach(scene -> line(text, "%-12s %s   %.2f %% over 50 ms", scene.scene(), histogram(scene.ticks()), scene.ticks().overBudgetShare() * PERCENT));
        line(text, "");
        report.scenes().forEach(scene -> line(text, "%-12s peak VRAM %.0f MB, %d GC pauses (longest %.1f ms), %d frames dropped from the record",
            scene.scene(), scene.peakVramMb(), scene.gcPauses().pauses(), scene.gcPauses().longestMs(), scene.droppedFrames()));
        line(text, "");
        report.scenes().forEach(scene -> line(text, "%-12s median frame %.2f ms, p99 %.2f ms, p99.9 %.2f ms, %d frames over 33 ms, %d over 50 ms, %d hitches, motion error %.3f ms avg %.3f ms p99",
            scene.scene(), scene.frames().medianMs(), scene.frames().p99Ms(), scene.frames().p999Ms(), scene.frames().framesOver33Ms(), scene.frames().framesOver50Ms(),
            scene.frames().hitches(), scene.motion().averageMs(), scene.motion().p99Ms()));
        comparison.ifPresent(delta -> comparisonSection(text, delta));
        return text.toString();
    }

    private static void sceneRow(StringBuilder text, String name, FrameTimeSummary frames, TickSummary ticks, double gcMs, double heapMb) {
        line(text, "%-12s %8.1f %8.1f %8.1f %9.2f %9.2f %9.2f %9.2f %9.2f %8d %8.1f %9.0f",
            name, frames.averageFps(), frames.onePercentLowFps(), frames.pointOnePercentLowFps(), frames.averageMs(),
            frames.standardDeviationMs(), frames.maxMs(), ticks.averageMs(), ticks.p99Ms(), ticks.ticksOverBudget(), gcMs, heapMb);
    }

    private static void comparisonSection(StringBuilder text, BenchmarkComparison comparison) {
        line(text, "");
        line(text, "Change against the previous run (%s)", comparison.previousStartedAt());
        comparison.scenes().forEach(delta -> line(text, "%-12s avg FPS %+.1f %%, 1%% low %+.1f %%, 0.1%% low %+.1f %%, MSPT %+.1f %%",
            delta.scene(), delta.averageFpsPercent(), delta.onePercentLowPercent(), delta.pointOnePercentLowPercent(), delta.averageMsptPercent()));
        line(text, "Largest average FPS change: %.1f %%", comparison.largestAverageFpsChangePercent());
    }

    private static String histogram(TickSummary ticks) {
        StringBuilder buckets = new StringBuilder();
        for (int count : ticks.histogram()) {
            buckets.append(String.format(Locale.ROOT, "%6d", count));
        }
        return buckets.toString();
    }

    private static double totalGcMs(BenchmarkReport report) {
        return report.scenes().stream().mapToDouble(scene -> scene.gcPauses().totalMs()).sum();
    }

    private static double peakHeapMb(BenchmarkReport report) {
        return report.scenes().stream().mapToDouble(SceneResult::peakHeapMb).max().orElse(0.0);
    }

    private static void line(StringBuilder text, String format, Object... arguments) {
        text.append(arguments.length == 0 ? format : String.format(Locale.ROOT, format, arguments)).append(NEW_LINE);
    }
}
