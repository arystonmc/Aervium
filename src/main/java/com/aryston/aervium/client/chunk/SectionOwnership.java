package com.aryston.aervium.client.chunk;

import com.aryston.aervium.client.FrameTrace;
import com.aryston.aervium.core.module.OptimizationModule;
import com.aryston.aervium.integration.MixinModules;
import com.aryston.aervium.mixin.RenderSectionAccessor;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;

public final class SectionOwnership {
    private static final int MAX_SKIPPED_UPLOADS = 4;
    private static final long STAGING_FULL_PARK_NANOS = 200_000L;
    private static final ConcurrentLinkedQueue<Retired> RETIRED = new ConcurrentLinkedQueue<>();

    private static final boolean ACTIVE = MixinModules.activeModules().getOrDefault(OptimizationModule.LOCK_FREE_SECTIONS.key(), false);

    private static int skippedUploads;

    private SectionOwnership() {
    }

    public static boolean active() {
        return ACTIVE;
    }

    public static boolean ownsAllocations() {
        return RenderSystem.isOnRenderThread();
    }

    public static void retire(SectionRenderDispatcher.RenderSection section, SectionMesh mesh) {
        RETIRED.add(new Retired(section, mesh));
    }

    public static void releaseRetired() {
        Retired retired;
        while ((retired = RETIRED.poll()) != null) {
            ((RenderSectionAccessor) retired.section()).aervium$releaseSectionMesh(retired.mesh());
        }
    }

    public static boolean lockForUpload(ReentrantLock lock) {
        if (lock.tryLock() || skippedUploads >= MAX_SKIPPED_UPLOADS) {
            if (!lock.isHeldByCurrentThread()) {
                long start = System.nanoTime();
                lock.lock();
                ChunkUploadTimings.lockWaited(start);
            }
            skippedUploads = 0;
            return true;
        }
        skippedUploads++;
        FrameTrace.uploadSkipped();
        return false;
    }

    public static void waitForStagingSpace() {
        LockSupport.parkNanos(STAGING_FULL_PARK_NANOS);
    }

    private record Retired(SectionRenderDispatcher.RenderSection section, SectionMesh mesh) {
    }
}
