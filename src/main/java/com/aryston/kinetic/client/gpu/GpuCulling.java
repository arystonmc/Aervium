package com.aryston.kinetic.client.gpu;

import com.aryston.kinetic.Kinetic;
import com.aryston.kinetic.client.FrameTrace;
import com.aryston.kinetic.core.module.OptimizationModule;
import com.aryston.kinetic.integration.MixinModules;
import com.aryston.kinetic.mixin.FrontendRenderPassAccessor;
import com.aryston.kinetic.mixin.VulkanRenderPassAccessor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.IndexType;
import com.mojang.renderpearl.backend.vulkan.VulkanCommandEncoder;
import com.mojang.renderpearl.backend.vulkan.VulkanDevice;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkCommandBuffer;

public final class GpuCulling {
    public static final int MAX_QUAD_OFFSET = 0xFFFF;
    private static final int MAX_DRAWS_PER_SLOT = 4;
    private static final int INTS_PER_COMMAND = 5;
    private static final int COMMAND_BYTES = INTS_PER_COMMAND * Integer.BYTES;
    private static final int VISIBLE_BYTES = 2 * Integer.BYTES;
    private static final int INSTANCE_BYTES = 4 * Integer.BYTES;
    private static final int RANK_BYTES = TerrainGroups.MAX_GROUPS * Integer.BYTES;
    private static final int INITIAL_VISIBLE = 16384;
    private static final int VISIBLE_USAGE = GpuBuffer.USAGE_MAP_WRITE | GpuBufferUsage.STORAGE;
    private static final int COMMAND_USAGE = GpuBuffer.USAGE_INDIRECT_PARAMETERS | GpuBufferUsage.STORAGE;
    private static final int COUNT_USAGE = GpuBuffer.USAGE_INDIRECT_PARAMETERS | GpuBuffer.USAGE_COPY_DST | GpuBufferUsage.STORAGE;
    private static final int TABLE_BINDING = 0;
    private static final int VISIBLE_BINDING = 1;
    private static final int COMMAND_BINDING = 2;
    private static final int COUNT_BINDING = 3;
    private static final int INSTANCE_BINDING = 4;
    private static final int RANK_BINDING = 5;
    private static final int FACE_CULLING_ON = 1;
    private static final int FACE_CULLING_OFF = 0;

    private static final GpuSectionTable TABLE = new GpuSectionTable();
    private static final TerrainGroups GROUPS = new TerrainGroups();
    private static final List<GpuBuffer> RETIRED = new ArrayList<>();
    private static final List<MappableRingBuffer> RETIRED_RINGS = new ArrayList<>();
    private static final long[] BUFFERS = new long[CullPipeline.BINDINGS];
    private static final long[] OFFSETS = new long[CullPipeline.BINDINGS];
    private static final long[] RANGES = new long[CullPipeline.BINDINGS];

    private static State state = State.UNCHECKED;
    private static @Nullable CompletableFuture<CullPipeline> pendingPipeline;
    private static @Nullable VulkanDevice device;
    private static @Nullable CullPipeline pipeline;
    private static @Nullable MappableRingBuffer visibleRing;
    private static int visibleCapacity;
    private static GpuBufferSlice.@Nullable MappedView visibleView;
    private static long visibleAddress;
    private static @Nullable GpuBuffer commands;
    private static long commandBytes;
    private static @Nullable GpuBuffer counts;
    private static boolean passOpen;
    private static boolean usedThisFrame;
    private static boolean dispatchPending;
    private static int visibleCount;
    private static int drawCapacity;
    private static int largestIndexCount;
    private static int maxDrawCount;
    private static boolean faceCulling;
    private static double cameraX;
    private static double cameraY;
    private static double cameraZ;

    private GpuCulling() {
    }

    public static boolean beginPass(int maxVisible, Vec3 camera, boolean cullFaces) {
        if (usedThisFrame || !ready() || (long) maxVisible * MAX_DRAWS_PER_SLOT > Integer.toUnsignedLong(maxDrawCount)) {
            return false;
        }
        usedThisFrame = true;
        passOpen = true;
        dispatchPending = false;
        visibleCount = 0;
        faceCulling = cullFaces;
        cameraX = camera.x;
        cameraY = camera.y;
        cameraZ = camera.z;
        GROUPS.dropClosed();
        TABLE.begin(maxVisible);
        ensureVisible(maxVisible);
        visibleView = visibleRing.currentBuffer().slice(0, RANK_BYTES + (long) visibleCapacity * VISIBLE_BYTES).map(false, true);
        visibleAddress = MemoryUtil.memAddress(visibleView.data());
        return true;
    }

    public static int epoch() {
        return TABLE.epoch();
    }

    public static boolean fits(int section) {
        return TABLE.fits(section);
    }

    public static int groupId(ChunkSectionLayer layer, GpuBuffer vertexBuffer, @Nullable GpuBuffer indexBuffer, @Nullable IndexType indexType) {
        return GROUPS.idFor(layer, vertexBuffer, indexBuffer, indexType);
    }

    public static void upload(int section, GpuSectionEntry entry, int sharedIndexCount) {
        TABLE.put(section, entry.ints());
        FrameTrace.gpuSectionWritten();
        largestIndexCount = Math.max(largestIndexCount, sharedIndexCount);
    }

    public static void addVisible(int section, float visibility) {
        long entry = visibleAddress + RANK_BYTES + (long) visibleCount * VISIBLE_BYTES;
        MemoryUtil.memPutInt(entry, section);
        MemoryUtil.memPutInt(entry + Integer.BYTES, Float.floatToRawIntBits(visibility));
        visibleCount++;
    }

    public static int finishPass(Map<ChunkSectionLayer, List<LevelRenderer.ChunkDrawGroup>> drawGroups) {
        if (!passOpen) {
            return 0;
        }
        passOpen = false;
        int live = GROUPS.rankLive();
        for (int id = 0; id < TerrainGroups.MAX_GROUPS; id++) {
            MemoryUtil.memPutInt(visibleAddress + (long) id * Integer.BYTES, GROUPS.rank(id));
        }
        if (visibleCount == 0 || live == 0) {
            closeVisibleView();
            return 0;
        }
        drawCapacity = visibleCount * MAX_DRAWS_PER_SLOT;
        ensureCommands((long) live * drawCapacity * COMMAND_BYTES);
        for (int id = 0; id < TerrainGroups.MAX_GROUPS; id++) {
            TerrainGroups.Group group = GROUPS.group(id);
            if (group == null) {
                continue;
            }
            drawGroups.get(group.layer()).add(new LevelRenderer.ChunkDrawGroup(
                group.vertexBuffer().slice(),
                group.indexBuffer() == null ? null : group.indexBuffer().slice(),
                group.indexType(),
                new GpuDrawList(GROUPS.rank(id), drawCapacity)
            ));
        }
        dispatchPending = true;
        return largestIndexCount;
    }

    public static int instanceSlots() {
        return dispatchPending ? visibleCount : 0;
    }

    public static GpuBufferSlice commands(GpuDrawList draws) {
        return commands.slice((long) draws.rank() * drawCapacity * COMMAND_BYTES, (long) drawCapacity * COMMAND_BYTES);
    }

    public static void dispatch(GpuBuffer instances, long instanceOffset, int instanceBase) {
        if (!dispatchPending) {
            return;
        }
        dispatchPending = false;
        closeVisibleView();
        VkCommandBuffer commandBuffer = VulkanAccess.commandBuffer(device);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VulkanCommandEncoder.memoryBarrier(commandBuffer, stack);
            TABLE.recordUploads(commandBuffer);
            VK10.vkCmdFillBuffer(commandBuffer, VulkanAccess.handle(counts), 0L, RANK_BYTES, 0);
            VulkanCommandEncoder.memoryBarrier(commandBuffer, stack);
            bind(TABLE_BINDING, TABLE.buffer(), 0L, TABLE.size());
            bind(VISIBLE_BINDING, visibleRing.currentBuffer(), RANK_BYTES, (long) visibleCount * VISIBLE_BYTES);
            bind(COMMAND_BINDING, commands, 0L, commandBytes);
            bind(COUNT_BINDING, counts, 0L, RANK_BYTES);
            bind(INSTANCE_BINDING, instances, instanceOffset, (long) visibleCount * INSTANCE_BYTES);
            bind(RANK_BINDING, visibleRing.currentBuffer(), 0L, RANK_BYTES);
            pipeline.record(commandBuffer, BUFFERS, OFFSETS, RANGES, constants(stack, instanceBase), visibleCount);
            VulkanCommandEncoder.memoryBarrier(commandBuffer, stack);
        }
    }

    public static void drawIndexedIndirect(RenderPass pass, GpuBufferSlice slice, int drawCount) {
        if (commands == null || slice.buffer() != commands) {
            pass.drawIndexedIndirect(slice, drawCount);
            return;
        }
        if (!(pass instanceof FrontendRenderPassAccessor frontend) || !(frontend.kinetic$backend() instanceof VulkanRenderPassAccessor vulkan)) {
            fail(new IllegalStateException("The terrain render pass is not a Vulkan render pass"));
            return;
        }
        long rank = slice.offset() / ((long) drawCapacity * COMMAND_BYTES);
        vulkan.kinetic$pushDescriptors();
        VK12.vkCmdDrawIndexedIndirectCount(
            vulkan.kinetic$commandBuffer(),
            VulkanAccess.handle(commands),
            slice.offset(),
            VulkanAccess.handle(counts),
            rank * Integer.BYTES,
            drawCount,
            COMMAND_BYTES
        );
    }

    public static void endFrame() {
        passOpen = false;
        dispatchPending = false;
        usedThisFrame = false;
        closeVisibleView();
        if (visibleRing != null) {
            visibleRing.rotate();
        }
        TABLE.endFrame();
        RETIRED.forEach(GpuBuffer::close);
        RETIRED.clear();
        RETIRED_RINGS.forEach(MappableRingBuffer::close);
        RETIRED_RINGS.clear();
    }

    private static ByteBuffer constants(MemoryStack stack, int instanceBase) {
        int blockX = Mth.floor(cameraX);
        int blockY = Mth.floor(cameraY);
        int blockZ = Mth.floor(cameraZ);
        ByteBuffer constants = stack.malloc(CullPipeline.PUSH_CONSTANT_BYTES);
        constants.putInt(blockX).putInt(blockY).putInt(blockZ).putInt(visibleCount);
        constants.putFloat((float) (cameraX - blockX)).putFloat((float) (cameraY - blockY)).putFloat((float) (cameraZ - blockZ)).putFloat(0.0F);
        constants.putInt(drawCapacity).putInt(instanceBase).putInt(faceCulling ? FACE_CULLING_ON : FACE_CULLING_OFF).putInt(0);
        return constants.flip();
    }

    private static void bind(int binding, GpuBuffer buffer, long offset, long range) {
        BUFFERS[binding] = VulkanAccess.handle(buffer);
        OFFSETS[binding] = offset;
        RANGES[binding] = range;
    }

    public static void warmUp(Executor worker) {
        if (state != State.UNCHECKED) {
            return;
        }
        if (!MixinModules.activeModules().getOrDefault(OptimizationModule.GPU_CULLING.key(), false)) {
            state = State.UNAVAILABLE;
            return;
        }
        VulkanDevice vulkan = VulkanAccess.device();
        if (vulkan == null || !VulkanAccess.supportsIndirectCount(vulkan)) {
            Kinetic.LOGGER.info("Kinetic GPU culling is not available on this device; terrain draw lists stay on the CPU");
            state = State.UNAVAILABLE;
            return;
        }
        device = vulkan;
        pendingPipeline = CompletableFuture.supplyAsync(() -> {
            try {
                return CullPipeline.create(vulkan);
            } catch (IOException failure) {
                throw new UncheckedIOException(failure);
            }
        }, worker);
        state = State.COMPILING;
    }

    private static boolean ready() {
        if (state == State.UNCHECKED) {
            warmUp(ForkJoinPool.commonPool());
        }
        if (state == State.COMPILING && pendingPipeline != null && pendingPipeline.isDone()) {
            state = initialize(pendingPipeline);
            pendingPipeline = null;
        }
        return state == State.READY;
    }

    private static State initialize(CompletableFuture<CullPipeline> compiled) {
        try {
            pipeline = compiled.join();
        } catch (CompletionException failure) {
            Kinetic.LOGGER.warn("Kinetic GPU culling could not be set up; terrain draw lists stay on the CPU", failure.getCause());
            return State.UNAVAILABLE;
        }
        maxDrawCount = RenderSystem.getDevice().getDeviceInfo().limits().maxDrawIndirectDrawCount();
        counts = RenderSystem.getDevice().createBuffer(() -> "Kinetic GPU draw counts", COUNT_USAGE, RANK_BYTES);
        Kinetic.LOGGER.info("Kinetic GPU culling is on: the GPU builds the solid and cutout terrain draw lists");
        return State.READY;
    }

    private static void ensureVisible(int maxVisible) {
        if (visibleRing != null && maxVisible <= visibleCapacity) {
            return;
        }
        if (visibleRing != null) {
            RETIRED_RINGS.add(visibleRing);
        }
        visibleCapacity = Mth.smallestEncompassingPowerOfTwo(Math.max(maxVisible, INITIAL_VISIBLE));
        int capacity = visibleCapacity;
        visibleRing = new MappableRingBuffer(() -> "Kinetic visible sections " + capacity, VISIBLE_USAGE, RANK_BYTES + visibleCapacity * VISIBLE_BYTES);
    }

    private static void ensureCommands(long bytes) {
        if (commands != null && bytes <= commandBytes) {
            return;
        }
        if (commands != null) {
            RETIRED.add(commands);
        }
        commandBytes = Mth.smallestEncompassingPowerOfTwo((int) Math.min(Integer.MAX_VALUE / 2, bytes));
        long size = commandBytes;
        commands = RenderSystem.getDevice().createBuffer(() -> "Kinetic GPU draw commands " + size, COMMAND_USAGE, commandBytes);
    }

    private static void closeVisibleView() {
        if (visibleView != null) {
            visibleView.close();
            visibleView = null;
        }
    }

    private static void fail(RuntimeException failure) {
        if (state == State.READY) {
            Kinetic.LOGGER.warn("Kinetic GPU culling stops; terrain draw lists go back to the CPU", failure);
        }
        state = State.UNAVAILABLE;
    }

    private enum State {
        UNCHECKED,
        COMPILING,
        READY,
        UNAVAILABLE
    }
}
