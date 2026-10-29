package com.aryston.kinetic.core.telemetry;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class SampleRingTest {
    @Test
    void keepsSamplesInRecordingOrder() {
        SampleRing ring = new SampleRing(4);
        ring.record(1);
        ring.record(2);
        ring.record(3);
        assertEquals(3, ring.size());
        assertArrayEquals(new long[] {1, 2, 3}, ring.latest(10));
    }

    @Test
    void overwritesTheOldestSamplesWhenFull() {
        SampleRing ring = new SampleRing(3);
        for (long sample = 1; sample <= 5; sample++) {
            ring.record(sample);
        }
        assertEquals(3, ring.size());
        assertEquals(5, ring.recorded());
        assertArrayEquals(new long[] {3, 4, 5}, ring.latest(3));
        assertArrayEquals(new long[] {4, 5}, ring.latest(2));
    }

    @Test
    void copiesIntoAnExistingArrayWithoutGrowingIt() {
        SampleRing ring = new SampleRing(8);
        for (long sample = 1; sample <= 6; sample++) {
            ring.record(sample);
        }
        long[] destination = new long[4];
        assertEquals(4, ring.copyLatest(destination, 10));
        assertArrayEquals(new long[] {3, 4, 5, 6}, destination);
    }

    @Test
    void returnsOnlySamplesRecordedAfterAMark() {
        SampleRing ring = new SampleRing(8);
        ring.record(1);
        long mark = ring.recorded();
        ring.record(2);
        ring.record(3);
        assertArrayEquals(new long[] {2, 3}, ring.since(mark));
        assertArrayEquals(new long[0], ring.since(ring.recorded()));
    }

    @Test
    void clearForgetsEverySample() {
        SampleRing ring = new SampleRing(2);
        ring.record(7);
        ring.clear();
        assertEquals(0, ring.size());
        assertArrayEquals(new long[0], ring.latest(2));
    }

    @Test
    void rejectsAnEmptyCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new SampleRing(0));
    }
}
