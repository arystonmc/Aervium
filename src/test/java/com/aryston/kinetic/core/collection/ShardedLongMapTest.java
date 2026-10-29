package com.aryston.kinetic.core.collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.Random;
import org.junit.jupiter.api.Test;

class ShardedLongMapTest {
    @Test
    void snapshotMatchesTheSourceMap() {
        Long2ObjectOpenHashMap<String> source = new Long2ObjectOpenHashMap<>();
        for (long key = -500; key < 500; key++) {
            source.put(key * 4099L, "v" + key);
        }
        ShardedLongMap<String> snapshot = ShardedLongMapBuilder.copyOf(source).publish();
        assertEquals(source.size(), snapshot.size());
        for (long key = -500; key < 500; key++) {
            assertEquals("v" + key, snapshot.get(key * 4099L));
            assertTrue(snapshot.containsKey(key * 4099L));
        }
        assertNull(snapshot.get(1L));
        assertFalse(snapshot.containsKey(1L));
    }

    @Test
    void publishedSnapshotsNeverChangeAfterLaterWrites() {
        ShardedLongMapBuilder<String> builder = ShardedLongMapBuilder.copyOf(new Long2ObjectOpenHashMap<>());
        builder.put(7L, "a");
        ShardedLongMap<String> first = builder.publish();
        builder.put(7L, "b");
        builder.put(8L, "c");
        builder.remove(9L);
        ShardedLongMap<String> second = builder.publish();
        builder.remove(7L);
        ShardedLongMap<String> third = builder.publish();
        assertEquals("a", first.get(7L));
        assertNull(first.get(8L));
        assertEquals("b", second.get(7L));
        assertEquals("c", second.get(8L));
        assertNull(third.get(7L));
        assertEquals(1, third.size());
    }

    @Test
    void followsRandomWritesLikeAPlainMap() {
        Random random = new Random(8675309L);
        Long2ObjectOpenHashMap<Integer> expected = new Long2ObjectOpenHashMap<>();
        ShardedLongMapBuilder<Integer> builder = ShardedLongMapBuilder.copyOf(expected);
        for (int round = 0; round < 50; round++) {
            for (int write = 0; write < 200; write++) {
                long key = random.nextInt(2000) - 1000L;
                if (random.nextInt(4) == 0) {
                    expected.remove(key);
                    builder.remove(key);
                } else {
                    expected.put(key, Integer.valueOf(write));
                    builder.put(key, Integer.valueOf(write));
                }
            }
            ShardedLongMap<Integer> snapshot = builder.publish();
            assertEquals(expected.size(), snapshot.size());
            for (long key = -1000; key < 1000; key++) {
                assertEquals(expected.get(key), snapshot.get(key));
            }
        }
    }

    @Test
    void isReadOnly() {
        ShardedLongMap<String> snapshot = ShardedLongMapBuilder.copyOf(new Long2ObjectOpenHashMap<String>()).publish();
        assertThrows(UnsupportedOperationException.class, () -> snapshot.put(1L, "x"));
        assertThrows(UnsupportedOperationException.class, () -> snapshot.remove(1L));
        assertSame(snapshot, snapshot.clone());
    }
}
