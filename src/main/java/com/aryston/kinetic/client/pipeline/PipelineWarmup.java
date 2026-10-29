package com.aryston.kinetic.client.pipeline;

import com.aryston.kinetic.Kinetic;
import com.aryston.kinetic.core.job.JobSystem;
import com.aryston.kinetic.core.job.MainThreadQueue;
import com.aryston.kinetic.mixin.PipelineCacheAccessor;
import com.mojang.blaze3d.pipeline.PipelineCache;
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.minecraft.client.renderer.RenderPipelines;

public final class PipelineWarmup {
    private static volatile PipelineWarmup installed;

    private final JobSystem jobs;
    private final MainThreadQueue mainThread;
    private PipelineCache current;

    private PipelineWarmup(JobSystem jobs, MainThreadQueue mainThread) {
        this.jobs = jobs;
        this.mainThread = mainThread;
    }

    public static void install(JobSystem jobs, MainThreadQueue mainThread) {
        installed = new PipelineWarmup(jobs, mainThread);
    }

    public static void cacheInstalled(PipelineCache cache) {
        PipelineWarmup warmup = installed;
        if (warmup != null) {
            warmup.warmUp(cache);
        }
    }

    private void warmUp(PipelineCache cache) {
        current = cache;
        PipelineCacheAccessor accessor = (PipelineCacheAccessor) cache;
        List<RenderPipeline> missing = unregisteredBuiltInPipelines().stream()
            .filter(pipeline -> !accessor.kinetic$compiled().containsKey(pipeline))
            .toList();
        if (missing.isEmpty()) {
            return;
        }
        Kinetic.LOGGER.info("Kinetic compiles {} built-in pipelines ahead of their first use", missing.size());
        missing.forEach(pipeline -> accessor.kinetic$device()
            .compilePipeline(pipeline, accessor.kinetic$shaderSource(), jobs::submit)
            .thenAccept(pending -> mainThread.post(() -> insert(cache, pipeline, pending))));
    }

    private void insert(PipelineCache cache, RenderPipeline pipeline, CompiledRenderPipeline.Pending pending) {
        CompiledRenderPipeline compiled = pending.finishCompile();
        if (compiled == null) {
            return;
        }
        PipelineCacheAccessor accessor = (PipelineCacheAccessor) cache;
        if (cache != current || accessor.kinetic$compiled().containsKey(pipeline)) {
            compiled.close();
            return;
        }
        cache.insert(pipeline, compiled);
    }

    private static List<RenderPipeline> unregisteredBuiltInPipelines() {
        Set<RenderPipeline> registered = Collections.newSetFromMap(new IdentityHashMap<>());
        registered.addAll(RenderPipelines.requiredPipelines());
        registered.addAll(RenderPipelines.optionalPipelines());
        return Arrays.stream(RenderPipelines.class.getFields())
            .filter(field -> Modifier.isStatic(field.getModifiers()) && field.getType() == RenderPipeline.class)
            .map(PipelineWarmup::read)
            .filter(Objects::nonNull)
            .filter(pipeline -> !registered.contains(pipeline))
            .toList();
    }

    private static RenderPipeline read(Field field) {
        try {
            return (RenderPipeline) field.get(null);
        } catch (IllegalAccessException inaccessible) {
            return null;
        }
    }
}
