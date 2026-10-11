package com.aryston.aervium.core.telemetry;

import java.util.Locale;

public final class GcPauseClassifier {
    private static final String CYCLE_MARKER = "cycle";

    private GcPauseClassifier() {
    }

    public static boolean isPause(String collectorName, String action) {
        return !mentionsCycle(collectorName) && !mentionsCycle(action);
    }

    private static boolean mentionsCycle(String text) {
        return text != null && text.toLowerCase(Locale.ROOT).contains(CYCLE_MARKER);
    }
}
