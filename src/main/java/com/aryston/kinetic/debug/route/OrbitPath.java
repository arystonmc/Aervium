package com.aryston.kinetic.debug.route;

public record OrbitPath(double centerX, double centerY, double centerZ, double radius, double height, int ticksPerTurn) implements CameraPath {
    private static final double FULL_TURN = Math.PI * 2.0;

    @Override
    public CameraPose pose(int tick) {
        double angle = FULL_TURN * tick / ticksPerTurn;
        double x = centerX + Math.cos(angle) * radius;
        double z = centerZ + Math.sin(angle) * radius;
        double towardX = centerX - x;
        double towardZ = centerZ - z;
        float yaw = (float) Math.toDegrees(Math.atan2(-towardX, towardZ));
        float pitch = (float) Math.toDegrees(Math.atan2(height, radius));
        return new CameraPose(x, centerY + height, z, yaw, pitch);
    }

    @Override
    public boolean followsGround() {
        return false;
    }
}
