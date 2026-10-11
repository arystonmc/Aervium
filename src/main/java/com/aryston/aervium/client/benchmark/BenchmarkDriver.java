package com.aryston.aervium.client.benchmark;

import com.aryston.aervium.Aervium;
import com.aryston.aervium.core.job.JobSystem;
import com.aryston.aervium.core.job.MainThreadQueue;
import com.aryston.aervium.core.telemetry.FrameTimeRecorder;
import com.aryston.aervium.core.telemetry.FrameTimeSummary;
import com.aryston.aervium.core.telemetry.GcPauseRecorder;
import com.aryston.aervium.core.telemetry.HeapUsage;
import com.aryston.aervium.core.telemetry.TickSummary;
import com.aryston.aervium.core.telemetry.VramUsage;
import com.aryston.aervium.debug.benchmark.BenchmarkAnchors;
import com.aryston.aervium.debug.benchmark.BenchmarkComparison;
import com.aryston.aervium.debug.benchmark.BenchmarkFiles;
import com.aryston.aervium.debug.benchmark.BenchmarkReport;
import com.aryston.aervium.debug.benchmark.BenchmarkRoute;
import com.aryston.aervium.debug.benchmark.BenchmarkScene;
import com.aryston.aervium.debug.benchmark.BenchmarkText;
import com.aryston.aervium.debug.benchmark.SceneMeasurement;
import com.aryston.aervium.debug.benchmark.SceneResult;
import com.aryston.aervium.debug.route.CameraPath;
import com.aryston.aervium.world.TickTimeRecorder;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.LongStream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.server.IntegratedServer;

public final class BenchmarkDriver {
    private static final String PROPERTY = "aervium.benchmark";
    private static final String QUIT_PROPERTY = "aervium.benchmark.quit";
    private static final String REPORT_FOLDER = "aervium/benchmarks";
    private static final DateTimeFormatter STARTED_AT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final int FRAME_CAPACITY = 1 << 17;
    private static final int VRAM_SAMPLE_INTERVAL_TICKS = 20;

    private final GcPauseRecorder gcPauses;
    private final JobSystem jobs;
    private final MainThreadQueue mainThread;
    private final Supplier<VramUsage> vramProbe;
    private final boolean quitWhenDone = Boolean.getBoolean(QUIT_PROPERTY);
    private final FrameTimeRecorder frames = new FrameTimeRecorder(FRAME_CAPACITY);
    private final BenchmarkCamera camera = new BenchmarkCamera();
    private final List<SceneResult> results = new ArrayList<>();
    private final List<long[]> allFrames = new ArrayList<>();
    private final List<long[]> allTicks = new ArrayList<>();
    private Stage stage = Stage.CREATE_WORLD;
    private CompletableFuture<BenchmarkAnchors> pendingAnchors;
    private BenchmarkAnchors anchors;
    private String startedAt;
    private boolean pauseOnLostFocus;
    private boolean focusOverridden;
    private int sceneIndex;
    private int tick;
    private CameraPath path;
    private SceneMeasurement measurement;
    private long lastVramBytes;

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
        try {
            BenchmarkWorld.createFresh(minecraft, minecraft.gui.screen());
            Aervium.LOGGER.info("Aervium benchmark: creating a fresh world with seed {}", BenchmarkWorld.SEED);
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
        Aervium.LOGGER.info("Aervium benchmark: spawn {}, village {} (found: {}), redstone {}", anchors.spawn(), anchors.village(), anchors.villageFound(), anchors.redstone());
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
        BenchmarkWorld.setUpScene(minecraft.getSingleplayerServer(), scene, anchors);
        stage = Stage.SCENE;
        Aervium.LOGGER.info("Aervium benchmark: scene {} ({})", scene.id(), scene.description());
    }

    private void runScene(Minecraft minecraft) {
        if (leftWorld(minecraft)) {
            return;
        }
        holdInputActive(minecraft);
        LocalPlayer player = minecraft.player;
        camera.place(player, minecraft.level, path, tick);
        tick++;
        if (tick == BenchmarkRoute.SETTLE_TICKS) {
            measurement = new SceneMeasurement(BenchmarkScene.values()[sceneIndex], frames.frameTimes(), TickTimeRecorder.SERVER.mark(), gcPauses.mark(), System.nanoTime());
            lastVramBytes = vramProbe.get().usedBytes();
        }
        if (measurement != null) {
            sampleMemory();
        }
        if (tick == BenchmarkRoute.SETTLE_TICKS + BenchmarkRoute.MEASURE_TICKS) {
            finishScene(minecraft);
        }
    }

    private void sampleMemory() {
        if (tick % VRAM_SAMPLE_INTERVAL_TICKS == 0) {
            lastVramBytes = vramProbe.get().usedBytes();
        }
        measurement.sampleMemory(HeapUsage.current().usedBytes(), lastVramBytes);
    }

    private void finishScene(Minecraft minecraft) {
        long[] frameNanos = measurement.frameTimes(frames.frameTimes());
        long recordedFrames = measurement.framesRecordedSince(frames.frameTimes());
        long[] tickNanos = TickTimeRecorder.SERVER.since(measurement.tickMark());
        SceneResult result = measurement.finish(frameNanos, recordedFrames, tickNanos, gcPauses.since(measurement.gcMark()), System.nanoTime());
        results.add(result);
        allFrames.add(frameNanos);
        allTicks.add(tickNanos);
        measurement = null;
        Aervium.LOGGER.info("Aervium benchmark: {} average {} FPS, 1% low {} FPS, MSPT {} ms",
            result.scene(), Math.round(result.frames().averageFps()), Math.round(result.frames().onePercentLowFps()), Math.round(result.ticks().averageMs()));
        if (sceneIndex + 1 < BenchmarkScene.values().length) {
            startScene(minecraft, sceneIndex + 1);
            return;
        }
        finish(minecraft);
    }

    private void finish(Minecraft minecraft) {
        end(minecraft);
        long[] overallFrames = concatenate(allFrames);
        long[] overallTicks = concatenate(allTicks);
        BenchmarkReport report = new BenchmarkReport(
            BenchmarkReport.FORMAT_VERSION,
            startedAt,
            BenchmarkEnvironmentProbe.capture(minecraft),
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
            Aervium.LOGGER.info("Aervium benchmark finished, report written to {}\n{}", written, text);
        } catch (IOException | RuntimeException failure) {
            Aervium.LOGGER.error("Aervium benchmark report could not be written", failure);
        }
        if (quitWhenDone) {
            mainThread.post(() -> Minecraft.getInstance().stop());
        }
    }

    private void abort(Minecraft minecraft, String reason, Exception failure) {
        end(minecraft);
        Aervium.LOGGER.error("Aervium benchmark stopped: {}", reason, failure);
    }

    private boolean leftWorld(Minecraft minecraft) {
        if (minecraft.player != null && minecraft.level != null && minecraft.getSingleplayerServer() != null) {
            return false;
        }
        end(minecraft);
        Aervium.LOGGER.warn("Aervium benchmark stopped: the player left the benchmark world");
        return true;
    }

    private void end(Minecraft minecraft) {
        stage = Stage.FINISHED;
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
