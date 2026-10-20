package com.aryston.aervium.core.lod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class LodAllocatorTest {
    @Test
    void alignsAllocationsAndCoalescesReleasedRanges() {
        LodAllocator allocator = new LodAllocator(1024, 1, 64);
        var first = allocator.allocate(65);
        var second = allocator.allocate(1);
        assertEquals(128, first.bytes());
        assertEquals(128, second.offset());
        allocator.free(first);
        allocator.free(second);
        assertEquals(new LodAllocator.Allocation(0, 0, 1024), allocator.allocate(1024));
        assertNull(allocator.allocate(1));
    }

    @Test
    void enforcesPageLimitsAndRejectsDoubleRelease() {
        LodAllocator allocator = new LodAllocator(256, 2, 64);
        var first = allocator.allocate(256);
        var second = allocator.allocate(200);
        assertEquals(1, second.page());
        assertNull(allocator.allocate(1));
        allocator.free(first);
        assertThrows(IllegalArgumentException.class, () -> allocator.free(first));
        assertNull(allocator.allocate(257));
    }
}
