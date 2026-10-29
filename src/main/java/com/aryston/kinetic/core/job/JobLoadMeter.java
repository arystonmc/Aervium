package com.aryston.kinetic.core.job;

public final class JobLoadMeter {
    private static final double NANOS_PER_SECOND = 1.0E9;

    private final JobSystem jobs;
    private long markJobs;
    private long markBusyNanos;
    private long markTime;

    public JobLoadMeter(JobSystem jobs, long nowNanos) {
        this.jobs = jobs;
        mark(nowNanos);
    }

    public JobLoad take(long nowNanos) {
        long elapsed = nowNanos - markTime;
        long finished = jobs.timedJobs() - markJobs;
        long busy = jobs.busyNanos() - markBusyNanos;
        mark(nowNanos);
        if (elapsed <= 0) {
            return new JobLoad(0.0, 0.0);
        }
        double perSecond = finished * NANOS_PER_SECOND / elapsed;
        double busyShare = Math.clamp((double) busy / ((double) elapsed * jobs.workers()), 0.0, 1.0);
        return new JobLoad(perSecond, busyShare);
    }

    private void mark(long nowNanos) {
        markJobs = jobs.timedJobs();
        markBusyNanos = jobs.busyNanos();
        markTime = nowNanos;
    }

    public record JobLoad(double jobsPerSecond, double busyShare) {
    }
}
