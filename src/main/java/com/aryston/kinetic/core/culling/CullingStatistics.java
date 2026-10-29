package com.aryston.kinetic.core.culling;

import java.util.Arrays;

public final class CullingStatistics {
    public static final CullingStatistics SHARED = new CullingStatistics();

    private volatile int cameraSections;
    private volatile int cameraHidden;
    private volatile int shadowCasters;
    private volatile int shadowHidden;
    private volatile int entitiesTested;
    private volatile int entitiesHidden;
    private volatile int particlesTested;
    private volatile int particlesHidden;
    private volatile long cameraJobNanos;
    private long cameraSectionTotal;
    private long cameraHiddenTotal;
    private long faceQuadTotal;
    private long faceSkippedTotal;
    private long shadowCasterTotal;
    private long shadowHiddenTotal;
    private int pendingShadowCasters;
    private int pendingShadowHidden;
    private final long[] cameraResults = new long[CameraResult.values().length];

    public void cameraResult(CameraResult result) {
        cameraResults[result.ordinal()]++;
    }

    public CameraResultCounts takeCameraResults() {
        long frames = 0L;
        for (long count : cameraResults) {
            frames += count;
        }
        CameraResultCounts counts = new CameraResultCounts(
            frames,
            cameraResults[CameraResult.WIDENED.ordinal()],
            cameraResults[CameraResult.NONE.ordinal()],
            cameraResults[CameraResult.TOO_OLD.ordinal()],
            cameraResults[CameraResult.OUT_OF_REACH.ordinal()]
        );
        Arrays.fill(cameraResults, 0L);
        return counts;
    }

    public void camera(int sections, int hidden) {
        cameraSections = sections;
        cameraHidden = hidden;
        cameraSectionTotal += sections;
        cameraHiddenTotal += hidden;
    }

    public void cameraJob(long nanos) {
        cameraJobNanos = nanos;
    }

    public long cameraJobNanos() {
        return cameraJobNanos;
    }

    public int takeCameraHiddenPercent() {
        int percent = (int) (cameraSectionTotal == 0 ? 0 : Math.round(100.0 * cameraHiddenTotal / cameraSectionTotal));
        cameraSectionTotal = 0;
        cameraHiddenTotal = 0;
        return percent;
    }

    public int takeShadowHiddenPercent() {
        int percent = (int) (shadowCasterTotal == 0 ? 0 : Math.round(100.0 * shadowHiddenTotal / shadowCasterTotal));
        shadowCasterTotal = 0;
        shadowHiddenTotal = 0;
        return percent;
    }

    public void faces(long quads, long skipped) {
        faceQuadTotal += quads;
        faceSkippedTotal += skipped;
    }

    public int takeFaceSkippedPercent() {
        int percent = (int) (faceQuadTotal == 0 ? 0 : Math.round(100.0 * faceSkippedTotal / faceQuadTotal));
        faceQuadTotal = 0;
        faceSkippedTotal = 0;
        return percent;
    }

    public void entities(int tested, int hidden) {
        entitiesTested = tested;
        entitiesHidden = hidden;
    }

    public int entitiesTested() {
        return entitiesTested;
    }

    public int entitiesHidden() {
        return entitiesHidden;
    }

    public void particles(int tested, int hidden) {
        particlesTested = tested;
        particlesHidden = hidden;
    }

    public int particlesTested() {
        return particlesTested;
    }

    public int particlesHidden() {
        return particlesHidden;
    }

    public void beginShadows() {
        pendingShadowCasters = 0;
        pendingShadowHidden = 0;
    }

    public void shadowCascade(int casters, int hidden) {
        pendingShadowCasters += casters;
        pendingShadowHidden += hidden;
    }

    public void endShadows() {
        shadowCasters = pendingShadowCasters;
        shadowHidden = pendingShadowHidden;
        shadowCasterTotal += pendingShadowCasters;
        shadowHiddenTotal += pendingShadowHidden;
    }

    public int cameraSections() {
        return cameraSections;
    }

    public int cameraHidden() {
        return cameraHidden;
    }

    public int shadowCasters() {
        return shadowCasters;
    }

    public int shadowHidden() {
        return shadowHidden;
    }

    public static int percent(int part, int whole) {
        return whole == 0 ? 0 : Math.round(100.0F * part / whole);
    }
}
