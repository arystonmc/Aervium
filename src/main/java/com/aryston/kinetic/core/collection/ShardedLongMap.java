package com.aryston.kinetic.core.collection;

import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectCollection;

public final class ShardedLongMap<V> extends Long2ObjectOpenHashMap<V> {
    static final int SHARD_BITS = 10;
    static final int SHARDS = 1 << SHARD_BITS;
    private static final String READ_ONLY = "Kinetic snapshot maps are read only";

    private final Long2ObjectOpenHashMap<V>[] shards;
    private final int size;

    ShardedLongMap(Long2ObjectOpenHashMap<V>[] shards) {
        super(0);
        this.shards = shards;
        int total = 0;
        for (Long2ObjectOpenHashMap<V> shard : shards) {
            total += shard.size();
        }
        this.size = total;
    }

    static int shard(long key) {
        return (int) (HashCommon.mix(key) >>> (Long.SIZE - SHARD_BITS));
    }

    @Override
    public V get(long key) {
        return shards[shard(key)].get(key);
    }

    @Override
    public V getOrDefault(long key, V defaultValue) {
        return shards[shard(key)].getOrDefault(key, defaultValue);
    }

    @Override
    public boolean containsKey(long key) {
        return shards[shard(key)].containsKey(key);
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public ShardedLongMap<V> clone() {
        return this;
    }

    @Override
    public V put(long key, V value) {
        throw new UnsupportedOperationException(READ_ONLY);
    }

    @Override
    public V remove(long key) {
        throw new UnsupportedOperationException(READ_ONLY);
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException(READ_ONLY);
    }

    @Override
    public Long2ObjectMap.FastEntrySet<V> long2ObjectEntrySet() {
        throw new UnsupportedOperationException(READ_ONLY);
    }

    @Override
    public LongSet keySet() {
        throw new UnsupportedOperationException(READ_ONLY);
    }

    @Override
    public ObjectCollection<V> values() {
        throw new UnsupportedOperationException(READ_ONLY);
    }
}
