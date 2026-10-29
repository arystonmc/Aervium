package com.aryston.kinetic.client.benchmark;

import com.aryston.kinetic.debug.route.CameraPath;
import com.aryston.kinetic.debug.route.CameraPose;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

final class BenchmarkCamera {
    private static final double MAX_CLIMB_PER_TICK = 0.6;
    private static final double UNKNOWN_GROUND = Double.NaN;

    private double groundY = UNKNOWN_GROUND;

    void reset() {
        groundY = UNKNOWN_GROUND;
    }

    void place(LocalPlayer player, ClientLevel level, CameraPath path, int tick) {
        CameraPose pose = path.pose(tick);
        double y = path.followsGround() ? followGround(level, pose) : pose.y();
        player.getAbilities().flying = true;
        player.setDeltaMovement(Vec3.ZERO);
        player.setPos(pose.x(), y, pose.z());
        player.setYRot(pose.yaw());
        player.setYHeadRot(pose.yaw());
        player.setXRot(pose.pitch());
    }

    private double followGround(ClientLevel level, CameraPose pose) {
        int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(pose.x()), Mth.floor(pose.z()));
        if (surface <= level.getMinY()) {
            return Double.isNaN(groundY) ? pose.y() : groundY;
        }
        groundY = Double.isNaN(groundY) ? surface : groundY + Math.clamp(surface - groundY, -MAX_CLIMB_PER_TICK, MAX_CLIMB_PER_TICK);
        return groundY;
    }
}
