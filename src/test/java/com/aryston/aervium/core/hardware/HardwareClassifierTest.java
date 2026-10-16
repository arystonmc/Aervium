package com.aryston.aervium.core.hardware;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HardwareClassifierTest {
    private static final long GB = 1L << 30;

    private static HardwareProfile machine(int threads, long heapGb, long ramGb, GpuKind kind, long vramGb) {
        return new HardwareProfile(threads, heapGb * GB, ramGb * GB, "Test GPU", kind, vramGb * GB);
    }

    @Test
    void gamingDesktopIsHigh() {
        assertEquals(HardwareTier.HIGH, HardwareClassifier.classify(machine(16, 6, 32, GpuKind.DISCRETE, 8)));
    }

    @Test
    void integratedGraphicsLaptopIsMedium() {
        assertEquals(HardwareTier.MEDIUM, HardwareClassifier.classify(machine(12, 4, 16, GpuKind.INTEGRATED, 2)));
    }

    @Test
    void weakestPartDecides() {
        assertEquals(HardwareTier.LOW, HardwareClassifier.classify(machine(4, 6, 32, GpuKind.DISCRETE, 8)));
        assertEquals(HardwareTier.LOW, HardwareClassifier.classify(machine(16, 1, 32, GpuKind.DISCRETE, 8)));
        assertEquals(HardwareTier.LOW, HardwareClassifier.classify(machine(16, 6, 32, GpuKind.SOFTWARE, 0)));
        assertEquals(HardwareTier.MEDIUM, HardwareClassifier.classify(machine(16, 6, 32, GpuKind.DISCRETE, 3)));
        assertEquals(HardwareTier.MEDIUM, HardwareClassifier.classify(machine(16, 6, 8, GpuKind.DISCRETE, 8)));
    }

    @Test
    void unknownValuesDoNotLowerTheClass() {
        assertEquals(HardwareTier.HIGH, HardwareClassifier.classify(
            new HardwareProfile(16, 6 * GB, HardwareProfile.UNKNOWN_BYTES, "?", GpuKind.UNKNOWN, HardwareProfile.UNKNOWN_BYTES)));
    }

    @Test
    void tiersOnlyLowerDistances() {
        assertEquals(8, HardwareTier.LOW.renderDistance(12));
        assertEquals(5, HardwareTier.LOW.renderDistance(5));
        assertEquals(6, HardwareTier.LOW.simulationDistance(12));
        assertEquals(32, HardwareTier.HIGH.renderDistance(32));
        assertTrue(HardwareTier.LOW.leafCulling());
        assertFalse(HardwareTier.MEDIUM.leafCulling());
    }
}
