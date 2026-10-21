# Aervium Code Map: `com.aryston.aervium.config`

Part of the [Aervium code map](../CODEMAP.md). Every class of this package has its entry here and nowhere else (rule D1).

### `com.aryston.aervium.config`

#### AerviumConfig
- Path: `src/main/java/com/aryston/aervium/config/AerviumConfig.java`
- Role: Client config: Aervium details on the F3 screen (on), the target frame rate of the frame budget, dynamic frame rate (on by default, 30 FPS in the background, 60 FPS while paused), occlusion culling (on), leaf culling (off), pregeneration on join (on) and its radius (32 chunks, 6 to 32), distant terrain (on) and its distance (64 to 512 chunks, default 256).
- Depends on: `FrameBudget`, `FrameRatePolicy`, `SpawnPregenerationSettings`, `DistantTerrainLimits`.
