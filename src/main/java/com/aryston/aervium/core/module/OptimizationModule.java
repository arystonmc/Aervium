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
    FACE_CULLING("faceCulling", true),
    POI_SEARCH("poiSearch", false),
    ENTITY_COLLISIONS("entityCollisions", false),
    HOPPER_SEARCH("hopperSearch", false),
    COMPACT_VERTICES("compactVertices", true, false),
    NETWORK_COMPRESSION("networkCompression", false),
    PARALLEL_FEATURES("parallelFeatures", false);

    private final String key;
    private final boolean renderer;
    private final boolean enabledByDefault;

    OptimizationModule(String key, boolean renderer) {
        this(key, renderer, true);
    }

    OptimizationModule(String key, boolean renderer, boolean enabledByDefault) {
        this.key = key;
        this.renderer = renderer;
        this.enabledByDefault = enabledByDefault;
    }

    public String key() {
        return key;
    }

    public boolean renderer() {
        return renderer;
    }

    public boolean enabledByDefault() {
        return enabledByDefault;
    }
}
