# Kinetic Code Map: `com.aryston.kinetic.debug.route`

Part of the [Kinetic code map](../CODEMAP.md). Every class of this package has its entry here and nowhere else (rule D1).

### `com.aryston.kinetic.debug.route`

#### CameraPath
- Path: `src/main/java/com/aryston/kinetic/debug/route/CameraPath.java`
- Role: Sealed interface of a camera route: `pose(tick)` and whether the height follows the ground.
- Depends on: `CameraPose`, `LinePath`, `OrbitPath`.

#### CameraPose
- Path: `src/main/java/com/aryston/kinetic/debug/route/CameraPose.java`
- Role: Record of a feet position and the yaw and pitch of the camera.
- Depends on: nothing.

#### LinePath
- Path: `src/main/java/com/aryston/kinetic/debug/route/LinePath.java`
- Role: Straight route at a constant speed in the direction of its yaw (Minecraft convention: yaw 0 looks south).
- Depends on: `CameraPose`.

#### OrbitPath
- Path: `src/main/java/com/aryston/kinetic/debug/route/OrbitPath.java`
- Role: Circle around a center at a height, always looking at the center, one turn per `ticksPerTurn`.
- Depends on: `CameraPose`.
