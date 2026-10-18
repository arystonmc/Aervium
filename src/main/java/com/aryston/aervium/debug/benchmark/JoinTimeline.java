package com.aryston.aervium.debug.benchmark;

import com.aryston.aervium.core.telemetry.Durations;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class JoinTimeline {
    public static final long GAP_NANOS = 50_000_000L;

    private static final long NO_FRAME = -1L;
    private static final int REPORTED_GAPS = 8;

    private final List<Phase> phases = new ArrayList<>();
    private final List<Gap> gaps = new ArrayList<>();
    private long startedAt = NO_FRAME;
    private long lastFrame = NO_FRAME;

    public void frame(long now, String phase) {
        if (startedAt == NO_FRAME) {
            startedAt = now;
            lastFrame = now;
            phases.add(new Phase(phase, 0L));
            return;
        }
        long interval = now - lastFrame;
        Phase current = phases.getLast();
        current.add(interval);
        if (interval > GAP_NANOS) {
            gaps.add(new Gap(lastFrame - startedAt, interval, current.name, phase));
        }
        if (!current.name.equals(phase)) {
            phases.add(new Phase(phase, now - startedAt));
        }
        lastFrame = now;
    }

    public boolean started() {
        return startedAt != NO_FRAME;
    }

    public long elapsedNanos(long now) {
        return startedAt == NO_FRAME ? 0L : now - startedAt;
    }

    public List<String> lines() {
        List<String> lines = new ArrayList<>();
        for (Phase phase : phases) {
            lines.add(String.format(Locale.ROOT, "phase %s: starts %d ms, lasts %d ms, %d frames, longest %.1f ms, %d over 50 ms",
                phase.name, Math.round(Durations.millis(phase.startedAt)), Math.round(Durations.millis(phase.total)), phase.frames,
                Durations.millis(phase.longest), phase.over50));
        }
        List<Gap> longest = new ArrayList<>(gaps);
        longest.sort(Comparator.comparingLong(Gap::duration).reversed());
        StringBuilder gapLine = new StringBuilder(String.format(Locale.ROOT, "%d frames over 50 ms", gaps.size()));
        for (Gap gap : longest.subList(0, Math.min(REPORTED_GAPS, longest.size()))) {
            gapLine.append(String.format(Locale.ROOT, "; %.0f ms at %d ms (%s", Durations.millis(gap.duration()),
                Math.round(Durations.millis(gap.startedAt())), gap.before()));
            gapLine.append(gap.before().equals(gap.after()) ? ")" : " to " + gap.after() + ")");
        }
        lines.add(gapLine.toString());
        return lines;
    }

    public int gapCount() {
        return gaps.size();
    }

    public long longestNanos() {
        long longest = 0L;
        for (Phase phase : phases) {
            longest = Math.max(longest, phase.longest);
        }
        return longest;
    }

    private static final class Phase {
        private final String name;
        private final long startedAt;
        private long total;
        private long longest;
        private int frames;
        private int over50;

        private Phase(String name, long startedAt) {
            this.name = name;
            this.startedAt = startedAt;
        }

        private void add(long interval) {
            total += interval;
            longest = Math.max(longest, interval);
            frames++;
            if (interval > GAP_NANOS) {
                over50++;
            }
        }
    }

    private record Gap(long startedAt, long duration, String before, String after) {
    }
}
