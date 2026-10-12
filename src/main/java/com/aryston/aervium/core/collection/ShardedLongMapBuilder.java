package com.aryston.aervium.core.collection;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.Arrays;

public final class ShardedLongMapBuilder<V> {
    private static final int SHARDS_PER_WORD = Long.SIZE;

    private final Long2ObjectOpenHashMap<V>[] mirrors;
    private final long[] dirty = new long[ShardedLongMap.SHARDS / SHARDS_PER_WORD];
    private Long2ObjectOpenHashMap<V>[] published;

    @SuppressWarnings("unchecked")
    private ShardedLongMapBuilder() {
        mirrors = new Long2ObjectOpenHashMap[ShardedLongMap.SHARDS];
        published = new Long2ObjectOpenHashMap[ShardedLongMap.SHARDS];
        for (int shard = 0; shard < ShardedLongMap.SHARDS; shard++) {
            mirrors[shard] = new Long2ObjectOpenHashMap<>();
        }
    }

    public static <V> ShardedLongMapBuilder<V> copyOf(Long2ObjectMap<V> source) {
        ShardedLongMapBuilder<V> builder = new ShardedLongMapBuilder<>();
        for (Long2ObjectMap.Entry<V> entry : source.long2ObjectEntrySet()) {
            builder.mirrors[ShardedLongMap.shard(entry.getLongKey())].put(entry.getLongKey(), entry.getValue());
        }
        Arrays.fill(builder.dirty, -1L);
        return builder;
    }

    public void put(long key, V value) {
        int shard = ShardedLongMap.shard(key);
        mirrors[shard].put(key, value);
        markDirty(shard);
    }

    public void remove(long key) {
        int shard = ShardedLongMap.shard(key);
        if (mirrors[shard].remove(key) != null) {
            markDirty(shard);
        }
    }

    public ShardedLongMap<V> publish() {
        Long2ObjectOpenHashMap<V>[] next = published.clone();
        for (int word = 0; word < dirty.length; word++) {
            long bits = dirty[word];
            while (bits != 0L) {
                int shard = word * SHARDS_PER_WORD + Long.numberOfTrailingZeros(bits);
                next[shard] = mirrors[shard].clone();
                bits &= bits - 1;
            }
            dirty[word] = 0L;
        }
        published = next;
        return new ShardedLongMap<>(next);
    }

    private void markDirty(int shard) {
        dirty[shard / SHARDS_PER_WORD] |= 1L << (shard % SHARDS_PER_WORD);
    }
}
