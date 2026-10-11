package com.aryston.aervium.core.telemetry;

public record GcPauseTotals(long pauses, double totalMs, double longestMs) {
}
