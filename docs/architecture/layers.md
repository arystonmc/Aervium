# Aervium Architecture: Layers

Part of the [Aervium architecture](../ARCHITECTURE.md).

## Layers

```
Minecraft and NeoForge
   │  events (RenderFrameEvent, ClientTickEvent, ServerTickEvent, GUI layers), mixins
   ▼
Adapter layer                 the only code that touches Minecraft or NeoForge
   integration/               both sides: server events, mod list checks
   client/                    client side: frame events, keys, overlay drawing, VRAM probe
   client/benchmark/          benchmark world, camera and environment
   client/pregeneration/      join hold, terrain readiness, compile order while joining
   mixin/                     accessors and hooks where no API exists
   config/                    NeoForge config spec
   ▼
Engine (game-independent, unit tested)
   core/telemetry             ring buffers, statistics, GC pauses, snapshots
   core/budget                frame budget, timed systems, timing scopes
   core/job                   work-stealing job system, main-thread queue
   core/culling               software depth raster, occluder boxes, occlusion passes
   core/pregeneration         terrain progress, join hold states, loading progress blend
   world/                     server-side engine parts (tick recorder now, TPS work in Phase 4)
   debug/route, debug/benchmark  camera routes, benchmark scenes, commands, reports
```

Engine code never imports `net.minecraft` or `net.neoforged` classes (Gson and SLF4J are allowed). Common code never references client classes.
