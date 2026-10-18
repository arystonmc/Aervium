package com.aryston.aervium.core.pregeneration;

public record TerrainProgress(int chunksReady, int chunksRequired, int sectionsReady, int sectionsRequired, int lightUpdates) {
    public static final TerrainProgress NONE = new TerrainProgress(0, 1, 0, 0, 0);

    private static final float CHUNK_SHARE = 0.5F;

    public boolean chunksComplete() {
        return chunksReady >= chunksRequired;
    }

    public boolean complete() {
        return chunksComplete() && sectionsReady >= sectionsRequired && lightUpdates == 0;
    }

    public float fraction() {
        float chunks = share(chunksReady, chunksRequired);
        return chunks * (CHUNK_SHARE + (1.0F - CHUNK_SHARE) * share(sectionsReady, sectionsRequired));
    }

    public boolean advancedFrom(TerrainProgress earlier) {
        return chunksReady > earlier.chunksReady || sectionsReady > earlier.sectionsReady || sectionsRequired != earlier.sectionsRequired
            || lightUpdates < earlier.lightUpdates;
    }

    private static float share(int ready, int required) {
        return required <= 0 ? 1.0F : Math.min(1.0F, ready / (float) required);
    }
}
