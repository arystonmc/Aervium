package com.aryston.kinetic.core.telemetry;

public record GcPauseTotals(long pauses, double totalMs, double longestMs) {
}
