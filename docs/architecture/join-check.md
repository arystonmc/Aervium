# Kinetic Architecture: Join Check

Part of the [Kinetic architecture](../ARCHITECTURE.md).

## Join Check

`-Dkinetic.joinCheck=true` measures a world join: it opens the save folder named by `kinetic.joinCheck.world`, creates a fresh world on the benchmark seed with `kinetic.joinCheck.newWorld=true`, or follows a join started by hand or by quick play. `JoinTimeline` sorts every frame into a phase by the screen on show (reading world data, loading data packs, progress, server, terrain, game), and the log prints each phase with its frames, longest frame and frames over 50 ms, the slowest frames with their phase, the chunks and visible sections missing at entry and every second after it, and frame rate and lows of the first 10 s in game. `kinetic.joinCheck.quit=true` closes the game afterwards and `kinetic.joinCheck.delay` sets the seconds to wait on the title screen (5); `kinetic.joinCheck.shots=true` saves a screenshot every 700 ms while loading and every 70 ms in the first 1.4 s in game (they slow those frames, so measure without them).
