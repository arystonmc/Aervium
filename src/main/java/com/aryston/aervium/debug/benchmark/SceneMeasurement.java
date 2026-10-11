package com.aryston.aervium.debug.benchmark;

import com.aryston.aervium.core.telemetry.ByteSizes;
import com.aryston.aervium.core.telemetry.Durations;
import com.aryston.aervium.core.telemetry.FrameTimeSummary;
import com.aryston.aervium.core.telemetry.GcPauseTotals;
import com.aryston.aervium.core.telemetry.SampleRing;
import com.aryston.aervium.core.telemetry.TickSummary;

public final class SceneMeasurement {
    private final BenchmarkScene scene;
    private final long frameMark;
    private final long tickMark;
    private final long gcMark;
    private final long startNanos;
    private long peakHeapBytes;
    private long peakVramBytes;

    public SceneMeasurement(BenchmarkScene scene, SampleRing frames, long tickMark, long gcMark, long startNanos) {
        this.scene = scene;
        this.frameMark = frames.recorded();
        this.tickMark = tickMark;
        this.gcMark = gcMark;
        this.startNanos = startNanos;
    }

    public long tickMark() {
        return tickMark;
    }

    public long gcMark() {
        return gcMark;
    }

    public void sampleMemory(long heapBytes, long vramBytes) {
        peakHeapBytes = Math.max(peakHeapBytes, heapBytes);
        peakVramBytes = Math.max(peakVramBytes, vramBytes);
    }

    public long[] frameTimes(SampleRing frames) {
        return frames.since(frameMark);
    }

    public long framesRecordedSince(SampleRing frames) {
        return frames.recorded() - frameMark;
    }

    public SceneResult finish(long[] frameNanos, long recordedFrames, long[] tickNanos, GcPauseTotals gcPauses, long endNanos) {
        long droppedFrames = Math.max(0, recordedFrames - frameNanos.length);
        return new SceneResult(
            scene.id(),
            scene.description(),
            (endNanos - startNanos) / Durations.NANOS_PER_SECOND,
            FrameTimeSummary.of(frameNanos, frameNanos.length),
            droppedFrames,
            TickSummary.of(tickNanos, tickNanos.length),
            gcPauses,
            ByteSizes.megabytes(peakHeapBytes),
            ByteSizes.megabytes(peakVramBytes)
        );
    }
}
