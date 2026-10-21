# Aervium Code Map: `com.aryston.aervium.core.budget`

Part of the [Aervium code map](../CODEMAP.md). Every class of this package has its entry here and nowhere else (rule D1).

### `com.aryston.aervium.core.budget`

#### TimedSystem
- Path: `src/main/java/com/aryston/aervium/core/budget/TimedSystem.java`
- Role: Every system with its own timing scope (frame, client tick, server tick, overlay, telemetry), its label and its share of the frame or tick budget.
- Depends on: nothing.

#### FrameBudget
- Path: `src/main/java/com/aryston/aervium/core/budget/FrameBudget.java`
- Role: Record of the target frame rate; gives the frame time and the budget of each `TimedSystem` (tick systems always get 50 ms).
- Depends on: `TimedSystem`, `Durations`.

#### LapTimer
- Path: `src/main/java/com/aryston/aervium/core/budget/LapTimer.java`
- Role: Lap timer on nanosecond time for `DeltaTrackerTimerPrecisionMixin`: the first lap is empty, later laps return the milliseconds since the previous lap with their fraction, and `elapsedMillis` reads the running lap without ending it.
- Depends on: `Durations`.

#### StallMeter
- Path: `src/main/java/com/aryston/aervium/core/budget/StallMeter.java`
- Role: Count, total and longest of a repeated render thread stall since the last `take`, for the benchmark log. Render thread only, no locking.
- Depends on: nothing.

#### SystemTimings
- Path: `src/main/java/com/aryston/aervium/core/budget/SystemTimings.java`
- Role: Allocation-free timing scopes: `begin()` and `end(system, start)` keep a smoothed time and a peak per system; `sample(budget)` reads and resets the peaks and flags systems over budget.
- Members:
  - `SHARED`: the instance every scope writes to.
- Depends on: `TimedSystem`, `FrameBudget`, `TelemetrySnapshot.SystemTiming`.
- Notes: Each system is written by one thread; readers accept slightly stale values.

#### FrameRatePolicy
- Path: `src/main/java/com/aryston/aervium/core/budget/FrameRatePolicy.java`
- Role: Dynamic frame rate: keeps the vanilla limit while the game is active and caps it while the window is in the background (`UNFOCUSED`) or the single player game is paused (`PAUSED`); never raises a lower vanilla limit.
- Depends on: nothing.
