# Aervium Code Map: `com.aryston.aervium.core.mesh`

Part of the [Aervium code map](../CODEMAP.md). Every class of this package has its entry here and nowhere else (rule D1).

### `com.aryston.aervium.core.mesh`

#### CompactVertexEncoder
- Path: `src/main/java/com/aryston/aervium/core/mesh/CompactVertexEncoder.java`
- Role: Rewrites a finished chunk layer mesh in place from the vanilla 28-byte block vertex into the 16-byte compact vertex, front to back so no unread vertex is overwritten: position as three unsigned shorts in 1/2048 block steps over -8 to +24 blocks of the section with block light in the low and sky light in the high byte of a fourth short, the color unchanged, texture coordinates as two unsigned shorts over 65536. Texture coordinates that are not on the 1/65536 grid round toward the inside of their quad, so a face never samples the neighbouring sprite. Positions outside the range are moved to its edge and counted.
- Notes: Per-thread instance (`encodeInPlace`), runs on the chunk build workers. Vanilla models reach at most one block outside their block, so the range is never left by vanilla content; coordinates on the 1/16 model grid and on a power-of-two atlas are exact.
- Depends on: nothing.

#### FacingSort
- Path: `src/main/java/com/aryston/aervium/core/mesh/FacingSort.java`
- Role: Reorders the quads of a finished chunk layer mesh in place into seven groups: quads whose plane is not axis aligned first, then east, west, up, down, south and north facing quads. A quad's facing is the normal of its diagonals in vertex order (counter-clockwise seen from the front, the order Minecraft's baked faces use and the GPU culls by); all four corners must lie on one plane. Records per group the plane nearest the far side (lowest plane for faces toward positive axes, highest for negative) and returns a `FaceRanges`.
- Notes: Counting sort through a per-thread scratch array; runs on the chunk build workers before upload. Reads the position as three floats at the start of each vertex in native byte order.
- Depends on: `FaceRanges`.

#### FaceRanges
- Path: `src/main/java/com/aryston/aervium/core/mesh/FaceRanges.java`
- Role: The first quad of each facing group of one layer mesh and its plane bound. Stored as one packed `int[]` (`PACKED_INTS`: first quads, then the planes as float bits); `pack` copies it into a draw record and the static methods read it there. `visibleBuckets` gives the groups that can face an eye at a section-relative position: a group is skipped only when the eye is at least half a block behind every plane in it (the margin covers view bobbing, which moves the real eye slightly).
- Depends on: nothing.

#### SharedCorners
- Path: `src/main/java/com/aryston/aervium/core/mesh/SharedCorners.java`
- Role: Finds the distinct vertex positions of a model cube by exact float bits and maps every vertex to its shared corner.
- Depends on: nothing.
