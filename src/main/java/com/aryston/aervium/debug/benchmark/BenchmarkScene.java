package com.aryston.aervium.debug.benchmark;

public enum BenchmarkScene {
    WALK("walk", "Walking over the surface"),
    ELYTRA("elytra", "Elytra-speed flight over new terrain"),
    VILLAGE("village", "Village with a crowd of villagers"),
    UNDERGROUND("underground", "Flight through caves and deepslate"),
    REDSTONE("redstone", "Running redstone build");

    private final String id;
    private final String description;

    BenchmarkScene(String id, String description) {
        this.id = id;
        this.description = description;
    }

    public String id() {
        return id;
    }

    public String description() {
        return description;
    }
}
