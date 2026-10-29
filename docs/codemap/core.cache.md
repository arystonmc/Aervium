# Kinetic Code Map: `com.aryston.kinetic.core.cache`

Part of the [Kinetic code map](../CODEMAP.md). Every class of this package has its entry here and nowhere else (rule D1).

### `com.aryston.kinetic.core.cache`

#### ContentKey
- Path: `src/main/java/com/aryston/kinetic/core/cache/ContentKey.java`
- Role: SHA-256 key builder over separated text parts.
- Depends on: nothing.

#### DiskCache
- Path: `src/main/java/com/aryston/kinetic/core/cache/DiskCache.java`
- Role: One file per key in a folder; writes go through a uniquely named temporary file and an atomic move, so a crash never leaves half an entry. Keys address their content, so an existing entry is never rewritten, and a move blocked by another reader or writer (Windows reports it as access denied) is retried and treated as done once the entry exists.
- Depends on: nothing.

#### ShaderIncludes
- Path: `src/main/java/com/aryston/kinetic/core/cache/ShaderIncludes.java`
- Role: Finds `#include` and `#moj_import` targets of a shader source, directly and transitively (cycles stop once every include is seen).
- Depends on: nothing.

#### ShaderCacheKey
- Path: `src/main/java/com/aryston/kinetic/core/cache/ShaderCacheKey.java`
- Role: Cache key of one shader compilation: format version, game version, shader name and stage, source, every transitive include with its content, sorted defines and flags, and the device switches that change the compiled code.
- Depends on: `ContentKey`, `ShaderIncludes`.

#### SpirvBinary
- Path: `src/main/java/com/aryston/kinetic/core/cache/SpirvBinary.java`
- Role: Rejects cache entries that are not whole SPIR-V words or lack the SPIR-V magic number.
- Depends on: nothing.

#### PipelineCacheHeader
- Path: `src/main/java/com/aryston/kinetic/core/cache/PipelineCacheHeader.java`
- Role: Checks the Vulkan pipeline cache header (version one, vendor, device, pipeline cache UUID) before saved data is handed to the driver.
- Depends on: nothing.

#### CacheStatistics
- Path: `src/main/java/com/aryston/kinetic/core/cache/CacheStatistics.java`
- Role: Thread-safe hit, miss and work time counters for the startup report.
- Depends on: nothing.
