# Aervium Architecture: Measuring

Part of the [Aervium architecture](../ARCHITECTURE.md).

## Measuring

Before and after an optimization:

1. Start the game with `-Define aervium.benchmark=true,aervium.benchmark.quit=true` (or run the two properties separately) with the same options and mods.
2. Run with and without the change in alternating order (off, on, off, on); one run says nothing about the lows (lesson L-105 of the workspace).
3. Keep the change only when both pairs move the same way, the lows and MSPT improve or stay equal and no scene loses more than the run-to-run noise. Count slow frames by cause with a JFR recording of `aervium.Frame` instead of reading only the lows.
