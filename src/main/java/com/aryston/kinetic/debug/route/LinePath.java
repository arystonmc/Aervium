package com.aryston.kinetic.debug.route;

public record LinePath(double startX, double startY, double startZ, float yaw, float pitch, double blocksPerTick, boolean followsGround) implements CameraPath {
    @Override
    public CameraPose pose(int tick) {
        double radians = Math.toRadians(yaw);
        double distance = blocksPerTick * tick;
        return new CameraPose(
            startX - Math.sin(radians) * distance,
            startY,
            startZ + Math.cos(radians) * distance,
            yaw,
            pitch
        );
    }
}
