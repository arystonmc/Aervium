package com.aryston.kinetic.debug.benchmark;

public record SceneSite(int eastOffset, int southOffset, int height) {
    public static final SceneSite SPAWN = new SceneSite(0, 0, 0);
}
