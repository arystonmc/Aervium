package com.aryston.aervium.core.telemetry;

import com.sun.management.GarbageCollectionNotificationInfo;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;

public final class GcPauseRecorder implements NotificationListener {
    private static final int RECENT_PAUSES = 4096;

    private final SampleRing pauses = new SampleRing(RECENT_PAUSES);
    private long totalPauseNanos;
    private long longestPauseNanos;

    GcPauseRecorder() {
    }

    public static GcPauseRecorder install() {
        GcPauseRecorder recorder = new GcPauseRecorder();
        for (GarbageCollectorMXBean collector : ManagementFactory.getGarbageCollectorMXBeans()) {
            if (collector instanceof NotificationEmitter emitter) {
                emitter.addNotificationListener(recorder, null, null);
            }
        }
        return recorder;
    }

    @Override
    public void handleNotification(Notification notification, Object handback) {
        if (!GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION.equals(notification.getType())) {
            return;
        }
        GarbageCollectionNotificationInfo info = GarbageCollectionNotificationInfo.from((CompositeData) notification.getUserData());
        record(info.getGcName(), info.getGcAction(), info.getGcInfo().getDuration());
    }

    public synchronized void record(String collectorName, String action, long durationMs) {
        if (!GcPauseClassifier.isPause(collectorName, action)) {
            return;
        }
        long nanos = (long) (durationMs * Durations.NANOS_PER_MILLISECOND);
        pauses.record(nanos);
        totalPauseNanos += nanos;
        longestPauseNanos = Math.max(longestPauseNanos, nanos);
    }

    public synchronized long mark() {
        return pauses.recorded();
    }

    public synchronized GcPauseTotals total() {
        return new GcPauseTotals(pauses.recorded(), Durations.millis(totalPauseNanos), Durations.millis(longestPauseNanos));
    }

    public synchronized GcPauseTotals since(long mark) {
        long[] recent = pauses.since(mark);
        int count = recent.length;
        long total = 0;
        long longest = 0;
        for (int i = 0; i < count; i++) {
            total += recent[i];
            longest = Math.max(longest, recent[i]);
        }
        return new GcPauseTotals(count, Durations.millis(total), Durations.millis(longest));
    }
}
