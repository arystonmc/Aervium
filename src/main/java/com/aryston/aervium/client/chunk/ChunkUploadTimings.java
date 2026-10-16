package com.aryston.aervium.client.chunk;

import com.aryston.aervium.client.FrameTrace;
import com.aryston.aervium.core.budget.StallMeter;
import com.aryston.aervium.core.budget.SystemTimings;
import com.aryston.aervium.core.budget.TimedSystem;

public final class ChunkUploadTimings {
    public static final StallMeter UPLOADS = new StallMeter();
    public static final StallMeter LOCK_WAITS = new StallMeter();

    private static long uploadStart;

    private ChunkUploadTimings() {
    }

    public static void uploadStarted() {
        uploadStart = System.nanoTime();
    }

    public static void uploadFinished() {
        if (uploadStart != 0) {
            long nanos = System.nanoTime() - uploadStart;
            UPLOADS.record(nanos);
            FrameTrace.chunkUploaded(nanos);
            SystemTimings.SHARED.end(TimedSystem.CHUNK_UPLOAD, uploadStart);
            uploadStart = 0;
        }
    }

    public static void lockWaited(long startNanos) {
        long nanos = System.nanoTime() - startNanos;
        LOCK_WAITS.record(nanos);
        FrameTrace.chunkLockWaited(nanos);
        SystemTimings.SHARED.end(TimedSystem.CHUNK_LOCK_WAIT, startNanos);
    }
}
