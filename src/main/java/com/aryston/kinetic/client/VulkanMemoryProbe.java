package com.aryston.kinetic.client;

import com.aryston.kinetic.core.telemetry.VramUsage;
import com.aryston.kinetic.mixin.FrontendGpuDeviceAccessor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.backend.vulkan.VulkanDevice;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.vma.Vma;
import org.lwjgl.util.vma.VmaBudget;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties;

final class VulkanMemoryProbe {
    private static final long NO_ALLOCATOR = 0L;

    private VulkanMemoryProbe() {
    }

    static VramUsage sample() {
        long allocator = allocator();
        if (allocator == NO_ALLOCATOR) {
            return VramUsage.UNAVAILABLE;
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer propertiesPointer = stack.mallocPointer(1);
            Vma.vmaGetMemoryProperties(allocator, propertiesPointer);
            VkPhysicalDeviceMemoryProperties properties = VkPhysicalDeviceMemoryProperties.create(propertiesPointer.get(0));
            VmaBudget.Buffer budgets = VmaBudget.calloc(VK10.VK_MAX_MEMORY_HEAPS, stack);
            Vma.vmaGetHeapBudgets(allocator, budgets);
            long used = 0;
            long budget = 0;
            for (int heap = 0; heap < properties.memoryHeapCount(); heap++) {
                if ((properties.memoryHeaps(heap).flags() & VK10.VK_MEMORY_HEAP_DEVICE_LOCAL_BIT) != 0) {
                    used += budgets.get(heap).usage();
                    budget += budgets.get(heap).budget();
                }
            }
            return new VramUsage(used, budget);
        }
    }

    static long deviceLocalBytes() {
        long allocator = allocator();
        if (allocator == NO_ALLOCATOR) {
            return 0L;
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer propertiesPointer = stack.mallocPointer(1);
            Vma.vmaGetMemoryProperties(allocator, propertiesPointer);
            VkPhysicalDeviceMemoryProperties properties = VkPhysicalDeviceMemoryProperties.create(propertiesPointer.get(0));
            long size = 0;
            for (int heap = 0; heap < properties.memoryHeapCount(); heap++) {
                if ((properties.memoryHeaps(heap).flags() & VK10.VK_MEMORY_HEAP_DEVICE_LOCAL_BIT) != 0) {
                    size += properties.memoryHeaps(heap).size();
                }
            }
            return size;
        }
    }

    private static long allocator() {
        GpuDevice device = RenderSystem.tryGetDevice();
        if (device instanceof FrontendGpuDeviceAccessor accessor && accessor.kinetic$backend() instanceof VulkanDevice vulkan) {
            return vulkan.vma();
        }
        return NO_ALLOCATOR;
    }
}
