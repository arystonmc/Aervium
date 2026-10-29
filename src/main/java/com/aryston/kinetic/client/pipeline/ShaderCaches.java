package com.aryston.kinetic.client.pipeline;

import com.aryston.kinetic.Kinetic;
import com.aryston.kinetic.core.cache.CacheStatistics;
import com.aryston.kinetic.core.cache.DiskCache;
import com.aryston.kinetic.core.job.JobSystem;
import com.aryston.kinetic.core.telemetry.Durations;
import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.resources.Identifier;

public final class ShaderCaches {
    private static final String SPIRV_FOLDER = "spirv";
    private static final String PIPELINE_FILE = "vulkan-pipelines.bin";
    private static final long LATE_COMPILE_REPORT_NANOS = 1_000_000L;
    private static final double MILLIS_PER_SECOND = 1000.0;
    private static final int KILOBYTE = 1024;

    private static volatile ShaderCaches installed;

    private final SpirvCache spirv;
    private final VulkanPipelineCache pipelines;
    private final CacheStatistics lateCompiles = new CacheStatistics();

    private ShaderCaches(Path cacheFolder, JobSystem jobs) {
        this.spirv = new SpirvCache(new DiskCache(cacheFolder.resolve(SPIRV_FOLDER)), jobs);
        this.pipelines = new VulkanPipelineCache(cacheFolder.resolve(PIPELINE_FILE));
    }

    public static void install(Path cacheFolder, JobSystem jobs) {
        installed = new ShaderCaches(cacheFolder, jobs);
    }

    public static Optional<SpirvCache> spirv() {
        return Optional.ofNullable(installed).map(caches -> caches.spirv);
    }

    public static Optional<VulkanPipelineCache> pipelines() {
        return Optional.ofNullable(installed).map(caches -> caches.pipelines);
    }

    public static void recordLateCompile(Identifier pipeline, long nanos) {
        ShaderCaches caches = installed;
        if (caches == null) {
            return;
        }
        caches.lateCompiles.miss(nanos);
        if (nanos >= LATE_COMPILE_REPORT_NANOS) {
            Kinetic.LOGGER.info("Kinetic: pipeline {} was compiled on the render thread in {} ms", pipeline, Math.round(Durations.millis(nanos)));
        }
    }

    public static void reportStartup() {
        ShaderCaches caches = installed;
        if (caches == null) {
            return;
        }
        double seconds = (System.currentTimeMillis() - ManagementFactory.getRuntimeMXBean().getStartTime()) / MILLIS_PER_SECOND;
        CacheStatistics shaders = caches.spirv.statistics();
        CacheStatistics created = caches.pipelines.statistics();
        Kinetic.LOGGER.info(
            "Kinetic startup: game ready after {} s; shaders {} from cache and {} compiled ({} ms of worker time); {} Vulkan pipelines created in {} ms with {} KB of cached pipelines",
            String.format(Locale.ROOT, "%.1f", seconds),
            shaders.hits(),
            shaders.misses(),
            Math.round(Durations.millis(shaders.workNanos())),
            created.misses(),
            Math.round(Durations.millis(created.workNanos())),
            caches.pipelines.loadedBytes() / KILOBYTE
        );
    }

    public static void shutdown() {
        ShaderCaches caches = installed;
        if (caches != null) {
            Kinetic.LOGGER.info("Kinetic: {} pipelines were compiled late on the render thread this session ({} ms)",
                caches.lateCompiles.misses(), Math.round(Durations.millis(caches.lateCompiles.workNanos())));
            caches.pipelines.saveAndClose();
        }
    }
}
