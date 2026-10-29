# Kinetic Architecture: Rules

Part of the [Kinetic architecture](../ARCHITECTURE.md).

## Rules

These rules decide every design question. A change that breaks one is not merged, whatever it gains.

1. **20 TPS always.** The server tick stays under 50 ms (MSPT). Server time is never traded for frame rate.
2. **Smoothness over peak FPS.** Success is a higher 1% low and 0.1% low and a steady frame time (low standard deviation, no spikes in the graph), measured with the benchmark. A change that raises the average but lowers a low is a regression.
3. **Vanilla behavior is identical.** Redstone timing, farms, mob behavior, world generation and saved data behave exactly as in vanilla. Optimizations change how fast something is computed, never what is computed.
4. **Clean room.** Sodium (PolyForm Shield) and Lithium (LGPL) are never read or copied. Known techniques are implemented from public descriptions (papers, talks, articles) only. Ports from permissive projects follow rule T1 and T2 of the workspace.
5. **One performance engine.** Kinetic replaces Sodium, so it refuses to load next to it (`SodiumClash`, a NeoForge loading error in the game language) except in the benchmark (`-Dkinetic.benchmark=true`), where every render optimization stays off so Sodium can be measured on the same route. With Iris installed every optimization stays off; measurement still works.
6. **Thin adapter.** Everything Minecraft-specific goes through the adapter layer, so a new Minecraft version only touches that layer.
7. **No allocation in the frame.** Work that runs every frame uses preallocated memory: pooled lists, packed int arrays and mapped GPU rings. Off-heap frame arenas (FFM) were planned for Phase 1 and are not built: with ZGC no profile so far showed frame time in allocation, so they wait until one does.
8. **Measure first.** Every optimization starts with a benchmark run before and after, on the same settings, and lands only when the benchmark shows it (see Measuring).
