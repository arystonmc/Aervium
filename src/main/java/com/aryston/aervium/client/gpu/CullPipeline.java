package com.aryston.aervium.client.gpu;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import com.mojang.renderpearl.backend.vulkan.VulkanDevice;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.shaderc.Shaderc;
import org.lwjgl.vulkan.KHRPushDescriptor;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkComputePipelineCreateInfo;
import org.lwjgl.vulkan.VkDescriptorBufferInfo;
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding;
import org.lwjgl.vulkan.VkDescriptorSetLayoutCreateInfo;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkPipelineLayoutCreateInfo;
import org.lwjgl.vulkan.VkPushConstantRange;
import org.lwjgl.vulkan.VkShaderModuleCreateInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

final class CullPipeline {
    static final int BINDINGS = 6;
    static final int PUSH_CONSTANT_BYTES = 48;
    static final int WORKGROUP_SIZE = 64;
    private static final String SOURCE = "/assets/aervium/shaders/culling/terrain_draws.csh";
    private static final String NAME = "terrain_draws.csh";
    private static final String ENTRY = "main";

    private final long pipelineLayout;
    private final long pipeline;

    private CullPipeline(long pipelineLayout, long pipeline) {
        this.pipelineLayout = pipelineLayout;
        this.pipeline = pipeline;
    }

    static CullPipeline create(VulkanDevice device) throws IOException {
        ByteBuffer spirv = compile(readSource());
        try {
            VkDevice vkDevice = device.vkDevice();
            long setLayout = createSetLayout(vkDevice);
            long pipelineLayout = createPipelineLayout(vkDevice, setLayout);
            long module = createModule(vkDevice, spirv);
            try {
                return new CullPipeline(pipelineLayout, createPipeline(vkDevice, pipelineLayout, module));
            } finally {
                VK10.vkDestroyShaderModule(vkDevice, module, null);
            }
        } finally {
            MemoryUtil.memFree(spirv);
        }
    }

    void record(VkCommandBuffer commandBuffer, long[] buffers, long[] offsets, long[] ranges, ByteBuffer constants, int entries) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VK10.vkCmdBindPipeline(commandBuffer, VK10.VK_PIPELINE_BIND_POINT_COMPUTE, pipeline);
            VkWriteDescriptorSet.Buffer writes = VkWriteDescriptorSet.calloc(BINDINGS, stack);
            for (int binding = 0; binding < BINDINGS; binding++) {
                VkDescriptorBufferInfo.Buffer info = VkDescriptorBufferInfo.calloc(1, stack)
                    .buffer(buffers[binding])
                    .offset(offsets[binding])
                    .range(ranges[binding]);
                writes.get(binding)
                    .sType$Default()
                    .dstBinding(binding)
                    .descriptorCount(1)
                    .descriptorType(VK10.VK_DESCRIPTOR_TYPE_STORAGE_BUFFER)
                    .pBufferInfo(info);
            }
            KHRPushDescriptor.vkCmdPushDescriptorSetKHR(commandBuffer, VK10.VK_PIPELINE_BIND_POINT_COMPUTE, pipelineLayout, 0, writes);
            VK10.vkCmdPushConstants(commandBuffer, pipelineLayout, VK10.VK_SHADER_STAGE_COMPUTE_BIT, 0, constants);
            VK10.vkCmdDispatch(commandBuffer, (entries + WORKGROUP_SIZE - 1) / WORKGROUP_SIZE, 1, 1);
        }
    }

    private static String readSource() throws IOException {
        try (InputStream input = Objects.requireNonNull(CullPipeline.class.getResourceAsStream(SOURCE), SOURCE)) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static ByteBuffer compile(String source) {
        long compiler = Shaderc.shaderc_compiler_initialize();
        long options = Shaderc.shaderc_compile_options_initialize();
        try {
            Shaderc.shaderc_compile_options_set_target_env(options, Shaderc.shaderc_target_env_vulkan, Shaderc.shaderc_env_version_vulkan_1_2);
            Shaderc.shaderc_compile_options_set_optimization_level(options, Shaderc.shaderc_optimization_level_performance);
            long result = Shaderc.shaderc_compile_into_spv(compiler, source, Shaderc.shaderc_glsl_compute_shader, NAME, ENTRY, options);
            try {
                if (Shaderc.shaderc_result_get_compilation_status(result) != Shaderc.shaderc_compilation_status_success) {
                    throw new IllegalStateException("Could not compile " + NAME + ": " + Shaderc.shaderc_result_get_error_message(result));
                }
                ByteBuffer bytes = Objects.requireNonNull(Shaderc.shaderc_result_get_bytes(result), NAME);
                ByteBuffer copy = MemoryUtil.memAlloc(bytes.remaining());
                MemoryUtil.memCopy(bytes, copy);
                return copy;
            } finally {
                Shaderc.shaderc_result_release(result);
            }
        } finally {
            Shaderc.shaderc_compile_options_release(options);
            Shaderc.shaderc_compiler_release(compiler);
        }
    }

    private static long createSetLayout(VkDevice device) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkDescriptorSetLayoutBinding.Buffer bindings = VkDescriptorSetLayoutBinding.calloc(BINDINGS, stack);
            for (int binding = 0; binding < BINDINGS; binding++) {
                bindings.get(binding)
                    .binding(binding)
                    .descriptorType(VK10.VK_DESCRIPTOR_TYPE_STORAGE_BUFFER)
                    .descriptorCount(1)
                    .stageFlags(VK10.VK_SHADER_STAGE_COMPUTE_BIT);
            }
            VkDescriptorSetLayoutCreateInfo info = VkDescriptorSetLayoutCreateInfo.calloc(stack)
                .sType$Default()
                .flags(KHRPushDescriptor.VK_DESCRIPTOR_SET_LAYOUT_CREATE_PUSH_DESCRIPTOR_BIT_KHR)
                .pBindings(bindings);
            LongBuffer handle = stack.mallocLong(1);
            check(VK10.vkCreateDescriptorSetLayout(device, info, null, handle), "descriptor set layout");
            return handle.get(0);
        }
    }

    private static long createPipelineLayout(VkDevice device, long setLayout) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkPushConstantRange.Buffer constants = VkPushConstantRange.calloc(1, stack)
                .stageFlags(VK10.VK_SHADER_STAGE_COMPUTE_BIT)
                .offset(0)
                .size(PUSH_CONSTANT_BYTES);
            VkPipelineLayoutCreateInfo info = VkPipelineLayoutCreateInfo.calloc(stack)
                .sType$Default()
                .pSetLayouts(stack.longs(setLayout))
                .pPushConstantRanges(constants);
            LongBuffer handle = stack.mallocLong(1);
            check(VK10.vkCreatePipelineLayout(device, info, null, handle), "pipeline layout");
            return handle.get(0);
        }
    }

    private static long createModule(VkDevice device, ByteBuffer spirv) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkShaderModuleCreateInfo info = VkShaderModuleCreateInfo.calloc(stack).sType$Default().pCode(spirv);
            LongBuffer handle = stack.mallocLong(1);
            check(VK10.vkCreateShaderModule(device, info, null, handle), "shader module");
            return handle.get(0);
        }
    }

    private static long createPipeline(VkDevice device, long pipelineLayout, long module) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkComputePipelineCreateInfo.Buffer info = VkComputePipelineCreateInfo.calloc(1, stack)
                .sType$Default()
                .layout(pipelineLayout);
            info.stage()
                .sType$Default()
                .stage(VK10.VK_SHADER_STAGE_COMPUTE_BIT)
                .module(module)
                .pName(stack.UTF8(ENTRY));
            LongBuffer handle = stack.mallocLong(1);
            check(VK10.vkCreateComputePipelines(device, VK10.VK_NULL_HANDLE, info, null, handle), "compute pipeline");
            return handle.get(0);
        }
    }

    private static void check(int result, String what) {
        if (result != VK10.VK_SUCCESS) {
            throw new IllegalStateException("Could not create the culling " + what + " (VkResult " + result + ")");
        }
    }
}
