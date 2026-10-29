package com.aryston.kinetic.client.chunk;

import com.aryston.kinetic.Kinetic;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.Identifier;

public final class CompactTerrainFormat {
    public static final String DEFINE = "KINETIC_COMPACT_VERTICES";
    public static final int VERSION = 1;
    public static final VertexFormat FORMAT = VertexFormat.builder(0)
        .addAttribute("PositionLight", GpuFormat.RGBA16_UINT)
        .addAttribute("Color", GpuFormat.RGBA8_UNORM)
        .addAttribute("PackedUV0", GpuFormat.RG16_UINT)
        .build();

    private static final int VERTEX_BINDING = 0;
    private static final Identifier VERTEX_SHADER = Identifier.fromNamespaceAndPath(Kinetic.MOD_ID, "terrain/compact");
    private static final Set<Identifier> COMPACT_PIPELINES = Set.of(
        Identifier.withDefaultNamespace("pipeline/solid_terrain"),
        Identifier.withDefaultNamespace("pipeline/solid_terrain_multidraw"),
        Identifier.withDefaultNamespace("pipeline/cutout_terrain"),
        Identifier.withDefaultNamespace("pipeline/cutout_terrain_multidraw")
    );

    private CompactTerrainFormat() {
    }

    public static boolean appliesTo(Optional<Identifier> location) {
        return location.isPresent() && COMPACT_PIPELINES.contains(location.get());
    }

    public static RenderPipeline.Builder apply(RenderPipeline.Builder builder) {
        return builder.withVertexBinding(VERTEX_BINDING, FORMAT).withShaderDefine(DEFINE, VERSION).withVertexShader(VERTEX_SHADER);
    }
}
