# Kinetic Code Map: `com.aryston.kinetic.core.job`

Part of the [Kinetic code map](../CODEMAP.md). Every class of this package has its entry here and nowhere else (rule D1).

### `com.aryston.kinetic.core.job`

#### JobSystem
- Path: `src/main/java/com/aryston/kinetic/core/job/JobSystem.java`
- Role: Work-stealing worker pool (`ForkJoinPool` in async mode) with named daemon threads, below normal priority unless a priority is given; `defaultWorkers` leaves two cores to the game threads and caps at eight. `service()` is an `ExecutorService` view whose jobs are counted and timed (`timedJobs`, `busyNanos`).
- Depends on: nothing.

#### RegionLocks
- Path: `src/main/java/com/aryston/kinetic/core/job/RegionLocks.java`
- Role: Square regions of grid cells that threads hold exclusively: `acquire` waits until no cell of the region is held by another holder and takes them all at once (so two holders never deadlock), `release` frees them and wakes the waiters.
- Depends on: nothing.

#### JobLoadMeter
- Path: `src/main/java/com/aryston/kinetic/core/job/JobLoadMeter.java`
- Role: Jobs per second and the busy share of all workers of a `JobSystem` since the last `take`; each reader (overlay, benchmark) keeps its own meter.
- Depends on: `JobSystem`.

#### MainThreadQueue
- Path: `src/main/java/com/aryston/kinetic/core/job/MainThreadQueue.java`
- Role: Results of jobs that must be applied on the main thread; `drain(budget)` runs them in order until the time budget is spent (at least one per call).
- Depends on: nothing.
