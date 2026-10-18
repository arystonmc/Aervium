package com.aryston.aervium.core.pregeneration;

public final class PregenerationHold {
    private static final long NOT_COMPLETE = -1L;

    private final long startedAt;
    private final long limitNanos;
    private final long stallNanos;
    private final long settleNanos;
    private TerrainProgress last = TerrainProgress.NONE;
    private long lastAdvanceAt;
    private long completeSince = NOT_COMPLETE;

    public PregenerationHold(long startedAt, long limitNanos, long stallNanos, long settleNanos) {
        this.startedAt = startedAt;
        this.limitNanos = limitNanos;
        this.stallNanos = stallNanos;
        this.settleNanos = settleNanos;
        this.lastAdvanceAt = startedAt;
    }

    public State update(TerrainProgress progress, long now) {
        if (progress.advancedFrom(last)) {
            lastAdvanceAt = now;
        }
        last = progress;
        if (!progress.complete()) {
            completeSince = NOT_COMPLETE;
        } else if (completeSince == NOT_COMPLETE) {
            completeSince = now;
        }
        if (completeSince != NOT_COMPLETE && now - completeSince >= settleNanos) {
            return State.READY;
        }
        if (now - startedAt >= limitNanos) {
            return State.TIMED_OUT;
        }
        if (now - lastAdvanceAt >= stallNanos) {
            return State.STALLED;
        }
        return State.WAITING;
    }

    public long elapsedNanos(long now) {
        return now - startedAt;
    }

    public enum State {
        WAITING,
        READY,
        TIMED_OUT,
        STALLED
    }
}
