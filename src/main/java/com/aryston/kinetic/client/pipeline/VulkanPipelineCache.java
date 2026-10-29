package com.aryston.kinetic.client.pipeline;

import com.aryston.kinetic.Kinetic;
import com.aryston.kinetic.core.cache.CacheStatistics;
import com.aryston.kinetic.core.cache.PipelineCacheHeader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkPhysicalDeviceProperties;
import org.lwjgl.vulkan.VkPipelineCacheCreateInfo;

public final class VulkanPipelineCache {
    private static final long NO_CACHE = VK10.VK_NULL_HANDLE;
    private static final byte[] NO_DATA = new byte[0];
    private static final int KILOBYTE = 1024;

    private final Path file;
    private final CacheStatistics statistics = new CacheStatistics();
    private VkDevice device;
    private long handle = NO_CACHE;
    private boolean closed;
    private int loadedBytes;

    VulkanPipelineCache(Path file) {
        this.file = file;
    }

    public synchronized long handle(VkDevice vkDevice) {
        if (closed) {
            return NO_CACHE;
        }
        if (handle == NO_CACHE) {
            device = vkDevice;
            handle = create(vkDevice, compatibleData(vkDevice));
        }
        return vkDevice == device ? handle : NO_CACHE;
    }

    public void recordCreation(long nanos) {
        statistics.miss(nanos);
    }

    CacheStatistics statistics() {
        return statistics;
    }

    int loadedBytes() {
        return loadedBytes;
    }

    synchronized void saveAndClose() {
        if (closed) {
            return;
        }
        closed = true;
        if (handle == NO_CACHE) {
            return;
        }
        try {
            byte[] data = currentData();
            Files.createDirectories(file.getParent());
            Files.write(file, data);
            Kinetic.LOGGER.info("Kinetic saved {} KB of compiled pipelines", data.length / KILOBYTE);
        } catch (IOException failure) {
            Kinetic.LOGGER.warn("Kinetic could not save the pipeline cache", failure);
        } finally {
            VK10.vkDestroyPipelineCache(device, handle, null);
            handle = NO_CACHE;
        }
    }

    private long create(VkDevice vkDevice, byte[] initialData) {
        ByteBuffer data = initialData.length == 0 ? null : MemoryUtil.memAlloc(initialData.length).put(0, initialData);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkPipelineCacheCreateInfo info = VkPipelineCacheCreateInfo.calloc(stack)
                .sType$Default()
                .pInitialData(data);
            LongBuffer pointer = stack.mallocLong(1);
            if (VK10.vkCreatePipelineCache(vkDevice, info, null, pointer) != VK10.VK_SUCCESS) {
                Kinetic.LOGGER.warn("Kinetic could not create a Vulkan pipeline cache; pipelines are created without it");
                return NO_CACHE;
            }
            loadedBytes = initialData.length;
            return pointer.get(0);
        } finally {
            MemoryUtil.memFree(data);
        }
    }

    private byte[] compatibleData(VkDevice vkDevice) {
        byte[] data = readFile();
        if (data.length == 0) {
            return NO_DATA;
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkPhysicalDeviceProperties properties = VkPhysicalDeviceProperties.calloc(stack);
            VK10.vkGetPhysicalDeviceProperties(vkDevice.getPhysicalDevice(), properties);
            byte[] uuid = new byte[PipelineCacheHeader.UUID_BYTES];
            properties.pipelineCacheUUID().get(0, uuid);
            if (PipelineCacheHeader.matches(data, properties.vendorID(), properties.deviceID(), uuid)) {
                return data;
            }
        }
        Kinetic.LOGGER.info("Kinetic ignores the saved pipeline cache because the graphics driver or GPU changed");
        return NO_DATA;
    }

    private byte[] readFile() {
        try {
            return Files.readAllBytes(file);
        } catch (NoSuchFileException missing) {
            return NO_DATA;
        } catch (IOException failure) {
            Kinetic.LOGGER.warn("Kinetic could not read the pipeline cache", failure);
            return NO_DATA;
        }
    }

    private byte[] currentData() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer size = stack.mallocPointer(1);
            VK10.vkGetPipelineCacheData(device, handle, size, null);
            ByteBuffer data = MemoryUtil.memAlloc((int) size.get(0));
            try {
                VK10.vkGetPipelineCacheData(device, handle, size, data);
                byte[] copy = new byte[(int) size.get(0)];
                data.get(0, copy);
                return copy;
            } finally {
                MemoryUtil.memFree(data);
            }
        }
    }
}
