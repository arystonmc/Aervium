# Kinetic Code Map: `com.aryston.kinetic.client.particle`

Part of the [Kinetic code map](../CODEMAP.md). Every class of this package has its entry here and nowhere else (rule D1).

### `com.aryston.kinetic.client.particle`

#### ParticleCollisions
- Path: `src/main/java/com/aryston/kinetic/client/particle/ParticleCollisions.java`
- Role: Checks the exact widened, one-block-expanded swept block cuboid that vanilla collision iteration would visit, clamped to build height and limited to 256 cells. Returns true only when every required loaded chunk and cell is air, so an empty collider list has vanilla's unchanged movement result.
- Depends on: nothing.
