package com.aryston.kinetic.client.lod;

import com.aryston.kinetic.Kinetic;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.UniformType;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import java.util.Optional;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.resources.Identifier;

final class DistantTerrainPipeline {
    private static final Identifier SHADER = Identifier.fromNamespaceAndPath(Kinetic.MOD_ID, "lod/distant_terrain");
    private static final Identifier GEOMETRY_SHADER = Identifier.fromNamespaceAndPath(Kinetic.MOD_ID, "lod/distant_terrain_geometry");
    private static final Identifier WATER_SURFACE_SHADER = Identifier.fromNamespaceAndPath(Kinetic.MOD_ID, "lod/distant_water_surface");
    private static final String SCENE_DEPTH_SAMPLER = "SceneDepthSampler";
    private static final int SURFACE_TARGET = 0;
    private static final int TINT_TARGET = 1;
    private static final int NORMAL_TARGET = 1;
    private static final int LIGHT_TARGET = 2;
    private static final int ALBEDO_TARGET = 3;
    private static final VertexFormat INSTANCES = VertexFormat.builder(1)
        .addAttribute("TileOffset", GpuFormat.RGB32_FLOAT)
        .addAttribute("FadeRange", GpuFormat.RG32_FLOAT)
        .addAttribute("TransitionBounds", GpuFormat.RGBA32_FLOAT)
        .build();

    static final RenderPipeline PIPELINE = base("pipeline/distant_terrain").build();
    static final RenderPipeline WATER_PIPELINE = base("pipeline/distant_terrain_water").build();
    static final RenderPipeline GEOMETRY_PIPELINE = base("pipeline/distant_terrain_geometry")
        .withFragmentShader(GEOMETRY_SHADER)
        .withColorTargetState(NORMAL_TARGET, target(GpuFormat.RGB10A2_UNORM))
        .withColorTargetState(LIGHT_TARGET, target(GpuFormat.RGBA8_UNORM))
        .withColorTargetState(ALBEDO_TARGET, target(GpuFormat.RGBA8_UNORM))
        .build();

    static final RenderPipeline WATER_SURFACE_PIPELINE = base("pipeline/distant_terrain_water_surface")
        .withFragmentShader(WATER_SURFACE_SHADER)
        .withBindGroupLayout(BindGroupLayout.builder().withUniform(SCENE_DEPTH_SAMPLER, UniformType.COMBINED_IMAGE_SAMPLER).build())
        .withColorTargetState(SURFACE_TARGET, target(GpuFormat.RGBA16_FLOAT))
        .withColorTargetState(TINT_TARGET, target(GpuFormat.RGBA8_UNORM))
        .build();

    private DistantTerrainPipeline() {
    }

    private static RenderPipeline.Builder base(String location) {
        return RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
            .withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER2)
            .withLocation(Identifier.fromNamespaceAndPath(Kinetic.MOD_ID, location))
            .withVertexShader(SHADER)
            .withFragmentShader(SHADER)
            .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
            .withVertexBinding(1, INSTANCES)
            .withColorTargetState(ColorTargetState.DEFAULT)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, true))
            .withCull(false);
    }

    private static ColorTargetState target(GpuFormat format) {
        return new ColorTargetState(Optional.empty(), format, ColorTargetState.WRITE_ALL);
    }
}
