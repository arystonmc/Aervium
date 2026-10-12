package com.aryston.aervium.debug.benchmark;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aryston.aervium.core.telemetry.FrameTimeSummary;
import com.aryston.aervium.core.telemetry.GcPauseTotals;
import com.aryston.aervium.core.telemetry.TickSummary;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BenchmarkReportTest {
    private static final double EPSILON = 1e-9;
    private static final long TEN_MS = 10_000_000L;
    private static final long EIGHT_MS = 8_000_000L;

    @TempDir
    Path folder;

    @Test
    void comparesScenesWithThePreviousRun() {
        BenchmarkReport before = report("20261011-120000", TEN_MS);
        BenchmarkReport after = report("20261011-130000", EIGHT_MS);
        BenchmarkComparison comparison = BenchmarkComparison.between(before, after);
        assertEquals("20261011-120000", comparison.previousStartedAt());
        assertEquals(1, comparison.scenes().size());
        assertEquals(25.0, comparison.scenes().getFirst().averageFpsPercent(), EPSILON);
        assertEquals(25.0, comparison.largestAverageFpsChangePercent(), EPSILON);
        assertEquals(0.0, BenchmarkComparison.changePercent(0.0, 5.0), EPSILON);
    }

    @Test
    void writesJsonThatReadsBackAsTheSameReport() throws IOException {
        BenchmarkFiles files = new BenchmarkFiles(folder);
        BenchmarkReport first = report("20261011-120000", TEN_MS);
        BenchmarkReport second = report("20261011-130000", EIGHT_MS);
        files.write(first, "first");
        Path text = files.write(second, "second");
        assertEquals("second", Files.readString(text));
        BenchmarkReport latest = files.latest().orElseThrow();
        assertEquals(second.startedAt(), latest.startedAt());
        assertEquals(second.scenes().getFirst().frames(), latest.scenes().getFirst().frames());
        assertArrayEquals(second.overallTicks().histogram(), latest.overallTicks().histogram());
        assertEquals(second.environment(), latest.environment());
    }

    @Test
    void readsNothingFromAMissingFolder() {
        assertTrue(new BenchmarkFiles(folder.resolve("missing")).latest().isEmpty());
    }

    @Test
    void textReportListsEverySceneAndTheComparison() {
        BenchmarkReport before = report("20261011-120000", TEN_MS);
        BenchmarkReport after = report("20261011-130000", EIGHT_MS);
        String text = BenchmarkText.format(after, Optional.of(BenchmarkComparison.between(before, after)));
        assertTrue(text.contains("walk"));
        assertTrue(text.contains("overall"));
        assertTrue(text.contains("avg FPS +25.0 %"));
        assertTrue(text.contains("MSPT distribution"));
    }

    @Test
    void fileNamesSortByStartTime() throws IOException {
        BenchmarkFiles files = new BenchmarkFiles(folder);
        files.write(report("20261011-090000", TEN_MS), "late write of an early run");
        files.write(report("20261011-080000", EIGHT_MS), "early run");
        assertEquals("20261011-090000", files.latest().orElseThrow().startedAt());
        assertEquals(4, Files.list(folder).count());
    }

    private static BenchmarkReport report(String startedAt, long frameNanos) {
        long[] frames = new long[200];
        Arrays.fill(frames, frameNanos);
        long[] ticks = new long[100];
        Arrays.fill(ticks, 12_000_000L);
        FrameTimeSummary frameSummary = FrameTimeSummary.of(frames, frames.length);
        TickSummary tickSummary = TickSummary.of(ticks, ticks.length);
        SceneResult walk = new SceneResult("walk", "Walking", 30.0, frameSummary, 0, tickSummary, new GcPauseTotals(2, 7.5, 5.0), 1800.0, 900.0);
        BenchmarkEnvironment environment = new BenchmarkEnvironment(
            "0.1", "26.3", "25", "Windows 11 10.0", 16, 4096.0, "G1", "Test GPU", "Vulkan",
            1920, 1080, 12, 12, 260, false, 8_675_309L, List.of("aervium 0.1", "minecraft 26.3")
        );
        BlockPoint spawn = new BlockPoint(0, 64, 0);
        return new BenchmarkReport(
            BenchmarkReport.FORMAT_VERSION,
            startedAt,
            environment,
            new BenchmarkAnchors(spawn, spawn.offset(200, 3, -150), true, spawn.offset(96, 24, -96)),
            List.of(walk),
            frameSummary,
            tickSummary
        );
    }
}
