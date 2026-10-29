package com.aryston.kinetic.debug.route;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class CameraPathTest {
    private static final double EPSILON = 1e-9;

    @Test
    void lineMovesInTheDirectionTheCameraFaces() {
        LinePath east = new LinePath(0.0, 70.0, 0.0, -90.0F, 0.0F, 2.0, false);
        CameraPose pose = east.pose(10);
        assertEquals(20.0, pose.x(), EPSILON);
        assertEquals(0.0, pose.z(), EPSILON);
        assertEquals(70.0, pose.y(), EPSILON);
        LinePath south = new LinePath(5.0, 70.0, 5.0, 0.0F, 0.0F, 1.0, false);
        assertEquals(15.0, south.pose(10).z(), EPSILON);
        assertEquals(5.0, south.pose(10).x(), EPSILON);
    }

    @Test
    void orbitKeepsItsRadiusAndLooksAtTheCenter() {
        OrbitPath orbit = new OrbitPath(100.0, 64.0, -50.0, 20.0, 10.0, 400);
        for (int tick = 0; tick < 400; tick += 37) {
            CameraPose pose = orbit.pose(tick);
            double dx = pose.x() - 100.0;
            double dz = pose.z() + 50.0;
            assertEquals(20.0, Math.hypot(dx, dz), EPSILON);
            assertEquals(74.0, pose.y(), EPSILON);
            double yaw = Math.toRadians(pose.yaw());
            double lookX = -Math.sin(yaw);
            double lookZ = Math.cos(yaw);
            assertEquals(-dx / 20.0, lookX, 1e-5);
            assertEquals(-dz / 20.0, lookZ, 1e-5);
        }
        assertEquals(Math.toDegrees(Math.atan2(10.0, 20.0)), orbit.pose(0).pitch(), 1e-5);
        assertFalse(orbit.followsGround());
    }

    @Test
    void orbitClosesAfterOneTurn() {
        OrbitPath orbit = new OrbitPath(0.0, 0.0, 0.0, 8.0, 4.0, 200);
        assertEquals(orbit.pose(0).x(), orbit.pose(200).x(), 1e-9);
        assertEquals(orbit.pose(0).z(), orbit.pose(200).z(), 1e-9);
    }
}
