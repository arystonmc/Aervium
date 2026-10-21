# Aervium Code Map: `com.aryston.aervium.core.hardware`

Part of the [Aervium code map](../CODEMAP.md). Every class of this package has its entry here and nowhere else (rule D1).

### `com.aryston.aervium.core.hardware`

#### HardwareClassifier
- Path: `src/main/java/com/aryston/aervium/core/hardware/HardwareClassifier.java`
- Role: Sorts a `HardwareProfile` into `LOW`, `MEDIUM` or `HIGH`; each part (CPU threads, heap, RAM, GPU kind and video memory) can only lower the class, and unknown values never do. Software renderers are low, integrated GPUs at most medium.
- Depends on: `HardwareProfile`, `HardwareTier`, `GpuKind`.

#### HardwareTier
- Path: `src/main/java/com/aryston/aervium/core/hardware/HardwareTier.java`
- Role: What each class sets on first launch: render and simulation distance caps (8/6 low, 12/8 medium, none high) that only lower, and leaf culling on for low.
- Depends on: nothing.

#### HardwareProfile, GpuKind
- Path: `src/main/java/com/aryston/aervium/core/hardware/HardwareProfile.java`, `src/main/java/com/aryston/aervium/core/hardware/GpuKind.java`
- Role: The measured machine (CPU threads, max heap, physical RAM, GPU name, kind and device-local memory) and its log text.
- Depends on: nothing.
