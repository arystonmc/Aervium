package com.aryston.kinetic.core.collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class ColumnIndexedLongMapTest {
    private static final int SECTIONS = 4;
    private static final ColumnIndexedLongMap.Layout LAYOUT = new ColumnIndexedLongMap.Layout() {
        @Override
        public long column(long key) {
            return key / 100;
        }

        @Override
        public int index(long key) {
            return (int) (key % 100);
        }
    };

    private static ColumnIndexedLongMap<Optional<String>> map() {
        return new ColumnIndexedLongMap<>(LAYOUT, Optional::isPresent, SECTIONS);
    }

    @Test
    void columnIsCompleteOnlyWhenEverySectionIsStored() {
        ColumnIndexedLongMap<Optional<String>> map = map();
        map.put(700, Optional.empty());
        map.put(701, Optional.of("bed"));
        map.put(702, Optional.empty());
        assertFalse(map.complete(7));
        map.put(703, Optional.empty());
        assertTrue(map.complete(7));
        assertEquals(0b10, map.occupiedMask(7));
    }

    @Test
    void replacingAndRemovingKeepTheIndexExact() {
        ColumnIndexedLongMap<Optional<String>> map = map();
        for (int index = 0; index < SECTIONS; index++) {
            map.put(500 + index, Optional.empty());
        }
        map.put(503, Optional.of("bell"));
        map.put(503, Optional.of("bell"));
        assertTrue(map.complete(5));
        assertEquals(0b1000, map.occupiedMask(5));
        map.put(503, Optional.empty());
        assertEquals(0, map.occupiedMask(5));
        map.put(500, Optional.of("job"));
        map.remove(500);
        map.remove(500);
        assertFalse(map.complete(5));
        assertEquals(0, map.occupiedMask(5));
        for (int index = 1; index < SECTIONS; index++) {
            map.remove(500 + index);
        }
        assertTrue(map.isEmpty());
        map.put(500, Optional.empty());
        assertFalse(map.complete(5));
    }

    @Test
    void rejectsColumnsTallerThanTheMask() {
        assertFalse(ColumnIndexedLongMap.supports(65));
        assertTrue(ColumnIndexedLongMap.supports(24));
        assertThrows(IllegalArgumentException.class, () -> new ColumnIndexedLongMap<Optional<String>>(LAYOUT, Optional::isPresent, 65));
    }
}
