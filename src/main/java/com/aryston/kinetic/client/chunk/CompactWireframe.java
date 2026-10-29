package com.aryston.kinetic.client.chunk;

import com.aryston.kinetic.Kinetic;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

public final class CompactWireframe {
    private static final RenderPipeline DIRECT = compact(RenderPipelines.WIREFRAME, "pipeline/compact_wireframe");
    private static final RenderPipeline MULTIDRAW = compact(RenderPipelines.WIREFRAME_MULTIDRAW, "pipeline/compact_wireframe_multidraw");

    private CompactWireframe() {
    }

    public static @Nullable RenderPipeline direct(@Nullable RenderPipeline override, ChunkSectionLayerGroup group) {
        return override == RenderPipelines.WIREFRAME && group == ChunkSectionLayerGroup.OPAQUE ? DIRECT : override;
    }

    public static @Nullable RenderPipeline multiDraw(@Nullable RenderPipeline override, ChunkSectionLayerGroup group) {
        return override == RenderPipelines.WIREFRAME_MULTIDRAW && group == ChunkSectionLayerGroup.OPAQUE ? MULTIDRAW : override;
    }

    private static RenderPipeline compact(RenderPipeline wireframe, String path) {
        return CompactTerrainFormat.apply(wireframe.toBuilder().withLocation(Identifier.fromNamespaceAndPath(Kinetic.MOD_ID, path))).build();
    }
}
