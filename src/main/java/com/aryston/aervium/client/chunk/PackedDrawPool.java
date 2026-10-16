package com.aryston.aervium.client.chunk;

import java.util.ArrayList;
import java.util.List;

final class PackedDrawPool {
    private final List<PackedDraws> lists = new ArrayList<>();
    private int used;

    void reset() {
        used = 0;
    }

    PackedDraws next() {
        if (used == lists.size()) {
            lists.add(new PackedDraws());
        }
        PackedDraws draws = lists.get(used++);
        draws.reset();
        return draws;
    }
}
