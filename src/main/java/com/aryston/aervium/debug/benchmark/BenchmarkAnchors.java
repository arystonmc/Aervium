package com.aryston.aervium.debug.benchmark;

import java.util.Map;

public record BenchmarkAnchors(BlockPoint spawn, Map<String, BlockPoint> sites) {
    public BlockPoint site(BenchmarkScene scene) {
        return sites.getOrDefault(scene.id(), spawn);
    }
}
