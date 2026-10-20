package com.aryston.aervium.core.lod;

public final class DistantTerrainLimits {
    public static final int MIN_CHUNKS = 64;
    public static final int DEFAULT_CHUNKS = 256;
    public static final int MAX_CHUNKS = 512;
    public static final int MAX_RESIDENT_TILES = 4096;
    public static final long MAX_RESIDENT_BYTES = 256L * 1024L * 1024L;

    private DistantTerrainLimits() {
    }
}
