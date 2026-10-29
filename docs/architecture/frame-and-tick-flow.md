# Kinetic Architecture: Frame and Tick Flow

Part of the [Kinetic architecture](../ARCHITECTURE.md).

## Frame and Tick Flow

```
RenderFrameEvent.Pre   frame start: FrameTimeRecorder (overlay), BenchmarkDriver (when running), FRAME scope begins
GameRenderer.render    vanilla, Helion or later Kinetic draw path
GUI layers             TelemetryOverlay draws text and the frame graph (OVERLAY scope)
RenderFrameEvent.Post  FRAME scope ends, MainThreadQueue drains job results within 0.5 ms

ClientTickEvent.Pre/Post   CLIENT_TICK scope; every 5 ticks ClientTelemetry copies raw data and hands it to a worker
ServerTickEvent.Pre/Post   TickTimeRecorder: one MSPT sample per tick, SERVER_TICK scope
GC notifications           GcPauseRecorder on the JMX thread
```

Raw data is copied on the thread that writes it, statistics are computed on a worker (`TELEMETRY` scope), and results come back through `MainThreadQueue`. The overlay itself only reads a preallocated array and cached strings.
