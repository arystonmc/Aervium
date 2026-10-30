# Kinetic Code Map: `com.aryston.kinetic.core.telemetry`

Part of the [Kinetic code map](../CODEMAP.md). Every class of this package has its entry here and nowhere else (rule D1).

### `com.aryston.kinetic.core.telemetry`

#### SampleRing
- Path: `src/main/java/com/aryston/kinetic/core/telemetry/SampleRing.java`
- Role: Fixed-capacity ring buffer of `long` samples (nanoseconds) for a single writer; recording never allocates.
- Members:
  - `record(long)`, `clear()`, `size()`, `recorded()`: write and count; `recorded` counts every sample ever written and serves as a mark.
  - `copyLatest(long[], int)`: copies the newest samples oldest first into an existing array without allocating.
  - `latest(int)`, `since(long mark)`: copies into a new array (for aggregation, not per frame).
- Depends on: nothing.

#### SampleStatistics
- Path: `src/main/java/com/aryston/kinetic/core/telemetry/SampleStatistics.java`
- Role: Record with count, mean, population standard deviation, min, max, median, p95, p99 and p99.9 of a sample.
- Members:
  - `ofSorting(long[], int)`: sorts the counted prefix in place and computes the statistics.
  - `percentile(long[], int, double)`: nearest-rank percentile of a sorted array.
- Depends on: nothing.

#### FrameTimeSummary
- Path: `src/main/java/com/aryston/kinetic/core/telemetry/FrameTimeSummary.java`
- Role: Frame time statistics for players: average FPS, 1% low (FPS at the 99th percentile frame time), 0.1% low (99.9th percentile), average, standard deviation, median, p99, p99.9, hitches (longer than twice the median and 8.33 ms), frames over 33 and 50 ms (counted before the samples are sorted), and maximum in milliseconds.
- Depends on: `SampleStatistics`, `Durations`.

#### MotionErrorRecorder
- Path: `src/main/java/com/aryston/kinetic/core/telemetry/MotionErrorRecorder.java`
- Role: Records the absolute difference between the simulated time step and real time between consecutive presented frames.
- Depends on: `SampleRing`, `Durations`.

#### MotionErrorSummary
- Path: `src/main/java/com/aryston/kinetic/core/telemetry/MotionErrorSummary.java`
- Role: Motion error statistics: sampled frames, average, p99 and maximum milliseconds.
- Depends on: `SampleStatistics`, `Durations`.

#### TickSummary
- Path: `src/main/java/com/aryston/kinetic/core/telemetry/TickSummary.java`
- Role: Server tick time statistics in milliseconds with the number and share of ticks over the 50 ms budget and a histogram (5, 10, 20, 30, 40, 50 ms and over).
- Depends on: `SampleStatistics`, `Durations`.

#### FrameCostRecorder
- Path: `src/main/java/com/aryston/kinetic/core/telemetry/FrameCostRecorder.java`
- Role: Shared CPU and GPU time per frame. The CPU time of a frame is the gap between two frame starts minus the time the render thread waited (for an earlier frame on the GPU, for a swapchain image, in `present` and in the frame limiter); the GPU time is the whole-frame timestamp pair of `GpuStageTimer`. Two `SampleRing`s of 131072 frames, written and read on the render thread.
- Depends on: `SampleRing`.

#### FrameCostSummary
- Path: `src/main/java/com/aryston/kinetic/core/telemetry/FrameCostSummary.java`
- Role: Average and p99 CPU and GPU milliseconds of a window of frames and which side limits the frame rate (`Limit`: CPU bound, GPU bound or no GPU timings); shown in the overlay and in every benchmark scene.
- Depends on: `SampleStatistics`, `Durations`.

#### FrameTimeRecorder
- Path: `src/main/java/com/aryston/kinetic/core/telemetry/FrameTimeRecorder.java`
- Role: Records the time between consecutive frame starts into a `SampleRing`; `reset()` forgets the gap to the next frame (world joins).
- Depends on: `SampleRing`.

#### GcPauseRecorder
- Path: `src/main/java/com/aryston/kinetic/core/telemetry/GcPauseRecorder.java`
- Role: Listens to the garbage collector notifications of the JVM and records stop-the-world pause durations.
- Members:
  - `install()`: registers the listener on every collector bean.
  - `mark()`, `since(long)`: pauses after a mark; `total()`: every pause since start.
- Depends on: `GcPauseClassifier`, `SampleRing`, `GcPauseTotals`, `Durations`.
- Notes: Notifications arrive on a JMX thread, so every method is synchronized.

#### GcPauseClassifier
- Path: `src/main/java/com/aryston/kinetic/core/telemetry/GcPauseClassifier.java`
- Role: Tells pauses from concurrent collector cycles by collector name and action (ZGC and Shenandoah report cycles separately).
- Depends on: nothing.

#### GarbageCollector
- Path: `src/main/java/com/aryston/kinetic/core/telemetry/GarbageCollector.java`
- Role: Enum of the collectors (ZGC, Shenandoah, G1, Parallel, Serial), recognized from the collector bean names; `lowLatency()` marks the concurrent ones.
- Members: `current()`: the collector of this JVM; `fromBeanNames(Collection)`.
- Depends on: nothing.

#### GcAdvice
- Path: `src/main/java/com/aryston/kinetic/core/telemetry/GcAdvice.java`
- Role: Advice on launcher JVM flags (low-pause collector, heap limit) for the startup log; a mod cannot change the collector of the running JVM.
- Members: `forThisJvm()`; `advise(GarbageCollector, long, long)`: empty when nothing should change.
- Depends on: `GarbageCollector`.

#### GcPauseTotals
- Path: `src/main/java/com/aryston/kinetic/core/telemetry/GcPauseTotals.java`
- Role: Record of pause count, total and longest pause in milliseconds.
- Depends on: nothing.

#### HeapUsage
- Path: `src/main/java/com/aryston/kinetic/core/telemetry/HeapUsage.java`
- Role: Record of used and maximum Java heap; `current()` reads the runtime.
- Depends on: nothing.

#### VramUsage
- Path: `src/main/java/com/aryston/kinetic/core/telemetry/VramUsage.java`
- Role: Record of used video memory and its budget; `UNAVAILABLE` when no Vulkan allocator is reachable.
- Depends on: nothing.

#### TelemetrySnapshot
- Path: `src/main/java/com/aryston/kinetic/core/telemetry/TelemetrySnapshot.java`
- Role: Everything the overlay shows at one moment: frame summary, CPU and GPU time per frame, optional server tick summary, GC pauses, heap, VRAM and the timing of every `TimedSystem` (`SystemTiming`).
- Depends on: `FrameTimeSummary`, `FrameCostSummary`, `TickSummary`, `GcPauseTotals`, `HeapUsage`, `VramUsage`, `TimedSystem`.

#### Durations
- Path: `src/main/java/com/aryston/kinetic/core/telemetry/Durations.java`
- Role: Nanosecond conversions and the 50 ms tick budget.
- Depends on: nothing.

#### ByteSizes
- Path: `src/main/java/com/aryston/kinetic/core/telemetry/ByteSizes.java`
- Role: Bytes to megabytes.
- Depends on: nothing.

#### FrameTraceEvent
- Path: `src/main/java/com/aryston/kinetic/core/telemetry/FrameTraceEvent.java`
- Role: JFR event `kinetic.Frame` for one frame, from `RenderFrameEvent.Pre` to the next one: render, submit and present, client ticks, the parts of submitting (queue submit, waiting for an earlier frame on the GPU, resource destruction, image present and acquire), GPU times of an earlier frame (whole frame and rendering before the swapchain copy), chunk lock wait, uploads, culling, camera result, sections, GPU table writes, light updates, chunk packets, skipped uploads, buffer heaps created or destroyed and Vulkan memory allocations and releases with their time. Off unless a recording enables it, so normal play pays nothing.
- Depends on: nothing.

#### StageTimes
- Path: `src/main/java/com/aryston/kinetic/core/telemetry/StageTimes.java`
- Role: Named stages (up to 128) with total and longest time over the measured frames; `take` returns the averages per frame and resets.
- Depends on: nothing.
