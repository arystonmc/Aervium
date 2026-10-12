package com.aryston.aervium.core.module;

public enum OptimizationModule {
    SHADER_CACHE("shaderCache", true),
    PIPELINE_CACHE("pipelineCache", true),
    PIPELINE_WARMUP("pipelineWarmup", true),
    COMPACT_CHUNK_LOCKS("compactChunkLocks", false),
    SECTION_SLICE_CACHE("sectionSliceCache", true),
    LIGHT_SNAPSHOTS("lightSnapshots", false),
    OCCLUSION_CULLING("occlusionCulling", true),
    MESH_WORKERS("meshWorkers", true),
    POI_SEARCH("poiSearch", false),
    ENTITY_COLLISIONS("entityCollisions", false);

    private final String key;
    private final boolean renderer;

    OptimizationModule(String key, boolean renderer) {
        this.key = key;
        this.renderer = renderer;
    }

    public String key() {
        return key;
    }

    public boolean renderer() {
        return renderer;
    }
}
