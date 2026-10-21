# Aervium Code Map: `com.aryston.aervium.world.network`

Part of the [Aervium code map](../CODEMAP.md). Every class of this package has its entry here and nowhere else (rule D1).

### `com.aryston.aervium.world.network`

#### PacketCompression, CompressionCheck
- Path: `src/main/java/com/aryston/aervium/world/network/PacketCompression.java`, `src/main/java/com/aryston/aervium/world/network/CompressionCheck.java`
- Role: `PacketCompression` creates the deflater of a connection at level 4. `CompressionCheck` runs at the start of the parity run: it sends a 128 KB packet through a real `CompressionEncoder` and `CompressionDecoder` (with the module's mixin applied when it is on) and logs whether the bytes came back identical and the compressed size.
- Depends on: nothing.
