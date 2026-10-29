package com.aryston.kinetic.core.telemetry;

import jdk.jfr.Category;
import jdk.jfr.DataAmount;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;
import jdk.jfr.Timespan;

@Name("kinetic.Frame")
@Label("Kinetic Frame")
@Category("Kinetic")
@Description("One frame from its start to the start of the next frame, with the work that can stall it")
@StackTrace(false)
public final class FrameTraceEvent extends Event {
    @Label("Render")
    @Timespan(Timespan.NANOSECONDS)
    public long render;

    @Label("Submit And Present")
    @Timespan(Timespan.NANOSECONDS)
    public long present;

    @Label("Client Ticks")
    @Timespan(Timespan.NANOSECONDS)
    public long clientTicks;

    @Label("Queue Submit")
    @Timespan(Timespan.NANOSECONDS)
    public long queueSubmit;

    @Label("GPU Wait")
    @Timespan(Timespan.NANOSECONDS)
    public long gpuWait;

    @Label("Resource Destruction")
    @Timespan(Timespan.NANOSECONDS)
    public long destruction;

    @Label("Image Present")
    @Timespan(Timespan.NANOSECONDS)
    public long imagePresent;

    @Label("Image Acquire")
    @Timespan(Timespan.NANOSECONDS)
    public long imageAcquire;

    @Label("GPU Time Of An Earlier Frame")
    @Timespan(Timespan.NANOSECONDS)
    public long earlierGpuFrame;

    @Label("GPU Rendering Time Of An Earlier Frame")
    @Timespan(Timespan.NANOSECONDS)
    public long earlierGpuRendering;

    @Label("Chunk Lock Wait")
    @Timespan(Timespan.NANOSECONDS)
    public long chunkLockWait;

    @Label("Chunk Lock Waits")
    public int chunkLockWaits;

    @Label("Chunk Upload")
    @Timespan(Timespan.NANOSECONDS)
    public long chunkUpload;

    @Label("Culling On Render Thread")
    @Timespan(Timespan.NANOSECONDS)
    public long culling;

    @Label("Camera Result")
    public String cameraResult;

    @Label("Visible Sections")
    public int visibleSections;

    @Label("Hidden Sections")
    public int hiddenSections;

    @Label("Sections Written To The GPU Table")
    public int gpuSectionWrites;

    @Label("Light Updates Polled")
    public int lightUpdates;

    @Label("Chunk Packets")
    public int chunkPackets;

    @Label("Chunk Uploads Skipped")
    public int uploadsSkipped;

    @Label("Buffer Heaps Created")
    public int heapsCreated;

    @Label("Buffer Heaps Destroyed")
    public int heapsDestroyed;

    @Label("GPU Memory Allocations")
    public int gpuAllocations;

    @Label("GPU Memory Allocated")
    @DataAmount
    public long gpuAllocatedBytes;

    @Label("GPU Memory Allocation Time")
    @Timespan(Timespan.NANOSECONDS)
    public long gpuAllocationTime;

    @Label("GPU Memory Frees")
    public int gpuFrees;

    @Label("GPU Memory Free Time")
    @Timespan(Timespan.NANOSECONDS)
    public long gpuFreeTime;
}
