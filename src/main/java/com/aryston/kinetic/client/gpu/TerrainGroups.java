package com.aryston.kinetic.client.gpu;

import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.pipeline.IndexType;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import org.jspecify.annotations.Nullable;

final class TerrainGroups {
    static final int MAX_GROUPS = 64;
    static final int NONE = -1;

    private final Group[] groups = new Group[MAX_GROUPS];
    private final Map<Key, Group> byKey = new HashMap<>();
    private final int[] ranks = new int[MAX_GROUPS];
    private int live;

    int idFor(ChunkSectionLayer layer, GpuBuffer vertexBuffer, @Nullable GpuBuffer indexBuffer, @Nullable IndexType indexType) {
        Key key = new Key(layer, vertexBuffer, indexBuffer, indexType);
        Group group = byKey.get(key);
        if (group != null) {
            return group.id();
        }
        for (int id = 0; id < MAX_GROUPS; id++) {
            if (groups[id] == null) {
                group = new Group(id, layer, vertexBuffer, indexBuffer, indexType);
                groups[id] = group;
                byKey.put(key, group);
                return id;
            }
        }
        return NONE;
    }

    void dropClosed() {
        for (int id = 0; id < MAX_GROUPS; id++) {
            Group group = groups[id];
            if (group != null && group.closed()) {
                groups[id] = null;
                byKey.remove(new Key(group.layer(), group.vertexBuffer(), group.indexBuffer(), group.indexType()));
            }
        }
    }

    int rankLive() {
        live = 0;
        for (int id = 0; id < MAX_GROUPS; id++) {
            ranks[id] = groups[id] == null ? NONE : live++;
        }
        return live;
    }

    int live() {
        return live;
    }

    int rank(int id) {
        return ranks[id];
    }

    @Nullable Group group(int id) {
        return groups[id];
    }

    record Group(int id, ChunkSectionLayer layer, GpuBuffer vertexBuffer, @Nullable GpuBuffer indexBuffer, @Nullable IndexType indexType) {
        boolean closed() {
            return vertexBuffer.isClosed() || (indexBuffer != null && indexBuffer.isClosed());
        }
    }

    private record Key(ChunkSectionLayer layer, GpuBuffer vertexBuffer, @Nullable GpuBuffer indexBuffer, @Nullable IndexType indexType) {
    }
}
