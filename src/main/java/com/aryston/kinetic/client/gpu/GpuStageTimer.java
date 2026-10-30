package com.aryston.kinetic.client.gpu;

import com.aryston.kinetic.client.FrameTrace;
import com.aryston.kinetic.core.telemetry.FrameCostRecorder;
import com.aryston.kinetic.core.telemetry.StageTimes;
import com.aryston.kinetic.mixin.VulkanQueryPoolAccessor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.commands.GpuQueryPool;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.backend.vulkan.VulkanDevice;
import java.nio.LongBuffer;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VK12;

public final class GpuStageTimer {
    public static final int NOT_MEASURED = -1;
    public static final String PASS_PREFIX = "render pass: ";
    public static final String WHOLE_FRAME = "whole frame";
    public static final String FRAME_RENDERING = "frame rendering before the swapchain copy";
    private static final int FRAMES_IN_FLIGHT = 4;
    private static final int WHOLE_FRAME_PAIR = 0;
    private static final int FRAME_RENDERING_PAIR = 1;
    private static final int MAX_PAIRS_PER_FRAME = 128;
    private static final int QUERIES_PER_PAIR = 2;
    private static final int QUERIES_PER_FRAME = MAX_PAIRS_PER_FRAME * QUERIES_PER_PAIR;
    private static final int RESULT_LONGS_PER_QUERY = 2;
    private static final long RESULT_STRIDE = (long) RESULT_LONGS_PER_QUERY * Long.BYTES;
    private static final int RESULT_FLAGS = VK10.VK_QUERY_RESULT_64_BIT | VK10.VK_QUERY_RESULT_WITH_AVAILABILITY_BIT;
    private static final StageTimes TIMES = new StageTimes();
    private static final int[][] PAIR_STAGES = new int[FRAMES_IN_FLIGHT][MAX_PAIRS_PER_FRAME];
    private static final int[] PAIRS = new int[FRAMES_IN_FLIGHT];

    private static @Nullable GpuQueryPool pool;
    private static @Nullable LongBuffer results;
    private static boolean enabled;
    private static boolean unavailable;
    private static int frame;
    private static int framePair = NOT_MEASURED;
    private static int renderingPair = NOT_MEASURED;
    private static double nanosPerTick;

    private GpuStageTimer() {
    }

    public static void setEnabled(boolean value) {
        enabled = value;
        if (!value) {
            TIMES.take();
        }
    }

    public static boolean enabled() {
        return enabled;
    }

    public static int stage(String name) {
        return TIMES.stage(name);
    }

    public static void frameStarted() {
        if (unavailable || !open()) {
            return;
        }
        frame = (frame + 1) % FRAMES_IN_FLIGHT;
        collect(frame);
        PAIRS[frame] = 0;
        framePair = beginPair(stage(WHOLE_FRAME), null);
        renderingPair = beginPair(stage(FRAME_RENDERING), null);
    }

    public static int begin(int stage, @Nullable RenderPass pass) {
        return enabled ? beginPair(stage, pass) : NOT_MEASURED;
    }

    private static int beginPair(int stage, @Nullable RenderPass pass) {
        if (pool == null || stage == StageTimes.UNKNOWN || PAIRS[frame] == MAX_PAIRS_PER_FRAME) {
            return NOT_MEASURED;
        }
        int pair = PAIRS[frame]++;
        PAIR_STAGES[frame][pair] = stage;
        write(firstQuery(frame, pair), pass);
        return pair;
    }

    public static void end(int pair, @Nullable RenderPass pass) {
        if (pair == NOT_MEASURED || pool == null) {
            return;
        }
        write(firstQuery(frame, pair) + 1, pass);
    }

    public static void swapchainCopyStarting() {
        end(renderingPair, null);
        renderingPair = NOT_MEASURED;
    }

    public static void frameSubmitting() {
        end(framePair, null);
        framePair = NOT_MEASURED;
    }

    public static List<StageTimes.Stage> take() {
        return TIMES.take();
    }

    private static boolean open() {
        if (pool != null) {
            return true;
        }
        GpuDevice device = RenderSystem.tryGetDevice();
        if (device == null || VulkanAccess.device() == null) {
            return false;
        }
        float period = device.getDeviceInfo().timestampPeriod();
        if (period <= 0.0F) {
            unavailable = true;
            return false;
        }
        nanosPerTick = period;
        pool = device.createTimestampQueryPool(FRAMES_IN_FLIGHT * QUERIES_PER_FRAME);
        results = MemoryUtil.memAllocLong(QUERIES_PER_FRAME * RESULT_LONGS_PER_QUERY);
        return true;
    }

    private static void write(int query, @Nullable RenderPass pass) {
        GpuQueryPool queries = pool;
        if (queries == null) {
            return;
        }
        if (pass != null) {
            pass.writeTimestamp(queries, query);
        } else {
            RenderSystem.getDevice().createCommandEncoder().writeTimestamp(queries, query);
        }
    }

    private static void collect(int slot) {
        int pairs = PAIRS[slot];
        GpuQueryPool queries = pool;
        LongBuffer buffer = results;
        VulkanDevice device = VulkanAccess.device();
        if (pairs == 0 || queries == null || buffer == null || device == null) {
            return;
        }
        int count = pairs * QUERIES_PER_PAIR;
        buffer.clear();
        buffer.limit(count * RESULT_LONGS_PER_QUERY);
        int status = VK12.vkGetQueryPoolResults(
            device.vkDevice(), ((VulkanQueryPoolAccessor) queries).kinetic$vkQueryPool(), firstQuery(slot, 0), count, buffer, RESULT_STRIDE, RESULT_FLAGS
        );
        if (status != VK10.VK_SUCCESS && status != VK10.VK_NOT_READY) {
            return;
        }
        for (int pair = 0; pair < pairs; pair++) {
            int begin = pair * QUERIES_PER_PAIR * RESULT_LONGS_PER_QUERY;
            int end = begin + RESULT_LONGS_PER_QUERY;
            if (buffer.get(begin + 1) != 0L && buffer.get(end + 1) != 0L) {
                long nanos = Math.round((buffer.get(end) - buffer.get(begin)) * nanosPerTick);
                if (enabled) {
                    TIMES.add(PAIR_STAGES[slot][pair], nanos);
                }
                if (pair == WHOLE_FRAME_PAIR) {
                    FrameTrace.earlierGpuFrameMeasured(nanos);
                    FrameCostRecorder.SHARED.gpuFrameMeasured(nanos);
                } else if (pair == FRAME_RENDERING_PAIR) {
                    FrameTrace.earlierGpuRenderingMeasured(nanos);
                }
            }
        }
        if (enabled) {
            TIMES.frameMeasured();
        }
    }

    private static int firstQuery(int slot, int pair) {
        return slot * QUERIES_PER_FRAME + pair * QUERIES_PER_PAIR;
    }
}
