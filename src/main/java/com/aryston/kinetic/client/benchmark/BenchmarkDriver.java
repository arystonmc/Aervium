package com.aryston.kinetic.client.benchmark;

import com.aryston.kinetic.Kinetic;
import com.aryston.kinetic.client.FrameRateGovernor;
import com.aryston.kinetic.client.chunk.ChunkUploadTimings;
import com.aryston.kinetic.client.chunk.MeshWorkers;
import com.aryston.kinetic.client.gpu.GpuStageTimer;
import com.aryston.kinetic.core.budget.StallMeter;
import com.aryston.kinetic.core.culling.CameraResultCounts;
import com.aryston.kinetic.core.culling.CullingStatistics;
import com.aryston.kinetic.core.job.JobLoadMeter;
import com.aryston.kinetic.core.job.JobSystem;
import com.aryston.kinetic.core.job.MainThreadQueue;
import com.aryston.kinetic.core.telemetry.Durations;
import com.aryston.kinetic.core.telemetry.FrameCostRecorder;
import com.aryston.kinetic.core.telemetry.FrameTimeRecorder;
import com.aryston.kinetic.core.telemetry.FrameTimeSummary;
import com.aryston.kinetic.core.telemetry.GcPauseRecorder;
import com.aryston.kinetic.core.telemetry.HeapUsage;
import com.aryston.kinetic.core.telemetry.MotionErrorRecorder;
import com.aryston.kinetic.core.telemetry.StageTimes;
import com.aryston.kinetic.core.telemetry.TickSummary;
import com.aryston.kinetic.core.telemetry.VramUsage;
import com.aryston.kinetic.debug.benchmark.BenchmarkAnchors;
import com.aryston.kinetic.debug.benchmark.BenchmarkComparison;
import com.aryston.kinetic.debug.benchmark.BenchmarkEnvironment;
import com.aryston.kinetic.debug.benchmark.BenchmarkFiles;
import com.aryston.kinetic.debug.benchmark.BenchmarkReport;
import com.aryston.kinetic.debug.benchmark.BenchmarkRoute;
import com.aryston.kinetic.debug.benchmark.BenchmarkScene;
import com.aryston.kinetic.debug.benchmark.BenchmarkText;
import com.aryston.kinetic.debug.benchmark.BlockPoint;
import com.aryston.kinetic.debug.benchmark.SceneMeasurement;
import com.aryston.kinetic.debug.benchmark.SceneResult;
import com.aryston.kinetic.debug.route.CameraPath;
import com.aryston.kinetic.world.TickTimeRecorder;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.LongStream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.server.IntegratedServer;
import org.lwjgl.sdl.SDLVideo;

public final class BenchmarkDriver {
    private static final String PROPERTY = "kinetic.benchmark";
    private static final String QUIT_PROPERTY = "kinetic.benchmark.quit";
    private static final String GPU_TIMINGS_PROPERTY = "kinetic.benchmark.gpuTimings";
    private static final String SCREENSHOTS_PROPERTY = "kinetic.benchmark.screenshots";
    private static final int SCREENSHOT_SCALE = 1;
    private static final String REPORT_FOLDER = "kinetic/benchmarks";
    private static final DateTimeFormatter STARTED_AT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final int FRAME_CAPACITY = 1 << 17;
    private static final int VRAM_SAMPLE_INTERVAL_TICKS = 20;

    private final GcPauseRecorder gcPauses;
    private final JobSystem jobs;
    private final MainThreadQueue mainThread;
    private final Supplier<VramUsage> vramProbe;
    private final boolean quitWhenDone = Boolean.getBoolean(QUIT_PROPERTY);
    private final boolean gpuTimings = Boolean.getBoolean(GPU_TIMINGS_PROPERTY);
    private final boolean screenshots = Boolean.getBoolean(SCREENSHOTS_PROPERTY);
    private final FrameTimeRecorder frames = new FrameTimeRecorder(FRAME_CAPACITY);
    private final MotionErrorRecorder motion = new MotionErrorRecorder(FRAME_CAPACITY);
    private final BenchmarkCamera camera = new BenchmarkCamera();
    private final List<SceneResult> results = new ArrayList<>();
    private final List<long[]> allFrames = new ArrayList<>();
    private final List<long[]> allTicks = new ArrayList<>();
    private Stage stage = Stage.CREATE_WORLD;
    private CompletableFuture<BenchmarkAnchors> pendingAnchors;
    private CompletableFuture<Boolean> pendingBuild;
    private boolean siteBuilt;
    private BlockPoint site;
    private BenchmarkAnchors anchors;
    private String startedAt;
    private boolean pauseOnLostFocus;
    private final BenchmarkOptions options = new BenchmarkOptions();
    private boolean focusOverridden;
    private int sceneIndex;
    private int tick;
    private CameraPath path;
    private SceneMeasurement measurement;
    private long lastVramBytes;
    private JobLoadMeter meshMeter;
    private long queuedSum;
    private int queuedSamples;
    private long settleMark;

    private BenchmarkDriver(GcPauseRecorder gcPauses, JobSystem jobs, MainThreadQueue mainThread, Supplier<VramUsage> vramProbe) {
        this.gcPauses = gcPauses;
        this.jobs = jobs;
        this.mainThread = mainThread;
        this.vramProbe = vramProbe;
    }

    public static Optional<BenchmarkDriver> requested(GcPauseRecorder gcPauses, JobSystem jobs, MainThreadQueue mainThread, Supplier<VramUsage> vramProbe) {
        if (!Boolean.getBoolean(PROPERTY)) {
            return Optional.empty();
        }
        return Optional.of(new BenchmarkDriver(gcPauses, jobs, mainThread, vramProbe));
    }

    public void frameStarted(long nanoTime) {
        if (stage == Stage.SCENE) {
            frames.frameStarted(nanoTime);
        }
    }

    public void motionSampled(long nanoTime, double simulatedMillis) {
        if (stage == Stage.SCENE) {
            motion.framePresented(nanoTime, simulatedMillis);
        }
    }

    public void motionInterrupted() {
        motion.interrupted();
    }

    public void clientTick(Minecraft minecraft) {
        switch (stage) {
            case CREATE_WORLD -> createWorld(minecraft);
            case WAIT_FOR_PLAYER -> waitForPlayer(minecraft);
            case LOCATE -> locate(minecraft);
            case WARMUP -> warmUp(minecraft);
            case SCENE -> runScene(minecraft);
            case FINISHED -> {
            }
        }
    }

    private void createWorld(Minecraft minecraft) {
        if (!minecraft.isGameLoadFinished() || minecraft.level != null || minecraft.gui.screen() == null) {
            return;
        }
        options.apply(minecraft.options);
        SDLVideo.SDL_RaiseWindow(minecraft.getWindow().handle());
        try {
            BenchmarkWorld.createFresh(minecraft, minecraft.gui.screen());
            Kinetic.LOGGER.info("Kinetic benchmark: creating a fresh world with seed {}", BenchmarkWorld.SEED);
            stage = Stage.WAIT_FOR_PLAYER;
        } catch (IOException exception) {
            abort(minecraft, "the benchmark world could not be created", exception);
        }
    }

    private void waitForPlayer(Minecraft minecraft) {
        IntegratedServer server = minecraft.getSingleplayerServer();
        if (minecraft.player == null || minecraft.level == null || server == null) {
            return;
        }
        pauseOnLostFocus = minecraft.options.pauseOnLostFocus;
        minecraft.options.pauseOnLostFocus = false;
        focusOverridden = true;
        FrameRateGovernor.suspend(true);
        startedAt = LocalDateTime.now().format(STARTED_AT);
        pendingAnchors = BenchmarkWorld.prepare(server);
        stage = Stage.LOCATE;
    }

    private void locate(Minecraft minecraft) {
        if (leftWorld(minecraft) || !pendingAnchors.isDone()) {
            return;
        }
        try {
            anchors = pendingAnchors.join();
        } catch (RuntimeException failure) {
            abort(minecraft, "the benchmark locations could not be found", failure);
            return;
        }
        Kinetic.LOGGER.info("Kinetic benchmark: spawn {}, scene sites {}", anchors.spawn(), anchors.sites());
        path = BenchmarkRoute.path(BenchmarkScene.values()[0], anchors);
        tick = 0;
        stage = Stage.WARMUP;
    }

    private void warmUp(Minecraft minecraft) {
        if (leftWorld(minecraft)) {
            return;
        }
        holdInputActive(minecraft);
        camera.place(minecraft.player, minecraft.level, path, 0);
        tick++;
        if (tick >= BenchmarkRoute.WARMUP_TICKS) {
            startScene(minecraft, 0);
        }
    }

    private void startScene(Minecraft minecraft, int index) {
        sceneIndex = index;
        BenchmarkScene scene = BenchmarkScene.values()[index];
        path = BenchmarkRoute.path(scene, anchors);
        tick = 0;
        camera.reset();
        settleMark = frames.frameTimes().recorded();
        site = anchors.site(scene);
        siteBuilt = !scene.setup().usesSite();
        pendingBuild = null;
        BenchmarkWorld.loadSite(minecraft.getSingleplayerServer(), scene, site);
        stage = Stage.SCENE;
        Kinetic.LOGGER.info("Kinetic benchmark: scene {} ({})", scene.id(), scene.description());
    }

    private void runScene(Minecraft minecraft) {
        if (leftWorld(minecraft)) {
            return;
        }
        holdInputActive(minecraft);
        LocalPlayer player = minecraft.player;
        camera.place(player, minecraft.level, path, tick);
        driveScene(minecraft.getSingleplayerServer());
        tick++;
        if (tick == BenchmarkRoute.SETTLE_TICKS) {
            warnIfSiteMissing();
            CullingStatistics.SHARED.takeCameraHiddenPercent();
            CullingStatistics.SHARED.takeFaceSkippedPercent();
            CullingStatistics.SHARED.takeShadowHiddenPercent();
            CullingStatistics.SHARED.takeCameraResults();
            logSettling(BenchmarkScene.values()[sceneIndex].id());
            measurement = new SceneMeasurement(BenchmarkScene.values()[sceneIndex], frames.frameTimes(), motion.errors(), FrameCostRecorder.SHARED, TickTimeRecorder.SERVER.mark(), gcPauses.mark(), System.nanoTime());
            lastVramBytes = vramProbe.get().usedBytes();
            meshMeter = MeshWorkers.meter(System.nanoTime());
            queuedSum = 0;
            queuedSamples = 0;
            ChunkUploadTimings.UPLOADS.take();
            ChunkUploadTimings.LOCK_WAITS.take();
            GpuStageTimer.setEnabled(gpuTimings);
            GpuStageTimer.take();
        }
        if (measurement != null) {
            sampleMemory();
            sampleBuildQueue(minecraft);
        }
        if (tick == BenchmarkRoute.SETTLE_TICKS + BenchmarkRoute.MEASURE_TICKS) {
            finishScene(minecraft);
        }
    }

    private void driveScene(IntegratedServer server) {
        BenchmarkScene scene = BenchmarkScene.values()[sceneIndex];
        if (!siteBuilt) {
            buildSite(server, scene);
            return;
        }
        BenchmarkWorld.tickScene(server, scene, site, tick);
        if (scene == BenchmarkScene.SERVER) {
            SimulatedPlayers.step(server, anchors.spawn(), tick);
        }
    }

    private void buildSite(IntegratedServer server, BenchmarkScene scene) {
        if (pendingBuild == null) {
            pendingBuild = BenchmarkWorld.buildWhenLoaded(server, scene, site);
            return;
        }
        if (pendingBuild.isDone()) {
            siteBuilt = pendingBuild.join();
            pendingBuild = null;
        }
    }

    private void warnIfSiteMissing() {
        if (!siteBuilt) {
            Kinetic.LOGGER.warn("Kinetic benchmark: the {} site at {} did not load while settling; the scene is measured without it",
                BenchmarkScene.values()[sceneIndex].id(), site);
        }
    }

    private void sampleMemory() {
        if (tick % VRAM_SAMPLE_INTERVAL_TICKS == 0) {
            lastVramBytes = vramProbe.get().usedBytes();
        }
        measurement.sampleMemory(HeapUsage.current().usedBytes(), lastVramBytes);
    }

    private void sampleBuildQueue(Minecraft minecraft) {
        SectionRenderDispatcher dispatcher = minecraft.levelRenderer.sectionRenderDispatcher();
        if (dispatcher != null) {
            queuedSum += dispatcher.getCompileQueueSize();
            queuedSamples++;
        }
    }

    private void logChunkBuilds(String scene) {
        StallMeter.Totals uploads = ChunkUploadTimings.UPLOADS.take();
        StallMeter.Totals waits = ChunkUploadTimings.LOCK_WAITS.take();
        Kinetic.LOGGER.info(String.format(Locale.ROOT,
            "Kinetic benchmark: %s chunk uploads %.2f ms average, %.1f ms longest; render thread waited for chunk builders %.0f ms in total, %.1f ms longest",
            scene, uploads.averageMs(), uploads.maxMs(), waits.totalMs(), waits.maxMs()));
        long averageQueued = queuedSamples == 0 ? 0 : Math.round((double) queuedSum / queuedSamples);
        if (!MeshWorkers.active()) {
            Kinetic.LOGGER.info("Kinetic benchmark: {} chunk build queue {} on average (vanilla workers)", scene, averageQueued);
            return;
        }
        JobLoadMeter.JobLoad load = meshMeter.take(System.nanoTime());
        Kinetic.LOGGER.info("Kinetic benchmark: {} chunk build queue {} on average, {} build jobs/s, {} mesh workers {}% busy",
            scene, averageQueued, Math.round(load.jobsPerSecond()), MeshWorkers.workers(), Math.round(load.busyShare() * 100.0));
    }

    private void logSettling(String scene) {
        long longest = LongStream.of(frames.frameTimes().since(settleMark)).max().orElse(0L);
        Kinetic.LOGGER.info(String.format(Locale.ROOT, "Kinetic benchmark: %s longest frame while settling %.1f ms", scene, Durations.millis(longest)));
    }

    private void logGpuStages(String scene) {
        if (!gpuTimings) {
            return;
        }
        Kinetic.LOGGER.info("Kinetic benchmark: {} GPU ms per frame: {}", scene, StageTimes.describe(GpuStageTimer.take()));
        GpuStageTimer.setEnabled(false);
    }

    private static void logCameraResults(String scene) {
        CameraResultCounts counts = CullingStatistics.SHARED.takeCameraResults();
        Kinetic.LOGGER.info("Kinetic benchmark: {} camera culling result missing in {} of {} frames (none {}, too old {}, eye out of reach {}), widened in {}",
            scene, counts.missing(), counts.frames(), counts.none(), counts.tooOld(), counts.outOfReach(), counts.widened());
    }

    private void finishScene(Minecraft minecraft) {
        long[] frameNanos = measurement.frameTimes(frames.frameTimes());
        long recordedFrames = measurement.framesRecordedSince(frames.frameTimes());
        long[] tickNanos = TickTimeRecorder.SERVER.since(measurement.tickMark());
        SceneResult result = measurement.finish(frameNanos, measurement.motionErrors(motion.errors()), measurement.frameCosts(FrameCostRecorder.SHARED), recordedFrames, tickNanos, gcPauses.since(measurement.gcMark()), System.nanoTime());
        results.add(result);
        allFrames.add(frameNanos);
        allTicks.add(tickNanos);
        measurement = null;
        Kinetic.LOGGER.info("Kinetic benchmark: {} average {} FPS, 1% low {} FPS, MSPT {} ms, occlusion culling hid {}% of sections and {}% of shadow casters, face culling skipped {}% of solid quads",
            result.scene(), Math.round(result.frames().averageFps()), Math.round(result.frames().onePercentLowFps()), Math.round(result.ticks().averageMs()),
            CullingStatistics.SHARED.takeCameraHiddenPercent(), CullingStatistics.SHARED.takeShadowHiddenPercent(), CullingStatistics.SHARED.takeFaceSkippedPercent());
        logChunkBuilds(result.scene());
        logCameraResults(result.scene());
        logGpuStages(result.scene());
        if (screenshots) {
            Screenshot.grab(minecraft.gameDirectory, "benchmark_" + result.scene() + ".png", minecraft.gameRenderer.mainRenderTarget(), SCREENSHOT_SCALE, message -> { });
        }
        BenchmarkWorld.clearSite(minecraft.getSingleplayerServer(), BenchmarkScene.values()[sceneIndex], site);
        if (sceneIndex + 1 < BenchmarkScene.values().length) {
            startScene(minecraft, sceneIndex + 1);
            return;
        }
        finish(minecraft);
    }

    private void finish(Minecraft minecraft) {
        BenchmarkEnvironment environment = BenchmarkEnvironmentProbe.capture(minecraft);
        end(minecraft);
        long[] overallFrames = concatenate(allFrames);
        long[] overallTicks = concatenate(allTicks);
        BenchmarkReport report = new BenchmarkReport(
            BenchmarkReport.FORMAT_VERSION,
            startedAt,
            environment,
            anchors,
            List.copyOf(results),
            FrameTimeSummary.of(overallFrames, overallFrames.length),
            TickSummary.of(overallTicks, overallTicks.length)
        );
        BenchmarkFiles files = new BenchmarkFiles(minecraft.gameDirectory.toPath().resolve(REPORT_FOLDER));
        jobs.submit(() -> writeReport(files, report));
    }

    private void writeReport(BenchmarkFiles files, BenchmarkReport report) {
        try {
            Optional<BenchmarkComparison> comparison = files.latest().map(previous -> BenchmarkComparison.between(previous, report));
            String text = BenchmarkText.format(report, comparison);
            Path written = files.write(report, text);
            Kinetic.LOGGER.info("Kinetic benchmark finished, report written to {}\n{}", written, text);
        } catch (IOException | RuntimeException failure) {
            Kinetic.LOGGER.error("Kinetic benchmark report could not be written", failure);
        }
        if (quitWhenDone) {
            mainThread.post(() -> Minecraft.getInstance().stop());
        }
    }

    private void abort(Minecraft minecraft, String reason, Exception failure) {
        end(minecraft);
        Kinetic.LOGGER.error("Kinetic benchmark stopped: {}", reason, failure);
    }

    private boolean leftWorld(Minecraft minecraft) {
        if (minecraft.player != null && minecraft.level != null && minecraft.getSingleplayerServer() != null) {
            return false;
        }
        end(minecraft);
        Kinetic.LOGGER.warn("Kinetic benchmark stopped: the player left the benchmark world");
        return true;
    }

    private void end(Minecraft minecraft) {
        stage = Stage.FINISHED;
        options.restore(minecraft.options);
        FrameRateGovernor.suspend(false);
        if (focusOverridden) {
            minecraft.options.pauseOnLostFocus = pauseOnLostFocus;
            focusOverridden = false;
        }
    }

    private static void holdInputActive(Minecraft minecraft) {
        minecraft.getFramerateLimitTracker().onInputReceived();
    }

    private static long[] concatenate(List<long[]> parts) {
        return parts.stream().flatMapToLong(LongStream::of).toArray();
    }

    private enum Stage {
        CREATE_WORLD,
        WAIT_FOR_PLAYER,
        LOCATE,
        WARMUP,
        SCENE,
        FINISHED
    }
}
