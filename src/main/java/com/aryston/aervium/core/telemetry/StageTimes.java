package com.aryston.aervium.core.telemetry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class StageTimes {
    public static final int MAX_STAGES = 128;
    public static final int UNKNOWN = -1;
    private static final double NANOS_PER_MILLI = 1.0E6;

    private final String[] names = new String[MAX_STAGES];
    private final long[] totalNanos = new long[MAX_STAGES];
    private final long[] maxNanos = new long[MAX_STAGES];
    private final int[] samples = new int[MAX_STAGES];
    private int stages;
    private long frames;

    public int stage(String name) {
        for (int index = 0; index < stages; index++) {
            if (names[index].equals(name)) {
                return index;
            }
        }
        if (stages == MAX_STAGES) {
            return UNKNOWN;
        }
        names[stages] = name;
        return stages++;
    }

    public void add(int stage, long nanos) {
        if (stage < 0 || stage >= stages || nanos < 0L) {
            return;
        }
        totalNanos[stage] += nanos;
        maxNanos[stage] = Math.max(maxNanos[stage], nanos);
        samples[stage]++;
    }

    public void frameMeasured() {
        frames++;
    }

    public List<Stage> take() {
        List<Stage> result = new ArrayList<>();
        for (int index = 0; index < stages; index++) {
            if (samples[index] > 0 && frames > 0L) {
                result.add(new Stage(names[index], totalNanos[index] / NANOS_PER_MILLI / frames, maxNanos[index] / NANOS_PER_MILLI));
            }
            totalNanos[index] = 0L;
            maxNanos[index] = 0L;
            samples[index] = 0;
        }
        frames = 0L;
        return Collections.unmodifiableList(result);
    }

    public static String describe(List<Stage> stages) {
        StringBuilder text = new StringBuilder();
        for (Stage stage : stages) {
            if (!text.isEmpty()) {
                text.append(", ");
            }
            text.append(String.format(Locale.ROOT, "%s %.3f (max %.2f)", stage.name(), stage.averageMs(), stage.maxMs()));
        }
        return text.toString();
    }

    public record Stage(String name, double averageMs, double maxMs) {
    }
}
