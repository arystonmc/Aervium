package com.aryston.kinetic.debug.benchmark;

import com.aryston.kinetic.debug.route.CameraPath;
import com.aryston.kinetic.debug.route.LinePath;
import com.aryston.kinetic.debug.route.OrbitPath;

public final class BenchmarkRoute {
    public static final int WARMUP_TICKS = 400;
    public static final int SETTLE_TICKS = 200;
    public static final int MEASURE_TICKS = 600;
    public static final double FLIGHT_SPEED = 4.0;

    private static final double BLOCK_CENTER = 0.5;
    private static final float EAST = -90.0F;
    private static final float SOUTH = 0.0F;
    private static final float WEST = 90.0F;
    private static final double WALK_SPEED = 0.2158;
    private static final float WALK_PITCH = 5.0F;
    private static final int FLIGHT_ALTITUDE = 80;
    private static final float FLIGHT_PITCH = 15.0F;
    private static final int UNDERGROUND_Y = -30;
    private static final double UNDERGROUND_SPEED = 0.6;
    private static final double CROWD_RADIUS = 36.0;
    private static final double CROWD_HEIGHT = 20.0;
    private static final double STORAGE_RADIUS = 44.0;
    private static final double STORAGE_HEIGHT = 16.0;
    private static final double PARTICLES_RADIUS = 30.0;
    private static final double PARTICLES_HEIGHT = 14.0;
    private static final double REDSTONE_RADIUS = 44.0;
    private static final double REDSTONE_HEIGHT = 16.0;
    private static final double EXPLOSION_RADIUS = 48.0;
    private static final double EXPLOSION_HEIGHT = 32.0;
    private static final double SERVER_RADIUS = 64.0;
    private static final double SERVER_HEIGHT = 48.0;
    private static final double OVERLOOK_RADIUS = 24.0;
    private static final double OVERLOOK_HEIGHT = 300.0;
    private static final int TICKS_PER_TURN = SETTLE_TICKS + MEASURE_TICKS;

    private BenchmarkRoute() {
    }

    public static CameraPath path(BenchmarkScene scene, BenchmarkAnchors anchors) {
        BlockPoint spawn = anchors.spawn();
        BlockPoint center = scene.setup().center(anchors.site(scene));
        return switch (scene) {
            case WALK -> line(spawn, spawn.y(), EAST, WALK_PITCH, WALK_SPEED, true);
            case FLIGHT -> line(spawn, spawn.y() + FLIGHT_ALTITUDE, SOUTH, FLIGHT_PITCH, FLIGHT_SPEED, false);
            case UNDERGROUND -> line(spawn, UNDERGROUND_Y, WEST, 0.0F, UNDERGROUND_SPEED, false);
            case CROWD -> orbit(center, CROWD_RADIUS, CROWD_HEIGHT);
            case STORAGE -> orbit(center, STORAGE_RADIUS, STORAGE_HEIGHT);
            case PARTICLES -> orbit(center, PARTICLES_RADIUS, PARTICLES_HEIGHT);
            case REDSTONE -> orbit(center, REDSTONE_RADIUS, REDSTONE_HEIGHT);
            case EXPLOSION -> orbit(center, EXPLOSION_RADIUS, EXPLOSION_HEIGHT);
            case SERVER -> orbit(spawn, SERVER_RADIUS, SERVER_HEIGHT);
            case OVERLOOK -> orbit(spawn, OVERLOOK_RADIUS, OVERLOOK_HEIGHT);
        };
    }

    private static LinePath line(BlockPoint start, double y, float yaw, float pitch, double speed, boolean followGround) {
        return new LinePath(start.x() + BLOCK_CENTER, y, start.z() + BLOCK_CENTER, yaw, pitch, speed, followGround);
    }

    private static OrbitPath orbit(BlockPoint center, double radius, double height) {
        return new OrbitPath(center.x() + BLOCK_CENTER, center.y(), center.z() + BLOCK_CENTER, radius, height, TICKS_PER_TURN);
    }
}
