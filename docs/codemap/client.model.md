# Kinetic Code Map: `com.aryston.kinetic.client.model`

Part of the [Kinetic code map](../CODEMAP.md). Every class of this package has its entry here and nowhere else (rule D1).

### `com.aryston.kinetic.client.model`

#### CubeCorners
- Path: `src/main/java/com/aryston/kinetic/client/model/CubeCorners.java`
- Role: Transforms each shared cube corner once and writes vertices in vanilla order with the same normals; cubes with more than 64 distinct positions use per-vertex transforms.
- Depends on: `SharedCorners`, Minecraft model and vertex classes.
