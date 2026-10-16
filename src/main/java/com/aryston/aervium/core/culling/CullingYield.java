package com.aryston.aervium.core.culling;

public final class CullingYield {
    private static final long NOT_PAUSED = Long.MIN_VALUE;

    private final float usefulShare;
    private final int lowResults;
    private final long pauseNanos;
    private int low;
    private long pausedUntil = NOT_PAUSED;

    public CullingYield(float usefulShare, int lowResults, long pauseNanos) {
        this.usefulShare = usefulShare;
        this.lowResults = lowResults;
        this.pauseNanos = pauseNanos;
    }

    public boolean paused(long now) {
        return pausedUntil != NOT_PAUSED && now < pausedUntil;
    }

    public boolean result(int tested, int hidden, long now) {
        if (tested > 0 && hidden >= tested * usefulShare) {
            low = 0;
            return false;
        }
        low++;
        if (low < lowResults) {
            return false;
        }
        pausedUntil = now + pauseNanos;
        low = lowResults - 1;
        return true;
    }

    public void reset() {
        low = 0;
        pausedUntil = NOT_PAUSED;
    }
}
