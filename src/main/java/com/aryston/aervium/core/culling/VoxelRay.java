package com.aryston.aervium.core.culling;

public final class VoxelRay {
    private static final int MAX_STEPS = 2048;

    private VoxelRay() {
    }

    public static boolean reaches(double fromX, double fromY, double fromZ, double toX, double toY, double toZ, BlockOpacity opacity) {
        int x = floor(fromX);
        int y = floor(fromY);
        int z = floor(fromZ);
        int endX = floor(toX);
        int endY = floor(toY);
        int endZ = floor(toZ);
        double dx = toX - fromX;
        double dy = toY - fromY;
        double dz = toZ - fromZ;
        int stepX = Double.compare(dx, 0.0);
        int stepY = Double.compare(dy, 0.0);
        int stepZ = Double.compare(dz, 0.0);
        double deltaX = stepX == 0 ? Double.POSITIVE_INFINITY : Math.abs(1.0 / dx);
        double deltaY = stepY == 0 ? Double.POSITIVE_INFINITY : Math.abs(1.0 / dy);
        double deltaZ = stepZ == 0 ? Double.POSITIVE_INFINITY : Math.abs(1.0 / dz);
        double nextX = stepX == 0 ? Double.POSITIVE_INFINITY : deltaX * (stepX > 0 ? x + 1 - fromX : fromX - x);
        double nextY = stepY == 0 ? Double.POSITIVE_INFINITY : deltaY * (stepY > 0 ? y + 1 - fromY : fromY - y);
        double nextZ = stepZ == 0 ? Double.POSITIVE_INFINITY : deltaZ * (stepZ > 0 ? z + 1 - fromZ : fromZ - z);
        for (int step = 0; step < MAX_STEPS; step++) {
            if (x == endX && y == endY && z == endZ) {
                return true;
            }
            if (nextX <= nextY && nextX <= nextZ) {
                if (nextX > 1.0) {
                    return true;
                }
                x += stepX;
                nextX += deltaX;
            } else if (nextY <= nextZ) {
                if (nextY > 1.0) {
                    return true;
                }
                y += stepY;
                nextY += deltaY;
            } else {
                if (nextZ > 1.0) {
                    return true;
                }
                z += stepZ;
                nextZ += deltaZ;
            }
            if (opacity.opaque(x, y, z)) {
                return false;
            }
        }
        return false;
    }

    public static double entry(double fromX, double fromY, double fromZ, double toX, double toY, double toZ,
                               double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        double enter = 0.0;
        double exit = 1.0;
        double[] from = {fromX, fromY, fromZ};
        double[] direction = {toX - fromX, toY - fromY, toZ - fromZ};
        double[] low = {minX, minY, minZ};
        double[] high = {maxX, maxY, maxZ};
        for (int axis = 0; axis < 3; axis++) {
            if (Math.abs(direction[axis]) < 1.0E-12) {
                if (from[axis] < low[axis] || from[axis] > high[axis]) {
                    return Double.NaN;
                }
                continue;
            }
            double t0 = (low[axis] - from[axis]) / direction[axis];
            double t1 = (high[axis] - from[axis]) / direction[axis];
            enter = Math.max(enter, Math.min(t0, t1));
            exit = Math.min(exit, Math.max(t0, t1));
        }
        return enter <= exit ? enter : Double.NaN;
    }

    private static int floor(double value) {
        return (int) Math.floor(value);
    }

    @FunctionalInterface
    public interface BlockOpacity {
        boolean opaque(int x, int y, int z);
    }
}
