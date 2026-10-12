package com.aryston.aervium.core.culling;

public final class CullingStatistics {
    public static final CullingStatistics SHARED = new CullingStatistics();

    private volatile int cameraSections;
    private volatile int cameraHidden;
    private volatile int shadowCasters;
    private volatile int shadowHidden;
    private volatile int entitiesTested;
    private volatile int entitiesHidden;
    private long cameraSectionTotal;
    private long cameraHiddenTotal;
    private int pendingShadowCasters;
    private int pendingShadowHidden;

    public void camera(int sections, int hidden) {
        cameraSections = sections;
        cameraHidden = hidden;
        cameraSectionTotal += sections;
        cameraHiddenTotal += hidden;
    }

    public int takeCameraHiddenPercent() {
        int percent = (int) (cameraSectionTotal == 0 ? 0 : Math.round(100.0 * cameraHiddenTotal / cameraSectionTotal));
        cameraSectionTotal = 0;
        cameraHiddenTotal = 0;
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
