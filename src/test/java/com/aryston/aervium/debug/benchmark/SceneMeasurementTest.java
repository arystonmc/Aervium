package com.aryston.aervium.debug.benchmark;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.aryston.aervium.core.telemetry.GcPauseTotals;
import com.aryston.aervium.core.telemetry.SampleRing;
import org.junit.jupiter.api.Test;

class SceneMeasurementTest {
    private static final double EPSILON = 1e-9;
    private static final long MEGABYTE = 1024L * 1024L;

    @Test
    void measuresOnlyFramesAfterTheSceneStarted() {
        SampleRing frames = new SampleRing(16);
        frames.record(99_000_000L);
        SceneMeasurement measurement = new SceneMeasurement(BenchmarkScene.WALK, frames, new SampleRing(1), 0, 0, 0);
        frames.record(10_000_000L);
        frames.record(10_000_000L);
        measurement.sampleMemory(512 * MEGABYTE, 128 * MEGABYTE);
        measurement.sampleMemory(256 * MEGABYTE, 300 * MEGABYTE);
        long[] frameNanos = measurement.frameTimes(frames);
        SceneResult result = measurement.finish(frameNanos, new long[0], measurement.framesRecordedSince(frames), new long[] {5_000_000L}, new GcPauseTotals(0, 0.0, 0.0), 2_000_000_000L);
        assertEquals("walk", result.scene());
        assertEquals(2, result.frames().frames());
        assertEquals(100.0, result.frames().averageFps(), EPSILON);
        assertEquals(0, result.droppedFrames());
        assertEquals(1, result.ticks().ticks());
        assertEquals(2.0, result.seconds(), EPSILON);
        assertEquals(512.0, result.peakHeapMb(), EPSILON);
        assertEquals(300.0, result.peakVramMb(), EPSILON);
    }

    @Test
    void countsFramesThatFellOutOfTheRing() {
        SampleRing frames = new SampleRing(4);
        SceneMeasurement measurement = new SceneMeasurement(BenchmarkScene.FLIGHT, frames, new SampleRing(1), 0, 0, 0);
        for (int i = 0; i < 10; i++) {
            frames.record(5_000_000L);
        }
        long[] frameNanos = measurement.frameTimes(frames);
        SceneResult result = measurement.finish(frameNanos, new long[0], measurement.framesRecordedSince(frames), new long[0], new GcPauseTotals(0, 0.0, 0.0), 1L);
        assertEquals(4, result.frames().frames());
        assertEquals(6, result.droppedFrames());
    }
}
