package com.aryston.kinetic.debug.route;

public sealed interface CameraPath permits LinePath, OrbitPath {
    CameraPose pose(int tick);

    boolean followsGround();
}
