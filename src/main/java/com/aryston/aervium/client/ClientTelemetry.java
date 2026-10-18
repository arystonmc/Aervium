package com.aryston.aervium.client;

import com.aryston.aervium.Aervium;
import com.aryston.aervium.core.budget.FrameBudget;
import com.aryston.aervium.core.budget.SystemTimings;
import com.aryston.aervium.core.budget.TimedSystem;
import com.aryston.aervium.core.job.JobSystem;
import com.aryston.aervium.core.job.MainThreadQueue;
import com.aryston.aervium.core.telemetry.FrameTimeRecorder;
import com.aryston.aervium.core.telemetry.FrameTimeSummary;
import com.aryston.aervium.core.telemetry.GcPauseRecorder;
import com.aryston.aervium.core.telemetry.GcPauseTotals;
import com.aryston.aervium.core.telemetry.HeapUsage;
import com.aryston.aervium.core.telemetry.MotionErrorRecorder;
import com.aryston.aervium.core.telemetry.MotionErrorSummary;
import com.aryston.aervium.core.telemetry.SampleRing;
import com.aryston.aervium.core.telemetry.TelemetrySnapshot;
import com.aryston.aervium.core.telemetry.TelemetrySnapshot.SystemTiming;
import com.aryston.aervium.core.telemetry.TickSummary;
import com.aryston.aervium.core.telemetry.VramUsage;
import com.aryston.aervium.world.TickTimeRecorder;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

final class ClientTelemetry implements AutoCloseable {
    private static final int FRAME_WINDOW = 2048;
    private static final int TICK_WINDOW = 200;
    private static final int AGGREGATION_INTERVAL_TICKS = 5;
    private static final long MAIN_THREAD_BUDGET_NANOS = 500_000L;
    private static final String JOB_SYSTEM_NAME = "Aervium";

    private final FrameTimeRecorder frames = new FrameTimeRecorder(FRAME_WINDOW);
    private final MotionErrorRecorder motion = new MotionErrorRecorder(FRAME_WINDOW);
    private final GcPauseRecorder gcPauses = GcPauseRecorder.install();
    private final JobSystem jobs = new JobSystem(
        JOB_SYSTEM_NAME,
        JobSystem.defaultWorkers(Runtime.getRuntime().availableProcessors()),
        (thread, failure) -> Aervium.LOGGER.error("Aervium job failed on {}", thread.getName(), failure)
    );
    private final MainThreadQueue mainThread = new MainThreadQueue(System::nanoTime);
    private int ticksUntilAggregation;
    private boolean aggregationPending;

    ClientTelemetry() {
        Aervium.LOGGER.info("Aervium job system started with {} workers", jobs.workers());
    }

    void frameStarted(long nanoTime) {
        frames.frameStarted(nanoTime);
    }

    void motionSampled(long nanoTime, double simulatedMillis) {
        motion.framePresented(nanoTime, simulatedMillis);
    }

    void motionInterrupted() {
        motion.interrupted();
    }

    void frameFinished() {
        mainThread.drain(MAIN_THREAD_BUDGET_NANOS);
    }

    void resetFrames() {
        frames.reset();
    }

    SampleRing frameTimes() {
        return frames.frameTimes();
    }

    GcPauseRecorder gcPauses() {
        return gcPauses;
    }

    JobSystem jobs() {
        return jobs;
    }

    MainThreadQueue mainThread() {
        return mainThread;
    }

    void clientTick(FrameBudget budget, Consumer<TelemetrySnapshot> publisher) {
        ticksUntilAggregation--;
        if (ticksUntilAggregation > 0 || aggregationPending) {
            return;
        }
        ticksUntilAggregation = AGGREGATION_INTERVAL_TICKS;
        aggregationPending = true;
        long[] frameNanos = frames.frameTimes().latest(FRAME_WINDOW);
        long[] motionErrorNanos = motion.errors().latest(FRAME_WINDOW);
        Optional<long[]> tickNanos = TickTimeRecorder.SERVER.isRunning()
            ? Optional.of(TickTimeRecorder.SERVER.latest(TICK_WINDOW))
            : Optional.empty();
        RawTelemetry raw = new RawTelemetry(frameNanos, motionErrorNanos, tickNanos, gcPauses.total(), HeapUsage.current(), VulkanMemoryProbe.sample(), SystemTimings.SHARED.sample(budget));
        jobs.submit(() -> {
            Optional<TelemetrySnapshot> snapshot = summarize(raw);
            mainThread.post(() -> {
                aggregationPending = false;
                snapshot.ifPresent(publisher);
            });
        });
    }

    private static Optional<TelemetrySnapshot> summarize(RawTelemetry raw) {
        long start = SystemTimings.begin();
        try {
            return Optional.of(new TelemetrySnapshot(
                FrameTimeSummary.of(raw.frameNanos(), raw.frameNanos().length),
                MotionErrorSummary.of(raw.motionErrorNanos(), raw.motionErrorNanos().length),
                raw.tickNanos().map(ticks -> TickSummary.of(ticks, ticks.length)),
                raw.gcPauses(),
                raw.heap(),
                raw.vram(),
                raw.timings()
            ));
        } catch (RuntimeException failure) {
            Aervium.LOGGER.error("Aervium could not summarize telemetry", failure);
            return Optional.empty();
        } finally {
            SystemTimings.SHARED.end(TimedSystem.TELEMETRY, start);
        }
    }

    @Override
    public void close() {
        jobs.close();
    }

    private record RawTelemetry(long[] frameNanos, long[] motionErrorNanos, Optional<long[]> tickNanos, GcPauseTotals gcPauses, HeapUsage heap, VramUsage vram, List<SystemTiming> timings) {
    }
}
