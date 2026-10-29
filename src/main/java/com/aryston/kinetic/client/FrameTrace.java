package com.aryston.kinetic.client;

import com.aryston.kinetic.core.culling.CameraResult;
import com.aryston.kinetic.core.telemetry.FrameTraceEvent;
import jdk.jfr.EventType;
import org.jspecify.annotations.Nullable;

public final class FrameTrace {
    private static final EventType TYPE = EventType.getEventType(FrameTraceEvent.class);
    private static final String NO_RESULT = "none";

    private static @Nullable FrameTraceEvent open;
    private static long renderStart;
    private static long renderEnd;
    private static long render;
    private static long present;
    private static long tickStart;
    private static long clientTicks;
    private static long queueSubmit;
    private static long gpuWait;
    private static long destruction;
    private static long imagePresent;
    private static long imageAcquire;
    private static long earlierGpuFrame;
    private static long earlierGpuRendering;
    private static long chunkLockWait;
    private static int chunkLockWaits;
    private static long chunkUpload;
    private static long culling;
    private static @Nullable CameraResult cameraResult;
    private static int visibleSections;
    private static int hiddenSections;
    private static int gpuSectionWrites;
    private static int lightUpdates;
    private static int chunkPackets;
    private static int uploadsSkipped;
    private static int heapsCreated;
    private static int heapsDestroyed;
    private static int gpuAllocations;
    private static long gpuAllocatedBytes;
    private static long gpuAllocationTime;
    private static int gpuFrees;
    private static long gpuFreeTime;

    private FrameTrace() {
    }

    static void frameStarted() {
        FrameTraceEvent previous = open;
        if (previous != null) {
            previous.end();
            if (previous.shouldCommit()) {
                fill(previous);
                previous.commit();
            }
        }
        reset();
        if (TYPE.isEnabled()) {
            FrameTraceEvent next = new FrameTraceEvent();
            next.begin();
            open = next;
        } else {
            open = null;
        }
    }

    static void renderStarted() {
        renderStart = System.nanoTime();
    }

    static void renderFinished() {
        renderEnd = System.nanoTime();
        render = renderEnd - renderStart;
    }

    static void presented() {
        present = System.nanoTime() - renderEnd;
    }

    static void tickStarted() {
        tickStart = System.nanoTime();
    }

    static void tickFinished() {
        clientTicks += System.nanoTime() - tickStart;
    }

    public static void queueSubmitted(long nanos) {
        queueSubmit += nanos;
    }

    public static void gpuWaited(long nanos) {
        gpuWait += nanos;
    }

    public static void resourcesDestroyed(long nanos) {
        destruction += nanos;
    }

    public static void imagePresented(long nanos) {
        imagePresent += nanos;
    }

    public static void imageAcquired(long nanos) {
        imageAcquire += nanos;
    }

    public static void earlierGpuFrameMeasured(long nanos) {
        earlierGpuFrame = nanos;
    }

    public static void earlierGpuRenderingMeasured(long nanos) {
        earlierGpuRendering = nanos;
    }

    public static void chunkLockWaited(long nanos) {
        chunkLockWait += nanos;
        chunkLockWaits++;
    }

    public static void chunkUploaded(long nanos) {
        chunkUpload += nanos;
    }

    public static void culled(long nanos) {
        culling += nanos;
    }

    public static void cameraResult(CameraResult result, int visible, int hidden) {
        cameraResult = result;
        visibleSections = visible;
        hiddenSections = hidden;
    }

    public static void gpuSectionWritten() {
        gpuSectionWrites++;
    }

    public static void lightUpdated() {
        lightUpdates++;
    }

    public static void chunkPacketHandled() {
        chunkPackets++;
    }

    public static void uploadSkipped() {
        uploadsSkipped++;
    }

    public static void bufferHeapCreated() {
        heapsCreated++;
    }

    public static void bufferHeapDestroyed() {
        heapsDestroyed++;
    }

    public static void gpuMemoryCreated(long bytes, long nanos) {
        gpuAllocations++;
        gpuAllocatedBytes += bytes;
        gpuAllocationTime += nanos;
    }

    public static void gpuMemoryDestroyed(long nanos) {
        gpuFrees++;
        gpuFreeTime += nanos;
    }

    private static void fill(FrameTraceEvent event) {
        event.render = render;
        event.present = present;
        event.clientTicks = clientTicks;
        event.queueSubmit = queueSubmit;
        event.gpuWait = gpuWait;
        event.destruction = destruction;
        event.imagePresent = imagePresent;
        event.imageAcquire = imageAcquire;
        event.earlierGpuFrame = earlierGpuFrame;
        event.earlierGpuRendering = earlierGpuRendering;
        event.chunkLockWait = chunkLockWait;
        event.chunkLockWaits = chunkLockWaits;
        event.chunkUpload = chunkUpload;
        event.culling = culling;
        event.cameraResult = cameraResult == null ? NO_RESULT : cameraResult.name();
        event.visibleSections = visibleSections;
        event.hiddenSections = hiddenSections;
        event.gpuSectionWrites = gpuSectionWrites;
        event.lightUpdates = lightUpdates;
        event.chunkPackets = chunkPackets;
        event.uploadsSkipped = uploadsSkipped;
        event.heapsCreated = heapsCreated;
        event.heapsDestroyed = heapsDestroyed;
        event.gpuAllocations = gpuAllocations;
        event.gpuAllocatedBytes = gpuAllocatedBytes;
        event.gpuAllocationTime = gpuAllocationTime;
        event.gpuFrees = gpuFrees;
        event.gpuFreeTime = gpuFreeTime;
    }

    private static void reset() {
        render = 0L;
        present = 0L;
        clientTicks = 0L;
        queueSubmit = 0L;
        gpuWait = 0L;
        destruction = 0L;
        imagePresent = 0L;
        imageAcquire = 0L;
        earlierGpuFrame = 0L;
        earlierGpuRendering = 0L;
        chunkLockWait = 0L;
        chunkLockWaits = 0;
        chunkUpload = 0L;
        culling = 0L;
        cameraResult = null;
        visibleSections = 0;
        hiddenSections = 0;
        gpuSectionWrites = 0;
        lightUpdates = 0;
        chunkPackets = 0;
        uploadsSkipped = 0;
        heapsCreated = 0;
        heapsDestroyed = 0;
        gpuAllocations = 0;
        gpuAllocatedBytes = 0L;
        gpuAllocationTime = 0L;
        gpuFrees = 0;
        gpuFreeTime = 0L;
    }
}
